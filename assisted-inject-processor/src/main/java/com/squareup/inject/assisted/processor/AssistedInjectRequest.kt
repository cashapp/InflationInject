package com.squareup.inject.assisted.processor

import com.squareup.inject.assisted.processor.internal.applyEach
import com.squareup.inject.assisted.processor.internal.applyEachIndexed
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.TypeElement

private val JAVAX_INJECT = ClassName.get("javax.inject", "Inject")
private val JAVAX_PROVIDER = ClassName.get("javax.inject", "Provider")

data class AssistedInjectRequest(
    val type: TypeElement,
    val factoryType: TypeElement,
    val factoryMethod: ExecutableElement,
    val parameterKeys: List<ParameterKey>
) {
  val generatedClassName: ClassName =
      ClassName.get(type).peerClass(type.simpleName.toString() + SUFFIX)

  fun brewJava(): TypeSpec {
    val typeName = TypeName.get(type.asType())

    val (assistedKeys, providedKeys) = parameterKeys.partition(ParameterKey::isAssisted)
    return TypeSpec.classBuilder(generatedClassName)
        .addModifiers(PUBLIC, FINAL)
        .addSuperinterface(ClassName.get(factoryType))
        .addOriginatingElement(type)
        .addOriginatingElement(factoryType)
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
        .addMethod(MethodSpec.methodBuilder(factoryMethod.simpleName.toString())
            .addAnnotation(Override::class.java)
            .addModifiers(PUBLIC)
            .returns(TypeName.get(factoryMethod.returnType))
            .apply {
              if (typeName is ParameterizedTypeName) {
                addTypeVariables(typeName.typeArguments.filterIsInstance<TypeVariableName>())
              }
            }
            .applyEach(assistedKeys) { key ->
              addParameter(key.type, key.name)
            }
            .addCode("$[return new \$T(\n", typeName)
            .applyEachIndexed(parameterKeys) { index, key ->
              if (index > 0) addCode(",\n")
              addCode(key.argumentProvider)
            }
            .addCode(");$]\n")
            .build())
        .build()
  }

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

  companion object {
    const val SUFFIX = "_AssistedFactory"
  }
}
