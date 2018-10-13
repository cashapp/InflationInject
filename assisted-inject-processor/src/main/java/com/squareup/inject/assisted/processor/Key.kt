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

import com.squareup.inject.assisted.processor.internal.hasAnnotation
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.TypeName
import javax.lang.model.element.VariableElement

private val keyComparator = compareBy<Key>({ it.type.toString() }, { it.qualifier == null })

/** Represents a type and an optional qualifier annotation for a binding. */
data class Key(
  val type: TypeName,
  val qualifier: AnnotationSpec? = null
) : Comparable<Key> {
  override fun toString() = qualifier?.let { "$it $type" } ?: type.toString()
  override fun compareTo(other: Key) = keyComparator.compare(this, other)
}

/** Create a [Key] from this type and any qualifier annotation. */
fun VariableElement.asKey() = Key(TypeName.get(asType()),
    annotationMirrors.find {
      it.annotationType.asElement().hasAnnotation("javax.inject.Qualifier")
    }?.let { AnnotationSpec.get(it) })
