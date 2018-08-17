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
import com.squareup.inject.assisted.AssistedInject
import com.squareup.inject.assisted.processor.internal.associateWithNotNull
import com.squareup.inject.assisted.processor.internal.cast
import com.squareup.inject.assisted.processor.internal.findElementsAnnotatedWith
import com.squareup.inject.assisted.processor.internal.hasAnnotation
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeName
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
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.STATIC
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Types
import javax.tools.Diagnostic.Kind.ERROR

@AutoService(Processor::class)
class AssistedInjectProcessor : AbstractProcessor() {
  override fun getSupportedSourceVersion() = SourceVersion.latest()
  override fun getSupportedAnnotationTypes() = setOf(
      AssistedInject::class.java.canonicalName,
      AssistedInject.Factory::class.java.canonicalName)

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
    roundEnv.findCandidateTypeElements()
        .mapNotNull { it.toElementsOrNull() }
        .associateWithNotNull { it.toInjectionOrNull() }
        .forEach(::generate)
    return false
  }

  /**
   * Find [TypeElement]s which are candidates for assisted injection by:
   * - Having a constructor annotated with [AssistedInject]
   * - Having a nested type annotated with [AssistedInject.Factory]
   */
  private fun RoundEnvironment.findCandidateTypeElements(): List<TypeElement> {
    // Grab types with only @AssistedInject.Factory so we can detect missing @AssistedInject.
    val (enclosed, orphaned) = findElementsAnnotatedWith<AssistedInject.Factory>()
        .partition { it.enclosingElement.kind == CLASS }
    orphaned.forEach {
      error("@AssistedInject.Factory must be declared as a nested type.", it)
    }
    val fromFactory = enclosed.map { it.enclosingElement as TypeElement }

    // Grab types with only @AssistedInject so we can detect missing @AssistedInject.Factory.
    val fromConstructor = findElementsAnnotatedWith<AssistedInject>()
        .map { it.enclosingElement as TypeElement }

    return (fromFactory + fromConstructor)
        .distinctBy { MoreTypes.equivalence().wrap(it.asType()) }
  }

  /**
   * From this [TypeElement] which is a candidate for assisted injection, find and validate the
   * syntactical elements required to generate the factory:
   * - Non-private, non-inner target type
   * - Single non-private target constructor
   * - Single nested, non-private interface factory type
   * - Single abstract factory method
   */
  private fun TypeElement.toElementsOrNull(): AssistedInjectElements? {
    var valid = true
    if (PRIVATE in modifiers) {
      error("@AssistedInject-using types must not be private", this)
      valid = false
    }
    if (enclosingElement.kind == CLASS && STATIC !in modifiers) {
      error("Nested @AssistedInject-using types must be static", this)
      valid = false
    }

    val constructors = enclosedElements
        .filter { it.kind == CONSTRUCTOR }
        .filter { it.hasAnnotation<AssistedInject>() }
        .cast<ExecutableElement>()
    if (constructors.isEmpty()) {
      error("Assisted injection requires an @AssistedInject-annotated constructor " +
          "with at least one @Assisted parameter.", this)
      valid = false
    } else if (constructors.size > 1) {
      error("Multiple @AssistedInject-annotated constructors found.", this)
      valid = false
    }

    val factoryTypes = enclosedElements
        .filter { it.hasAnnotation<AssistedInject.Factory>() }
        .cast<TypeElement>()
    if (factoryTypes.isEmpty()) {
      error("No nested @AssistedInject.Factory found.", this)
      valid = false
    } else if (factoryTypes.size > 1) {
      error("Multiple @AssistedInject.Factory types found.", this)
      valid = false
    }

    if (!valid) return null

    val constructor = constructors.single()
    if (PRIVATE in constructor.modifiers) {
      error("@AssistedInject constructor must not be private.", constructor)
      valid = false
    }

    val factoryType = factoryTypes.single()
    if (factoryType.kind != INTERFACE) {
      error("@AssistedInject.Factory must be an interface.", factoryType)
      valid = false
    }
    if (PRIVATE in factoryType.modifiers) {
      error("@AssistedInject.Factory must not be private.", factoryType)
      valid = false
    }

    val factoryMethods = factoryType.enclosedElements
        .filterIsInstance<ExecutableElement>() // Ignore non-method elements like constants.
        .filterNot { it.isDefault } // Ignore default methods for convenience overloads.
        .filterNot { STATIC in it.modifiers } // Ignore static helper methods.
        .filterNot { PRIVATE in it.modifiers } // Ignore private helper methods for default methods.
    if (factoryMethods.isEmpty()) {
      error("Factory interface does not define a factory method.", factoryType)
      valid = false
    } else if (factoryMethods.size > 1) {
      error("Factory interface defines multiple factory methods.", factoryType)
      valid = false
    }

    if (!valid) return null

    val factoryMethod = factoryMethods.single()
    return AssistedInjectElements(this, constructor, factoryType, factoryMethod)
  }

  /**
   * From this [AssistedInjectElements], parse and validate the semantic information of the elements
   * which is required to generate the factory:
   * - At least one assisted parameter and no duplicates
   * - At least one provided parameter and no duplicates
   * - Factory method parameters match assisted parameters in any order
   */
  private fun AssistedInjectElements.toInjectionOrNull(): AssistedInjection? {
    var valid = true

    val requests = targetConstructor.parameters.map { it.asDependencyRequest() }
    val (assistedRequests, providedRequests) = requests.partition { it.isAssisted }
    if (providedRequests.isEmpty()) {
      error("Assisted injection requires at least one non-@Assisted parameter.", targetConstructor)
      valid = false
    }
    if (assistedRequests.isEmpty()) {
      error("Assisted injection requires at least one @Assisted parameter.", targetConstructor)
      valid = false
    }
    val assistedDuplicates = assistedRequests.groupBy { it.key }.filterValues { it.size > 1 }
    if (assistedDuplicates.isNotEmpty()) {
      error("Duplicate @Assisted parameters declared. Forget a qualifier annotation?"
              + assistedDuplicates.values.flatten().joinToString("\n * ", prefix = "\n * "),
          targetConstructor)
      valid = false
    }
    val providedDuplicates = providedRequests.groupBy { it.key }.filterValues { it.size > 1 }
    if (providedDuplicates.isNotEmpty()) {
      error("Duplicate non-@Assisted parameters declared. Forget a qualifier annotation?"
              + providedDuplicates.values.flatten().joinToString("\n * ", prefix = "\n * "),
          targetConstructor)
      valid = false
    }

    val expectedKeys = assistedRequests.map { it.key }.toSet()
    val factoryKeys = factoryMethod.parameters.map { it.asKey() }
    val keys = factoryKeys.toSet()
    if (keys != expectedKeys) {
      val message = buildString {
        append("Factory method parameters do not match constructor @Assisted parameters.")

        val missingKeys = expectedKeys - keys
        if (missingKeys.isNotEmpty()) {
          append(missingKeys.joinToString("\n * ", prefix = "\n\nMissing:\n * "))
        }
        val unknownKeys = keys - expectedKeys
        if (unknownKeys.isNotEmpty()) {
          append(unknownKeys.joinToString("\n * ", prefix = "\n\nUnknown:\n * "))
        }
      }
      error(message, factoryMethod)
      valid = false
    }

    if (!valid) return null

    val targetType = TypeName.get(targetType.asType())
    val factoryType = ClassName.get(factoryType)
    val returnType = ClassName.get(factoryMethod.returnType)
    val methodName = factoryMethod.simpleName.toString()
    return AssistedInjection(targetType, requests, factoryType, methodName, returnType, factoryKeys)
  }

  private fun generate(elements: AssistedInjectElements, injection: AssistedInjection) {
    val generatedTypeSpec = injection.brewJava()
        .toBuilder()
        .addOriginatingElement(elements.targetType)
        .addOriginatingElement(elements.factoryType)
        .build()
    JavaFile.builder(injection.generatedType.packageName(), generatedTypeSpec)
        .addFileComment("Generated by @AssistedInject. Do not modify!")
        .build()
        .writeTo(filer)
  }

  private fun error(message: String, element: Element? = null) {
    messager.printMessage(ERROR, message, element)
  }

  private data class AssistedInjectElements(
    val targetType: TypeElement,
    val targetConstructor: ExecutableElement,
    val factoryType: TypeElement,
    val factoryMethod: ExecutableElement
  )
}
