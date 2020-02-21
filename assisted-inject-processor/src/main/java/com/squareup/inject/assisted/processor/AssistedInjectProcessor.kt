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

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.auto.service.AutoService
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.squareup.inject.assisted.processor.internal.castEach
import com.squareup.inject.assisted.processor.internal.createGeneratedAnnotation
import com.squareup.inject.assisted.processor.internal.filterNotNullValues
import com.squareup.inject.assisted.processor.internal.findElementsAnnotatedWith
import com.squareup.inject.assisted.processor.internal.hasAnnotation
import com.squareup.inject.assisted.processor.internal.toClassName
import com.squareup.inject.assisted.processor.internal.toTypeName
import com.squareup.javapoet.JavaFile
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.ISOLATING
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
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ExecutableType
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.WARNING

@IncrementalAnnotationProcessor(ISOLATING)
@AutoService(Processor::class)
class AssistedInjectProcessor : AbstractProcessor() {
  override fun getSupportedSourceVersion() = SourceVersion.latest()
  override fun getSupportedAnnotationTypes() = setOf(
      Assisted::class.java.canonicalName,
      AssistedInject::class.java.canonicalName,
      AssistedInject.Factory::class.java.canonicalName)

  override fun init(env: ProcessingEnvironment) {
    super.init(env)
    sourceVersion = env.sourceVersion
    messager = env.messager
    filer = env.filer
    elements = env.elementUtils
    types = env.typeUtils
  }

  private lateinit var sourceVersion: SourceVersion
  private lateinit var messager: Messager
  private lateinit var filer: Filer
  private lateinit var elements: Elements
  private lateinit var types: Types

  override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
    roundEnv.findAssistedInjectCandidateTypeElements()
        .mapNotNull { it.toAssistedInjectElementsOrNull() }
        .associateWith { it.toAssistedInjectionOrNull() }
        .filterNotNullValues()
        .forEach(::writeAssistedInject)

    val assistedMethods = roundEnv.findElementsAnnotatedWith<Assisted>()
        .map { it.enclosingElement as ExecutableElement }
    // Error any non-constructor usage of @Assisted. Methods called "copy" are also excluded due to the generated
    // for Kotlin data classes having their parameters carry the annotations from their counterparts in the primary
    // constructor.
    assistedMethods
        .filterNot { it.simpleName.contentEquals("<init>") }
        .filterNot { it.simpleName.contentEquals("copy") }
        .forEach {
          error("@Assisted is only supported on constructor parameters", it)
        }
    // Error any constructor usage of @Assisted which lacks method annotations.
    assistedMethods
        .filter { it.simpleName.contentEquals("<init>") }
        .filter { it.annotationMirrors.isEmpty() }
        .forEach {
          error("@Assisted parameter use requires a constructor annotation such as " +
              "@AssistedInject or @InflationInject", it)
        }
    // Error any constructor usage of @Assisted which also uses @Inject.
    assistedMethods
        .filter { it.simpleName.contentEquals("<init>") }
        .filter { it.hasAnnotation("javax.inject.Inject") }
        .forEach {
          error("@Assisted parameter does not work with @Inject! Use @AssistedInject or " +
              "@InflationInject", it)
        }

    return false
  }

  /**
   * Find [TypeElement]s which are candidates for assisted injection by:
   * - Having a constructor annotated with [AssistedInject]
   * - Having a nested type annotated with [AssistedInject.Factory]
   */
  private fun RoundEnvironment.findAssistedInjectCandidateTypeElements(): List<TypeElement> {
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
  private fun TypeElement.toAssistedInjectElementsOrNull(): AssistedInjectElements? {
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
        .castEach<ExecutableElement>()
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
        .castEach<TypeElement>()
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

    val factoryMethods = MoreElements.getLocalAndInheritedMethods(factoryType, types, elements)
        .filterNot { it.isDefault } // Ignore default methods for convenience overloads.
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
  private fun AssistedInjectElements.toAssistedInjectionOrNull(): AssistedInjection? {
    var valid = true

    val requests = targetConstructor.parameters.map { it.asDependencyRequest() }
    val (assistedRequests, providedRequests) = requests.partition { it.isAssisted }
    if (assistedRequests.isEmpty()) {
      warn("Assisted injection without at least one @Assisted parameter can use @Inject",
          targetConstructor)
    }
    if (providedRequests.isEmpty()) {
      warn("Assisted injection without at least one non-@Assisted parameter doesn't need a factory",
          targetConstructor)
    }

    // Project the factory method (which may have come from a supertype) as if it were a member of
    // the subtype to resolve any generic parameters.
    val factoryExecutable = types.asMemberOf(factoryType.asType() as DeclaredType,
        factoryMethod) as ExecutableType

    val expectedKeys = assistedRequests.map { it.namedKey }
    val factoryKeys = factoryMethod.parameters
        .zip(factoryExecutable.parameterTypes) { element, mirror -> element.asNamedKey(mirror) }

    // Rename any single keys to the factory keys to avoid requiring matching names for those keys.
    val singleExpectedKeys = expectedKeys
        .groupBy { it.key }
        .filterValues { it.size == 1 }
        .values
        .flatten()

    val singleFactoryKeys = factoryKeys
        .groupBy { it.key }
        .filterValues { it.size == 1 }
        .values
        .flatten()

    val renameableKeys = singleExpectedKeys
        .map { expectedKey ->
          val match = singleFactoryKeys.firstOrNull { factoryKey -> factoryKey.key == expectedKey.key }
          expectedKey to match
        }
        .filter { it.second != null }
        .toMap()

    val renamedExpectedKeys = expectedKeys.map { renameableKeys[it] ?: it }.sorted()
    val renamedRequests = requests.map { request ->
      DependencyRequest(
          renameableKeys[request.namedKey] ?: request.namedKey,
          request.isAssisted
      )
    }

    val sortedFactoryKeys = factoryKeys.sorted()
    if (sortedFactoryKeys != renamedExpectedKeys) {
      val message = buildString {
        append("Factory method parameters do not match constructor @Assisted parameters. ")
        append("Both parameter type and name must match.")

        val missingKeys = renamedExpectedKeys - sortedFactoryKeys
        if (missingKeys.isNotEmpty()) {
          append(missingKeys.joinToString("\n * ",
              prefix = "\nDeclared by constructor, unmatched in factory method:\n * "))
        }
        val unknownKeys = sortedFactoryKeys - renamedExpectedKeys
        if (unknownKeys.isNotEmpty()) {
          append(unknownKeys.joinToString("\n * ",
              prefix = "\nDeclared by factory method, unmatched in constructor:\n * "))
        }
      }
      error(message,
          if (factoryMethod.enclosingElement == factoryType) factoryMethod else targetConstructor)
      valid = false
    }

    if (!valid) return null

    val targetType = targetType.asType().toTypeName()
    val factoryType = factoryType.toClassName()
    val returnType = factoryExecutable.returnType.toTypeName()
    val methodName = factoryMethod.simpleName.toString()
    val generatedAnnotation = createGeneratedAnnotation(sourceVersion, elements)
    return AssistedInjection(targetType, renamedRequests, factoryType, methodName, returnType,
        factoryKeys, generatedAnnotation)
  }

  private fun writeAssistedInject(elements: AssistedInjectElements, injection: AssistedInjection) {
    val generatedTypeSpec = injection.brewJava()
        .toBuilder()
        .addOriginatingElement(elements.targetType)
        .build()
    JavaFile.builder(injection.generatedType.packageName(), generatedTypeSpec)
        .addFileComment("Generated by @AssistedInject. Do not modify!")
        .build()
        .writeTo(filer)
  }

  private fun warn(message: String, element: Element? = null) {
    messager.printMessage(WARNING, message, element)
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
