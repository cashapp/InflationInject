package com.squareup.inject.assisted.dagger2.processor

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

class SourceGeneratingProcessor(
  private val fqcn: String,
  private val content: String
) : AbstractProcessor() {
  override fun getSupportedAnnotationTypes() = setOf("*")

  private var done = false

  override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
    if (!done) {
      processingEnv.filer.createSourceFile(fqcn).openWriter().use { it.write(content) }
      done = true
    }
    return false
  }
}
