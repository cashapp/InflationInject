package com.squareup.inject.inflation.processor

import com.google.auto.service.AutoService
import com.squareup.inject.assisted.processor.AssistedInjection
import com.squareup.inject.assisted.processor.Key
import com.squareup.inject.assisted.processor.NamedKey
import com.squareup.inject.assisted.processor.asDependencyRequest
import com.squareup.inject.assisted.processor.internal.MirrorValue
import com.squareup.inject.assisted.processor.internal.applyEach
import com.squareup.inject.assisted.processor.internal.cast
import com.squareup.inject.assisted.processor.internal.castEach
import com.squareup.inject.assisted.processor.internal.createGeneratedAnnotation
import com.squareup.inject.assisted.processor.internal.filterNotNullValues
import com.squareup.inject.assisted.processor.internal.findElementsAnnotatedWith
import com.squareup.inject.assisted.processor.internal.getAnnotation
import com.squareup.inject.assisted.processor.internal.getValue
import com.squareup.inject.assisted.processor.internal.hasAnnotation
import com.squareup.inject.assisted.processor.internal.toClassName
import com.squareup.inject.assisted.processor.internal.toTypeName
import com.squareup.inject.inflation.InflationInject
import com.squareup.inject.inflation.InflationModule
import com.squareup.inject.inflation.ViewFactory
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.AGGREGATING
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
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.STATIC
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.WARNING

@IncrementalAnnotationProcessor(AGGREGATING)
@AutoService(Processor::class)
class InflationInjectProcessor : AbstractProcessor() {
  override fun getSupportedSourceVersion() = SourceVersion.latest()
  override fun getSupportedAnnotationTypes() = setOf(
      InflationInject::class.java.canonicalName,
      InflationModule::class.java.canonicalName)

  override fun init(env: ProcessingEnvironment) {
    super.init(env)
    sourceVersion = env.sourceVersion
    messager = env.messager
    filer = env.filer
    types = env.typeUtils
    elements = env.elementUtils
    viewType = elements.getTypeElement("android.view.View").asType()
  }

  private lateinit var sourceVersion: SourceVersion
  private lateinit var messager: Messager
  private lateinit var filer: Filer
  private lateinit var types: Types
  private lateinit var elements: Elements
  private lateinit var viewType: TypeMirror

  private var userModule: String? = null

  override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val inflationInjectElements = roundEnv.findInflationInjectCandidateTypeElements()
        .mapNotNull { it.toInflationInjectElementsOrNull() }

    inflationInjectElements
        .associateWith { it.toAssistedInjectionOrNull() }
        .filterNotNullValues()
        .forEach(::writeInflationInject)

    val inflationModuleElements = roundEnv.findInflationModuleTypeElement()
        ?.toInflationModuleElementsOrNull(inflationInjectElements)

    if (inflationModuleElements != null) {
      val moduleType = inflationModuleElements.moduleType

      val userModuleFqcn = userModule
      if (userModuleFqcn != null) {
        val userModuleType = elements.getTypeElement(userModuleFqcn)
        error("Multiple @InflationModule-annotated modules found.", userModuleType)
        error("Multiple @InflationModule-annotated modules found.", moduleType)
        userModule = null
      } else {
        userModule = moduleType.qualifiedName.toString()

        val inflationInjectionModule = inflationModuleElements.toInflationInjectionModule()
        writeInflationModule(inflationModuleElements, inflationInjectionModule)
      }
    }

    // Wait until processing is ending to validate that the @InflationModule's @Module annotation
    // includes the generated type.
    if (roundEnv.processingOver()) {
      val userModuleFqcn = userModule
      if (userModuleFqcn != null) {
        // In the processing round in which we handle the @InflationModule the @Module annotation's
        // includes contain an <error> type because we haven't generated the inflation module yet.
        // As a result, we need to re-lookup the element so that its referenced types are available.
        val userModule = elements.getTypeElement(userModuleFqcn)

        // Previous validation guarantees this annotation is present.
        val moduleAnnotation = userModule.getAnnotation("dagger.Module")!!
        // Dagger guarantees this property is present and is an array of types or errors.
        val includes = moduleAnnotation.getValue("includes", elements)!!
            .cast<MirrorValue.Array>()
            .filterIsInstance<MirrorValue.Type>()

        val generatedModuleName = userModule.toClassName().inflationInjectModuleName()
        val referencesGeneratedModule = includes
            .map { it.toTypeName() }
            .any { it == generatedModuleName }
        if (!referencesGeneratedModule) {
          error("@InflationModule's @Module must include ${generatedModuleName.simpleName()}",
              userModule)
        }
      }
    }

    return false
  }

  /**
   * Find [TypeElement]s which are candidates for assisted injection by having a constructor
   * annotated with [InflationInject].
   */
  private fun RoundEnvironment.findInflationInjectCandidateTypeElements(): List<TypeElement> {
    return findElementsAnnotatedWith<InflationInject>()
        .map { it.enclosingElement as TypeElement }
  }

  /**
   * From this [TypeElement] which is a candidate for inflation injection, find and validate the
   * syntactical elements required to generate the factory:
   * - Non-private, non-inner target type
   * - Single non-private target constructor
   */
  private fun TypeElement.toInflationInjectElementsOrNull(): InflationInjectElements? {
    var valid = true

    if (PRIVATE in modifiers) {
      error("@InflationInject-using types must not be private", this)
      valid = false
    }
    if (enclosingElement.kind == CLASS && STATIC !in modifiers) {
      error("Nested @InflationInject-using types must be static", this)
      valid = false
    }
    if (!types.isSubtype(asType(), viewType)) {
      error("@InflationInject-using types must be subtypes of View", this)
      valid = false
    }

    val constructors = enclosedElements
        .filter { it.kind == CONSTRUCTOR }
        .filter { it.hasAnnotation<InflationInject>() }
        .castEach<ExecutableElement>()
    if (constructors.size > 1) {
      error("Multiple @InflationInject-annotated constructors found.", this)
      valid = false
    }

    if (!valid) return null

    val constructor = constructors.single()
    if (PRIVATE in constructor.modifiers) {
      error("@InflationInject constructor must not be private.", constructor)
      return null
    }

    return InflationInjectElements(this, constructor)
  }

  /**
   * From this [InflationInjectElements], parse and validate the semantic information of the
   * elements which is required to generate the factory:
   * - Unqualified assisted parameters of Context and AttributeSet
   * - At least one provided parameter and no duplicates
   */
  private fun InflationInjectElements.toAssistedInjectionOrNull(): AssistedInjection? {
    var valid = true

    val requests = targetConstructor.parameters.map { it.asDependencyRequest() }
    val (assistedRequests, providedRequests) = requests.partition { it.isAssisted }
    val assistedKeys = assistedRequests.map { it.namedKey }
    if (assistedKeys.toSet() != FACTORY_KEYS.toSet()) {
      error("""
        Inflation injection requires Context and AttributeSet @Assisted parameters.
          Found:
            $assistedKeys
          Expected:
            $FACTORY_KEYS
        """.trimIndent(), targetConstructor)
      valid = false
    }
    if (providedRequests.isEmpty()) {
      warn("Inflation injection requires at least one non-@Assisted parameter.", targetConstructor)
    } else {
      val providedDuplicates = providedRequests.groupBy { it.key }.filterValues { it.size > 1 }
      if (providedDuplicates.isNotEmpty()) {
        error("Duplicate non-@Assisted parameters declared. Forget a qualifier annotation?"
            + providedDuplicates.values.flatten().joinToString("\n * ", prefix = "\n * "),
            targetConstructor)
        valid = false
      }
    }

    if (!valid) return null

    val targetType = targetType.asType().toTypeName()
    val generatedAnnotation = createGeneratedAnnotation(sourceVersion, elements)
    return AssistedInjection(targetType, requests, FACTORY, "create", VIEW,
        FACTORY_KEYS, generatedAnnotation)
  }

  private fun writeInflationInject(elements: InflationInjectElements, injection: AssistedInjection) {
    val generatedTypeSpec = injection.brewJava()
        .toBuilder()
        .addOriginatingElement(elements.targetType)
        .build()
    JavaFile.builder(injection.generatedType.packageName(), generatedTypeSpec)
        .addFileComment("Generated by @InflationInject. Do not modify!")
        .build()
        .writeTo(filer)
  }

  /**
   * Find and validate a [TypeElement] of the inflation module by being annotated
   * [InflationModule].
   */
  private fun RoundEnvironment.findInflationModuleTypeElement(): TypeElement? {
    val inflationModules = findElementsAnnotatedWith<InflationModule>().castEach<TypeElement>()
    if (inflationModules.size > 1) {
      inflationModules.forEach {
        error("Multiple @InflationModule-annotated modules found.", it)
      }
      return null
    }

    return inflationModules.singleOrNull()
  }

  private fun TypeElement.toInflationModuleElementsOrNull(
    inflationInjectElements: List<InflationInjectElements>
  ): InflationModuleElements? {
    if (!hasAnnotation("dagger.Module")) {
      error("@InflationModule must also be annotated as a Dagger @Module", this)
      return null
    }

    val inflationTargetTypes = inflationInjectElements.map { it.targetType }
    return InflationModuleElements(this, inflationTargetTypes)
  }

  private fun InflationModuleElements.toInflationInjectionModule(): InflationInjectionModule {
    val moduleName = moduleType.toClassName()
    val inflationNames = inflationTypes.map { it.toClassName() }
    val public = Modifier.PUBLIC in moduleType.modifiers
    val generatedAnnotation = createGeneratedAnnotation(sourceVersion, elements)
    return InflationInjectionModule(moduleName, public, inflationNames, generatedAnnotation)
  }

  private fun writeInflationModule(
    elements: InflationModuleElements,
    module: InflationInjectionModule
  ) {
    val generatedTypeSpec = module.brewJava()
        .toBuilder()
        .addOriginatingElement(elements.moduleType)
        .applyEach(elements.inflationTypes) {
          addOriginatingElement(it)
        }
        .build()
    JavaFile.builder(module.generatedType.packageName(), generatedTypeSpec)
        .addFileComment("Generated by @InflationModule. Do not modify!")
        .build()
        .writeTo(filer)
  }

  private fun warn(message: String, element: Element? = null) {
    messager.printMessage(WARNING, message, element)
  }

  private fun error(message: String, element: Element? = null) {
    messager.printMessage(ERROR, message, element)
  }

  private data class InflationInjectElements(
    val targetType: TypeElement,
    val targetConstructor: ExecutableElement
  )

  private data class InflationModuleElements(
    val moduleType: TypeElement,
    val inflationTypes: List<TypeElement>
  )
}

private val VIEW = ClassName.get("android.view", "View")
private val FACTORY = ViewFactory::class.toClassName()
private val FACTORY_KEYS = listOf(
    NamedKey(Key(ClassName.get("android.content", "Context")), "context"),
    NamedKey(Key(ClassName.get("android.util", "AttributeSet")), "attrs"))
