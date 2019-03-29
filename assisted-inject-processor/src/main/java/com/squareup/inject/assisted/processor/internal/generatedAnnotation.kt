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
package com.squareup.inject.assisted.processor.internal

import com.squareup.javapoet.AnnotationSpec
import javax.annotation.processing.Processor
import javax.lang.model.util.Elements

/**
 * Create a `@Generated` annotation using the correct type based on JDK version and availability on
 * the compilation classpath, a `value` with the fully-qualified class name of the calling
 * [Processor], and a comment pointing to this project's GitHub repo. Returns `null` if no
 * annotation type is available on the classpath.
 */
fun Processor.createGeneratedAnnotation(elements: Elements, comments: String = "https://github.com/square/AssistedInject"): AnnotationSpec? {
  val generatedType = elements.getTypeElement("javax.annotation.processing.Generated")
      ?: elements.getTypeElement("javax.annotation.Generated")
      ?: return null
  return AnnotationSpec.builder(generatedType.toClassName())
      .addMember("value", "\$S", javaClass.name)
      .addMember("comments", "\$S", comments)
      .build()
}
