/*
 * Copyright (C) 2020 Square, Inc.
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
package com.squareup.inject.assisted.dagger2.processor.internal

import com.squareup.inject.assisted.processor.internal.getAnnotation
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements

internal fun generateHiltAnnotation(moduleType: TypeElement, elements: Elements): AnnotationSpec? {
  val installIn = moduleType.getAnnotation("dagger.hilt.InstallIn")
  if (installIn != null) {
    // If the @AssistedModule class also has an @InstallIn we just mirror the annotation.
    return AnnotationSpec.get(installIn)
  }
  // If there is no @InstallIn annotation we add the @DisableInstallInCheck annotation, if
  // it exists.
  val disableInstallInCheck = ClassName.get("dagger.hilt.migration", "DisableInstallInCheck")
  if (elements.getTypeElement(disableInstallInCheck.reflectionName()) != null) {
    return AnnotationSpec.builder(disableInstallInCheck).build()
  }
  return null
}
