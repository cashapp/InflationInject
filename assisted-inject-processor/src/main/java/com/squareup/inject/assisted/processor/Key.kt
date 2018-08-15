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
package com.squareup.inject.assisted.processor

import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreTypes
import com.squareup.inject.assisted.processor.internal.hasAnnotation
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

/** Represents a type and an optional qualifier annotation for a binding. */
data class Key(
    val type: TypeMirror,
    val qualifier: AnnotationMirror?
) {
  override fun toString() = qualifier?.let { "$it $type" } ?: type.toString()

  // Wrap type and qualifier so that we can provide accurate equals and hashCode.
  private val wrappedType = MoreTypes.equivalence().wrap(type)
  private val wrappedQualifier = AnnotationMirrors.equivalence().wrap(qualifier)

  override fun hashCode() = wrappedType.hashCode() * 37 + wrappedQualifier.hashCode()
  override fun equals(other: Any?) =
      other is Key && wrappedType == other.wrappedType && wrappedQualifier == other.wrappedQualifier
}

/** Create from this type and any qualifier annotation. */
fun VariableElement.asKey() = Key(asType(),
    annotationMirrors.find {
      it.annotationType.asElement().hasAnnotation("javax.inject.Qualifier")
    })
