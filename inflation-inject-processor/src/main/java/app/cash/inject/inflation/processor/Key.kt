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
package app.cash.inject.inflation.processor

import com.google.auto.common.MoreTypes
import app.cash.inject.inflation.processor.internal.hasAnnotation
import app.cash.inject.inflation.processor.internal.toAnnotationSpec
import app.cash.inject.inflation.processor.internal.toTypeName
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

/** Represents a type and an optional qualifier annotation for a binding. */
data class Key(
  val type: TypeName,
  val qualifier: AnnotationSpec? = null,
  val useProvider: Boolean = true
) {
  override fun toString() = qualifier?.let { "$it $type" } ?: type.toString()
}

/** Create a [Key] from this type and any qualifier annotation. */
fun VariableElement.asKey(mirror: TypeMirror = asType()): Key {
  val type = mirror.toTypeName()
  val qualifier = annotationMirrors.find {
    it.annotationType.asElement().hasAnnotation("javax.inject.Qualifier")
  }?.toAnnotationSpec()

  // Do not wrap a Provider inside another Provider.
  val provider = type is ParameterizedTypeName && type.rawType == JAVAX_PROVIDER

  val typeElement = if (type.isPrimitive) null else MoreTypes.asElement(mirror)
  // Dagger forbids requesting an @AssistedFactory-annotated type inside of a Provider.
  val daggerAssistedFactory = typeElement?.hasAnnotation("dagger.assisted.AssistedFactory") ?: false

  return Key(type, qualifier, !provider && !daggerAssistedFactory)
}
