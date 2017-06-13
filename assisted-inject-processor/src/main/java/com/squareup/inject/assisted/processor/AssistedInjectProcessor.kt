/*
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.inject.assisted.processor

import com.google.auto.common.MoreTypes
import com.google.auto.service.AutoService
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.processor.internal.BindingKey
import com.squareup.inject.assisted.processor.internal.BindingKey.Use.ASSISTED
import com.squareup.inject.assisted.processor.internal.BindingKey.Use.PROVIDED
import com.squareup.inject.assisted.processor.internal.Key
import com.squareup.inject.assisted.processor.internal.applyEach
import com.squareup.inject.assisted.processor.internal.applyEachIndexed
import com.squareup.inject.assisted.processor.internal.cast
import com.squareup.inject.assisted.processor.internal.duplicates
import com.squareup.inject.assisted.processor.internal.findElementsAnnotatedWith
import com.squareup.inject.assisted.processor.internal.hasAnnotation
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind.CLASS
import javax.lang.model.element.ElementKind.CONSTRUCTOR
import javax.lang.model.element.ElementKind.INTERFACE
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Types
import javax.tools.Diagnostic.Kind.ERROR

@AutoService(Processor::class)
class AssistedInjectProcessor : AbstractProcessor() {
  override fun getSupportedSourceVersion() = SourceVersion.latest()
  override fun getSupportedAnnotationTypes() = setOf(
      Assisted::class.java.canonicalName,
      Assisted.Factory::class.java.canonicalName)

  override fun init(env: ProcessingEnvironment) {
    super.init(env)
    this.types = env.typeUtils
    this.messager = env.messager
    this.filer = env.filer
  }

  private lateinit var types: Types
  private lateinit var messager: Messager
  private lateinit var filer: Filer

  override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val factoryTypes = roundEnv.findElementsAnnotatedWith<Assisted.Factory>()
    factoryTypes
        .filterNot { it.enclosingElement.kind == CLASS }
        .forEach {
          messager.printMessage(ERROR, "@Assisted.Factory must be declared as a nested type.", it)
        }

    // Grab types with only an @Assisted.Factory so we can detect missing constructor annotations.
    val typeWrappersWithFactories = factoryTypes
        .map { it.enclosingElement }
        .filter { it.kind == CLASS }
        .map { MoreTypes.equivalence().wrap(it.asType()) }

    // Grab types with only @Assisted so we can detect missing @Assisted.Factory types.
    val typeWrappersWithAssisted = roundEnv.findElementsAnnotatedWith<Assisted>()
        .map { it.enclosingElement.enclosingElement as TypeElement } // param -> method -> type
        .map { MoreTypes.equivalence().wrap(it.asType()) }

    (typeWrappersWithFactories + typeWrappersWithAssisted)
        .toSet()
        .map { types.asElement(it.get()) as TypeElement }
        .forEach {
          try {
            process(it)
          } catch (e: StopProcessingException) {
            messager.printMessage(ERROR, e.message, e.originatingElement)
          } catch (e: Exception) {
            messager.printMessage(ERROR, "Uncaught error: ${e.message}")
          }
        }

    return false
  }

  private fun process(type: TypeElement) {
    val constructor = findAssistedConstructor(type)

    val bindingKeys = parseBindingKeys(constructor)
    val providedKeys = providedKeys(constructor, bindingKeys)
    val assistedKeys = assistedKeys(constructor, bindingKeys)

    val factoryType = findFactoryType(type)
    val factoryMethod = findFactoryMethod(factoryType, type)
    validateFactoryKeys(factoryMethod, assistedKeys.map(BindingKey::key).toSet())

    val typeName = ClassName.get(type)
    val factoryName = ClassName.get(factoryType)
    val generatedName = typeName.peerClass(type.simpleName.toString() + SUFFIX)
    val generatedSpec = TypeSpec.classBuilder(generatedName)
        .addModifiers(PUBLIC, FINAL)
        .addSuperinterface(factoryName)
        .addOriginatingElement(type)
        .addOriginatingElement(factoryType)
        .applyEach(providedKeys) {
          addField(it.providerType().withoutAnnotations(), it.name, PRIVATE, FINAL)
        }
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addAnnotation(INJECT)
            .applyEach(providedKeys) {
              addParameter(it.providerType(), it.name)
              addStatement("this.$1N = $1N", it.name)
            }
            .build())
        .addMethod(MethodSpec.overriding(factoryMethod)
            .addCode("$[return new \$T(\n", typeName)
            .applyEachIndexed(bindingKeys) { index, key ->
              if (index > 0) addCode(",\n")
              addCode(key.bindingResolveCode())
            }
            .addCode(");$]\n")
            .build())
        .build()

    JavaFile.builder(generatedName.packageName(), generatedSpec)
        .addFileComment("Generated by @Assisted. Do not modify!")
        .build()
        .writeTo(filer)
  }

  private fun findAssistedConstructor(type: TypeElement): ExecutableElement {
    val constructors = type.enclosedElements
        .filter { it.kind == CONSTRUCTOR }
        .cast<ExecutableElement>()
    if (constructors.size == 1) {
      // Only one constructor (common case). Use it! This also allows better error messages to be
      // produced in the case where you have no @Assisted parameters on this constructor.
      return constructors.single()
    }

    val assistedConstructors = constructors
        .filter { it.parameters.any { it.hasAnnotation<Assisted>() } }
    if (assistedConstructors.isEmpty()) {
      throw StopProcessingException(
          "Assisted injection requires a constructor with an @Assisted parameter.", type)
    }
    if (assistedConstructors.size > 1) {
      throw StopProcessingException("Multiple constructors define @Assisted parameters.", type)
    }
    return assistedConstructors.single()
  }

  private fun parseBindingKeys(method: ExecutableElement) = method.parameters
      .map { BindingKey(it, if (it.hasAnnotation<Assisted>()) ASSISTED else PROVIDED) }

  private fun providedKeys(method: ExecutableElement, keys: List<BindingKey>): List<BindingKey> {
    val providedKeys = keys.filter { it.use == PROVIDED }
    if (providedKeys.isEmpty()) {
      throw StopProcessingException(
          "Assisted injection requires at least one non-@Assisted parameter.", method)
    }
    val duplicateKeys = providedKeys.duplicates()
    if (duplicateKeys.isNotEmpty()) {
      throw StopProcessingException(
          "Duplicate non-@Assisted parameters declared. Forget a qualifier annotation?"
              + duplicateKeys.toSet().joinToString("\n * ", prefix = "\n * "),
          method)
    }
    return providedKeys
  }

  private fun assistedKeys(method: ExecutableElement, keys: List<BindingKey>): List<BindingKey> {
    val assistedKeys = keys.filter { it.use == ASSISTED }
    if (assistedKeys.isEmpty()) {
      throw StopProcessingException(
          "Assisted injection requires at least one @Assisted parameter.", method)
    }
    val duplicateKeys = assistedKeys.duplicates()
    if (duplicateKeys.isNotEmpty()) {
      throw StopProcessingException(
          "Duplicate @Assisted parameters declared. Forget a qualifier annotation?"
              + duplicateKeys.toSet().joinToString("\n * ", prefix = "\n * "),
          method)
    }
    return assistedKeys
  }

  private fun findFactoryType(type: TypeElement): TypeElement {
    val types = type.enclosedElements
        .filterIsInstance<TypeElement>()
        .filter { it.hasAnnotation<Assisted.Factory>() }
    if (types.isEmpty()) {
      throw StopProcessingException("No nested @Assisted.Factory found.", type)
    }
    if (types.size > 1) {
      throw StopProcessingException("Multiple @Assisted.Factory types found.", type)
    }
    val factory = types.single()
    if (factory.kind != INTERFACE) {
      throw StopProcessingException("@Assisted.Factory must be an interface.", factory)
    }
    return factory
  }

  private fun findFactoryMethod(factory: TypeElement, type: TypeElement): ExecutableElement {
    val methods = factory.enclosedElements
        .filterIsInstance<ExecutableElement>() // Ignore non-method elements like constants.
        .filterNot { it.isDefault } // Ignore default methods for convenience overloads.
        .filterNot { STATIC in it.modifiers } // Ignore static helper methods.
        .filterNot { PRIVATE in it.modifiers } // Ignore private helper methods for default methods.
    if (methods.isEmpty()) {
      throw StopProcessingException("Factory interface does not define a factory method.", factory)
    }
    if (methods.size > 1) {
      throw StopProcessingException("Factory interface defines multiple factory methods.", factory)
    }
    val method = methods.single()
    if (!types.isAssignable(type.asType(), method.returnType)) {
      throw StopProcessingException("Factory method returns incorrect type. "
          + "Must be ${type.simpleName} or one of its supertypes.", method)
    }
    return method
  }

  private fun validateFactoryKeys(method: ExecutableElement, expectedKeys: Set<Key>): Set<Key> {
    val keys = method.parameters
        .map(Key.Companion::invoke) // TODO https://youtrack.jetbrains.com/issue/KT-18403
        .toSet()
    if (keys != expectedKeys) {
      var message = "Factory method parameters do not match constructor @Assisted parameters."

      val missingKeys = expectedKeys - keys
      if (missingKeys.isNotEmpty()) {
        message += missingKeys.joinToString("\n * ", prefix = "\n\nMissing:\n * ")
      }

      val unknownKeys = keys - expectedKeys
      if (unknownKeys.isNotEmpty()) {
        message += unknownKeys.joinToString("\n * ", prefix = "\n\nUnknown:\n * ")
      }

      throw StopProcessingException(message, method)
    }
    return keys
  }

  companion object {
    const val SUFFIX = "_AssistedFactory"
    private val INJECT = ClassName.get("javax.inject", "Inject")
  }
}

internal class StopProcessingException(
    message: String,
    val originatingElement: Element? = null
) : Exception(message)
