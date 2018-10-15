package com.squareup.inject.inflation.processor

import com.squareup.inject.assisted.processor.assistedInjectFactoryName
import com.squareup.inject.assisted.processor.internal.applyEach
import com.squareup.inject.assisted.processor.internal.peerClassWithReflectionNesting
import com.squareup.inject.assisted.processor.internal.rawClassName
import com.squareup.inject.inflation.ViewFactory
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC

private val MODULE = ClassName.get("dagger", "Module")
private val BINDS = ClassName.get("dagger", "Binds")
private val INTO_MAP = ClassName.get("dagger.multibindings", "IntoMap")
private val STRING_KEY = ClassName.get("dagger.multibindings", "StringKey")

data class InflationInjectionModule(
  val moduleName: ClassName,
  val public: Boolean,
  val injectedNames: List<TypeName>,
  /** An optional `@Generated` annotation marker. */
  val generatedAnnotation: AnnotationSpec? = null
) {
  val generatedType = moduleName.inflationInjectModuleName()

  fun brewJava(): TypeSpec {
    return TypeSpec.classBuilder(generatedType)
        .addAnnotation(MODULE)
        .apply {
          if (generatedAnnotation != null) {
            addAnnotation(generatedAnnotation)
          }
        }
        .addModifiers(ABSTRACT)
        .apply {
          if (public) {
            addModifiers(PUBLIC)
          }
        }
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(PRIVATE)
            .build())
        .applyEach(injectedNames) { injectedName ->
          val rawName = injectedName.rawClassName()
          addMethod(MethodSpec.methodBuilder(rawName.bindMethodName())
              .addAnnotation(BINDS)
              .addAnnotation(INTO_MAP)
              .addAnnotation(AnnotationSpec.builder(STRING_KEY)
                  .addMember("value", "\$S", injectedName.toString())
                  .build())
              .addModifiers(ABSTRACT)
              .returns(ViewFactory::class.java)
              .addParameter(rawName.assistedInjectFactoryName(), "factory")
              .build())
        }
        .build()
  }
}

private fun ClassName.bindMethodName() = "bind_" + reflectionName().replace('.', '_')

fun ClassName.inflationInjectModuleName(): ClassName =
    peerClassWithReflectionNesting("InflationInject_" + simpleName())
