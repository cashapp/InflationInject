package com.squareup.inject.assisted.processor

import com.squareup.inject.assisted.processor.internal.applyEach
import com.squareup.inject.assisted.processor.internal.peerClassWithReflectionNesting
import com.squareup.inject.assisted.processor.internal.rawClassName
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
private val JAVAX_PROVIDER = ClassName.get("javax.inject", "Provider")

/** The structure of an assisted injection factory. */
data class AssistedInjection(
  /** The type which will be instantiated inside the factory. */
  val targetType: TypeName,
  /** TODO */
  val dependencyRequests: List<DependencyRequest>,
  /** The factory interface type. */
  val factoryType: ClassName,
  /** Name of the factory's only method. */
  val factoryMethod: String,
  /** The factory method return type. [targetType] must be assignable to this type. */
  val returnType: TypeName = targetType,
  /**
   * The factory method keys. These default to the keys of the assisted [dependencyRequests]
   * and when supplied must always match them, but the order is allowed to be different.
   */
  val assistedKeys: List<NamedKey> = dependencyRequests.filter { it.isAssisted }.map { it.namedKey }
) {
  init {
    val requestKeys = dependencyRequests.filter { it.isAssisted }.map { it.namedKey }
    check(requestKeys.sorted() == assistedKeys.sorted()) {
      """
        assistedKeys must contain the same elements as the assisted dependencyRequests.

        * assistedKeys:
            $assistedKeys
        * assisted dependencyRequests:
            $requestKeys
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
            .applyEach(assistedKeys) { namedKey ->
              addParameter(namedKey.key.type, namedKey.name)
            }
            .addStatement("return new \$T(\n\$L)", targetType,
                dependencyRequests.map { it.argumentProvider }.joinToCode(",\n"))
            .build())
        .build()
  }
}

private fun Iterable<CodeBlock>.joinToCode(separator: String) = CodeBlock.join(this, separator)

/** True when this key represents a parameterized JSR 330 `Provider`. */
private val Key.isProvider get() = type is ParameterizedTypeName && type.rawType == JAVAX_PROVIDER

private val DependencyRequest.providerType: TypeName
  get() {
    val type = if (key.isProvider) {
      key.type // Do not wrap a Provider inside another Provider.
    } else {
      ParameterizedTypeName.get(JAVAX_PROVIDER, key.type.box())
    }
    key.qualifier?.let {
      return type.annotated(it)
    }
    return type
  }

private val DependencyRequest.argumentProvider
  get() = CodeBlock.of(if (isAssisted || key.isProvider) "\$N" else "\$N.get()", name)

fun ClassName.assistedInjectFactoryName(): ClassName =
    peerClassWithReflectionNesting(simpleName() + "_AssistedFactory")
