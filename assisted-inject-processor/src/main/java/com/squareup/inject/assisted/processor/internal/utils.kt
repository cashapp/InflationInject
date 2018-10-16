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

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.AnnotatedConstruct
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ErrorType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.SimpleAnnotationValueVisitor6
import javax.lang.model.util.SimpleTypeVisitor6

/** Return a list of elements annotated with `T`. */
inline fun <reified T : Annotation> RoundEnvironment.findElementsAnnotatedWith(): Set<Element>
    = getElementsAnnotatedWith(T::class.java)

/** Return true if this [AnnotatedConstruct] is annotated with `T`. */
inline fun <reified T : Annotation> AnnotatedConstruct.hasAnnotation()
    = getAnnotation(T::class.java) != null

/** Return true if this [AnnotatedConstruct] is annotated with `qualifiedName`. */
fun AnnotatedConstruct.hasAnnotation(qualifiedName: String) = annotationMirrors
    .map { it.annotationType.asElement() as TypeElement }
    .any { it.qualifiedName.contentEquals(qualifiedName) }

/** Return the first annotation matching [qualifiedName] or null. */
fun AnnotatedConstruct.getAnnotation(qualifiedName: String) = annotationMirrors
    .firstOrNull {
      (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals(qualifiedName)
    }

fun AnnotationMirror.getValue(property: String, elements: Elements) = elements
    .getElementValuesWithDefaults(this)
    .entries
    .firstOrNull { it.key.simpleName.contentEquals(property) }
    ?.value
    ?.accept(MirrorValue.ValueVisitor, null)

sealed class MirrorValue {
  object Unmapped : MirrorValue()
  object Error : MirrorValue()

  data class Type(private val value: TypeMirror) : MirrorValue(), TypeMirror by value

  data class Array(
    private val value: List<MirrorValue>
  ) : MirrorValue(), List<MirrorValue> by value

  object ValueVisitor : SimpleAnnotationValueVisitor6<MirrorValue, Nothing?>() {
    override fun defaultAction(o: Any, ignored: Nothing?) = Unmapped

    override fun visitType(mirror: TypeMirror, ignored: Nothing?) = mirror.accept(TypeVisitor, null)

    override fun visitArray(values: List<AnnotationValue>, ignored: Nothing?) =
        Array(values.map { it.accept(this, null) })
  }
  private object TypeVisitor : SimpleTypeVisitor6<MirrorValue, Nothing?>() {
    override fun visitError(type: ErrorType, ignored: Nothing?) = Error
    override fun defaultAction(type: TypeMirror, ignored: Nothing?) = Type(type)
  }
}

// TODO Maybe replaced by https://youtrack.jetbrains.com/issue/KT-13814?
fun <K, V : Any> Iterable<K>.associateWithNotNull(func: (K) -> V?): Map<K, V> {
  val map = mutableMapOf<K, V>()
  for (key in this) {
    val value = func(key)
    if (value != null) {
      map[key] = value
    }
  }
  return map
}

/** Equivalent to `this as T` for use in function chains. */
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Any.cast(): T = this as T

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Iterable<*>.castEach() = map { it as T }

inline fun <T : Any, I> T.applyEach(items: Iterable<I>, func: T.(I) -> Unit): T {
  items.forEach { item -> func(item) }
  return this
}

/**
 * Like [ClassName.peerClass] except instead of honoring the enclosing class names they are
 * concatenated with `$` similar to the reflection name. `foo.Bar.Baz` invoking this function with
 * `Fuzz` will produce `foo.Baz$Fuzz`.
 */
fun ClassName.peerClassWithReflectionNesting(name: String): ClassName {
  var prefix = ""
  var peek = this
  while (true) {
    peek = peek.enclosingClassName() ?: break
    prefix = peek.simpleName() + "$" + prefix
  }
  return ClassName.get(packageName(), prefix + name)
}

// TODO https://github.com/square/javapoet/issues/671
fun TypeName.rawClassName(): ClassName = when (this) {
  is ClassName -> this
  is ParameterizedTypeName -> rawType
  else -> throw IllegalStateException("Cannot extract raw class name from $this")
}
