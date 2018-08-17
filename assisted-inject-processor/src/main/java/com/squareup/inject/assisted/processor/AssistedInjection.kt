package com.squareup.inject.assisted.processor

import com.squareup.inject.assisted.processor.internal.applyEach
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
private val JAVAX_PROVIDER = ClassName.get("javax.inject", "Provider")

/** The structure of an assisted injection factory. */
data class AssistedInjection(
  /** The type which will be instantiated inside the factory. */
  val targetType: TypeName,
  /** TODO */
  val parameterKeys: List<ParameterKey>,
  /** The factory interface type. */
  val factoryType: ClassName,
  /** Name of the factory's only method. */
  val factoryMethod: String,
  /** The factory method return type. [targetType] must be assignable to this type. */
  val returnType: TypeName = targetType,
  /**
   * The factory method parameter keys. These default to the assisted [parameterKeys] and when
   * supplied must always match them, but the order is allowed to be different.
   */
  val assistedKeys: List<ParameterKey> = parameterKeys.filter(ParameterKey::isAssisted)
) {
  init {
    val assistedParameterKeys = parameterKeys.filter(ParameterKey::isAssisted)
    check(assistedParameterKeys.toSet() == assistedKeys.toSet()) {
      """
        assistedKeys must contains the same elements as the assisted parameterKeys.

        * assistedKeys:
            $assistedKeys
        * assisted parameterKeys:
            $assistedParameterKeys
      """.trimIndent()
    }
  }

  /** The type generated from [brewJava]. */
  val generatedType = targetType.asClassName().assistedInjectFactoryName()

  private val providedKeys = parameterKeys.filterNot(ParameterKey::isAssisted)

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
            .applyEach(assistedKeys) { key ->
              addParameter(key.type, key.name)
            }
            .addStatement("return new \$T(\n\$L)", targetType,
                parameterKeys.map(ParameterKey::argumentProvider).joinToCode(",\n"))
            .build())
        .build()
  }
}

// TODO https://github.com/square/javapoet/issues/671
private fun TypeName.asClassName() = when (this) {
  is ClassName -> this
  is ParameterizedTypeName -> rawType
  else -> throw IllegalStateException("Cannot extract raw type from $this")
}

private fun Iterable<CodeBlock>.joinToCode(separator: String) = CodeBlock.join(this, separator)

private val ParameterKey.type get() = TypeName.get(key.type)

private val ParameterKey.providerType: TypeName
  get() {
    val type = ParameterizedTypeName.get(JAVAX_PROVIDER, type)
    key.qualifier?.let {
      return type.annotated(AnnotationSpec.get(it))
    }
    return type
  }

private val ParameterKey.argumentProvider
  get() = CodeBlock.of(if (isAssisted) "\$N" else "\$N.get()", name)

fun ClassName.assistedInjectFactoryName(): ClassName =
    peerClass(simpleName() + "_AssistedFactory")
