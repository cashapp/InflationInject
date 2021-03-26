package app.cash.inject.inflation.processor

import app.cash.inject.inflation.processor.internal.applyEach
import app.cash.inject.inflation.processor.internal.joinToCode
import app.cash.inject.inflation.processor.internal.peerClassWithReflectionNesting
import app.cash.inject.inflation.processor.internal.rawClassName
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC

private val JAVAX_INJECT = ClassName.get("javax.inject", "Inject")
internal val JAVAX_PROVIDER = ClassName.get("javax.inject", "Provider")

/** The structure of an assisted injection factory. */
data class AssistedInjection(
  /** The type which will be instantiated inside the factory. */
  val targetType: TypeName,
  /** TODO */
  val dependencyRequests: List<DependencyRequest>,
  /** The factory interface type. */
  val factoryType: TypeName,
  /** Name of the factory's only method. */
  val factoryMethod: String,
  /** The factory method return type. [targetType] must be assignable to this type. */
  val returnType: TypeName = targetType,
  /**
   * The factory method keys. These default to the keys of the assisted [dependencyRequests]
   * and when supplied must always match them, but the order is allowed to be different.
   */
  val assistedKeys: List<Key> = dependencyRequests.filter { it.isAssisted }.map { it.key },
  /** An optional `@Generated` annotation marker. */
  val generatedAnnotation: AnnotationSpec? = null
) {
  private val keyToRequest = dependencyRequests.filter { it.isAssisted }.associateBy { it.key }
  init {
    check(keyToRequest.keys == assistedKeys.toSet()) {
      """
        assistedKeys must contain the same elements as the assisted dependencyRequests.

        * assistedKeys:
            $assistedKeys
        * assisted dependencyRequests:
            ${keyToRequest.keys}
      """.trimIndent()
    }
  }

  /** The type generated from [brewJava]. */
  val generatedType = targetType.rawClassName().assistedInjectFactoryName()

  private val providedKeys = dependencyRequests.filterNot { it.isAssisted }

  fun brewJava(): TypeSpec {
    return TypeSpec.classBuilder(generatedType)
        .addModifiers(PUBLIC, FINAL)
        .addSuperinterface(factoryType)
        .apply {
          if (generatedAnnotation != null) {
            addAnnotation(generatedAnnotation)
          }
        }
        .applyEach(providedKeys) {
          addField(it.providerType.withoutAnnotations(), it.name, PRIVATE, FINAL)
        }
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addAnnotation(JAVAX_INJECT)
            .applyEach(providedKeys) {
              addParameter(it.providerType, it.name)
              addStatement("this.$1N = $1N", it.name)
            }
            .build())
        .addMethod(MethodSpec.methodBuilder(factoryMethod)
            .addAnnotation(Override::class.java)
            .addModifiers(PUBLIC)
            .returns(returnType)
            .apply {
              if (targetType is ParameterizedTypeName) {
                addTypeVariables(targetType.typeArguments.filterIsInstance<TypeVariableName>())
              }
            }
            .applyEach(assistedKeys) { key ->
              val parameterName = keyToRequest.getValue(key).name
              addParameter(key.type, parameterName)
            }
            .addStatement("return new \$T(\n\$L)", targetType,
                dependencyRequests.map { it.argumentProvider }.joinToCode(",\n"))
            .build())
        .build()
  }
}

private val DependencyRequest.providerType: TypeName
  get() {
    val type = if (key.useProvider) {
      ParameterizedTypeName.get(JAVAX_PROVIDER, key.type.box())
    } else {
      key.type
    }
    key.qualifier?.let {
      return type.annotated(it)
    }
    return type
  }

private val DependencyRequest.argumentProvider
  get() = CodeBlock.of(if (isAssisted || !key.useProvider) "\$N" else "\$N.get()", name)

fun ClassName.assistedInjectFactoryName(): ClassName =
    peerClassWithReflectionNesting(simpleName() + "_InflationFactory")
