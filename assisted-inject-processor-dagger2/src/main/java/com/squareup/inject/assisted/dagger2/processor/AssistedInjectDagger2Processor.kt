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
package com.squareup.inject.assisted.dagger2.processor

import com.google.auto.service.AutoService
import com.squareup.inject.assisted.AssistedInject
import com.squareup.inject.assisted.dagger2.AssistedModule
import com.squareup.inject.assisted.dagger2.processor.internal.generateHiltAnnotation
import com.squareup.inject.assisted.processor.internal.MirrorValue
import com.squareup.inject.assisted.processor.internal.applyEach
import com.squareup.inject.assisted.processor.internal.cast
import com.squareup.inject.assisted.processor.internal.castEach
import com.squareup.inject.assisted.processor.internal.createGeneratedAnnotation
import com.squareup.inject.assisted.processor.internal.findElementsAnnotatedWith
import com.squareup.inject.assisted.processor.internal.getAnnotation
import com.squareup.inject.assisted.processor.internal.getValue
import com.squareup.inject.assisted.processor.internal.hasAnnotation
import com.squareup.inject.assisted.processor.internal.toClassName
import com.squareup.inject.assisted.processor.internal.toTypeName
import com.squareup.javapoet.JavaFile
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.AGGREGATING
import java.util.TreeMap
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.tools.Diagnostic.Kind.ERROR

@IncrementalAnnotationProcessor(AGGREGATING)
@AutoService(Processor::class)
class AssistedInjectDagger2Processor : AbstractProcessor() {
  override fun getSupportedSourceVersion() = SourceVersion.latest()
  override fun getSupportedAnnotationTypes() = setOf(
      AssistedModule::class.java.canonicalName,
      AssistedInject.Factory::class.java.canonicalName)

  override fun init(env: ProcessingEnvironment) {
    super.init(env)
    sourceVersion = env.sourceVersion
    messager = env.messager
    filer = env.filer
    elements = env.elementUtils
  }

  private lateinit var sourceVersion: SourceVersion
  private lateinit var messager: Messager
  private lateinit var filer: Filer
  private lateinit var elements: Elements

  private val unprocessedFactoryNames = mutableListOf<Name>()
  private var userModule: String? = null

  override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
    // Record factories as fully qualified names so they can safely be accessed in future
    // processing rounds.
    unprocessedFactoryNames += roundEnv.findElementsAnnotatedWith<AssistedInject.Factory>()
        .castEach<TypeElement>()
        .map { it.qualifiedName }

    val assistedModuleElements = roundEnv.findAssistedModuleElementsOrNull()
    if (assistedModuleElements != null) {
      val moduleType = assistedModuleElements.moduleType

      val userModuleFqcn = userModule
      if (userModuleFqcn != null) {
        val userModuleType = elements.getTypeElement(userModuleFqcn)
        error("Multiple @AssistedModule-annotated modules found.", userModuleType)
        error("Multiple @AssistedModule-annotated modules found.", moduleType)
        userModule = null
      } else {
        userModule = moduleType.qualifiedName.toString()

        val assistedInjectionModule = assistedModuleElements.toAssistedInjectionModule()
        writeAssistedModule(assistedModuleElements, assistedInjectionModule)
      }
    }

    // Wait until processing is ending to validate that the @AssistedModule's @Module annotation
    // includes the generated type.
    if (roundEnv.processingOver()) {
      val userModuleFqcn = userModule
      if (userModuleFqcn != null) {
        // In the processing round in which we handle the @AssistedModule the @Module annotation's
        // includes contain an <error> type because we haven't generated the assisted module yet.
        // As a result, we need to re-lookup the element so that its referenced types are available.
        val userModule = elements.getTypeElement(userModuleFqcn)

        // Previous validation guarantees this annotation is present.
        val moduleAnnotation = userModule.getAnnotation("dagger.Module")!!
        // Dagger guarantees this property is present and is an array of types or errors.
        val includes = moduleAnnotation.getValue("includes", elements)!!
            .cast<MirrorValue.Array>()
            .filterIsInstance<MirrorValue.Type>()

        val generatedModuleName = userModule.toClassName().assistedInjectModuleName()
        val referencesGeneratedModule = includes
            .map { it.toTypeName() }
            .any { it == generatedModuleName }
        if (userModule.hasAnnotation("dagger.hilt.InstallIn")) {
          if (referencesGeneratedModule) {
            error("@AssistedModule's @Module must not include ${generatedModuleName.simpleName()} if @InstallIn is used",
              userModule)
          }
        } else {
          if (!referencesGeneratedModule) {
            error("@AssistedModule's @Module must include ${generatedModuleName.simpleName()}",
              userModule)
          }
        }
      }
    }

    return false
  }

  private fun RoundEnvironment.findAssistedModuleElementsOrNull(): AssistedModuleElements? {
    val assistedModules = findElementsAnnotatedWith<AssistedModule>().castEach<TypeElement>()
    if (assistedModules.isEmpty()) {
      return null
    }
    if (assistedModules.size > 1) {
      assistedModules.forEach {
        error("Multiple @AssistedModule-annotated modules found.", it)
      }
      return null
    }

    val assistedModule = assistedModules.single()
    if (!assistedModule.hasAnnotation("dagger.Module")) {
      error("@AssistedModule must also be annotated as a Dagger @Module", assistedModule)
      return null
    }

    val factoryTypeElements = unprocessedFactoryNames
        .map(elements::getTypeElement)
        // Ignore malformed factories without enclosing types. The other processor will validate.
        .associateBy { it.enclosingElement as? TypeElement }
        .filterNotNullKeys()

    return AssistedModuleElements(assistedModule, factoryTypeElements)
  }

  private fun AssistedModuleElements.toAssistedInjectionModule(): AssistedInjectionModule {
    val moduleName = moduleType.toClassName()
    val targetNameToFactoryNames = targetTypeToFactoryType
        .map { (target, factory) -> target.asType().toTypeName() to factory.toClassName() }
        .toMap(TreeMap())
    val public = Modifier.PUBLIC in moduleType.modifiers
    val extraAnnotations = listOfNotNull(
      createGeneratedAnnotation(sourceVersion, elements),
      generateHiltAnnotation(moduleType, elements)
    )
    return AssistedInjectionModule(moduleName, public, targetNameToFactoryNames, extraAnnotations)
  }

  private fun writeAssistedModule(
    elements: AssistedModuleElements,
    module: AssistedInjectionModule
  ) {
    val generatedTypeSpec = module.brewJava()
        .toBuilder()
        .addOriginatingElement(elements.moduleType)
        .applyEach(elements.targetTypeToFactoryType.keys) {
          addOriginatingElement(it)
        }
        .build()
    JavaFile.builder(module.generatedType.packageName(), generatedTypeSpec)
        .addFileComment("Generated by @AssistedModule. Do not modify!")
        .build()
        .writeTo(filer)
  }

  private fun error(message: String, element: Element? = null) {
    messager.printMessage(ERROR, message, element)
  }

  private data class AssistedModuleElements(
    val moduleType: TypeElement,
    val targetTypeToFactoryType: Map<TypeElement, TypeElement>
  )
}

// TODO https://youtrack.jetbrains.com/issue/KT-4734
private fun <K : Any, V> Map<K?, V>.filterNotNullKeys(): Map<K, V> {
  @Suppress("UNCHECKED_CAST")
  return filterKeys { it != null } as Map<K, V>
}
