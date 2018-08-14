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

import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreTypes
import com.google.common.base.Equivalence
import com.squareup.inject.assisted.processor.internal.BindingKey.Use.ASSISTED
import com.squareup.inject.assisted.processor.internal.BindingKey.Use.PROVIDED
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

/** Represents a type and an optional qualifier annotation for a binding. */
internal data class Key(
    /** A wrapped [TypeMirror] which provides correct `equals()` and `hashCode()` behavior. */
    val wrappedType: Equivalence.Wrapper<TypeMirror>,
    /** A wrapped [AnnotationMirror] which provides correct `equals()` and `hashCode()` behavior. */
    val wrappedQualifier: Equivalence.Wrapper<AnnotationMirror>?
) {
  constructor(type: TypeMirror, qualifier: AnnotationMirror? = null) : this(
      MoreTypes.equivalence().wrap(type),
      qualifier?.let { AnnotationMirrors.equivalence().wrap(it) })

  /** Create from `element`'s type and any qualifier annotation. */
  constructor(element: VariableElement) : this(
      element.asType(),
      element.annotationMirrors.find {
        it.annotationType.asElement().hasAnnotation("javax.inject.Qualifier")
      })

  val type get() = wrappedType.get()
  val qualifier get() = wrappedQualifier?.get()

  override fun toString() = qualifier?.let { "$it $type" } ?: type.toString()
}

/** Associates a [Key] with its desired use. */
internal data class BindingKey(
    val key: Key,
    val use: Use,
    val name: String
) {
  constructor(element: VariableElement, use: Use) :
      this(Key(element), use, element.simpleName.toString())

  enum class Use { PROVIDED, ASSISTED }

  val type: TypeName = TypeName.get(key.type)

  val providerType: TypeName
      get() {
        val type = ParameterizedTypeName.get(PROVIDER, type)
        key.qualifier?.let {
          return type.annotated(AnnotationSpec.get(it))
        }
        return type
      }

  fun bindingResolveCode(): CodeBlock = when (use) {
    PROVIDED -> CodeBlock.of("\$N.get()", name)
    ASSISTED -> CodeBlock.of("\$N", name)
  }

  companion object {
    private val PROVIDER = ClassName.get("javax.inject", "Provider")
  }
}
