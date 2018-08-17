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

import javax.annotation.processing.RoundEnvironment
import javax.lang.model.AnnotatedConstruct
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

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

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Iterable<*>.cast() = map { it as T }

inline fun <T : Any, I> T.applyEach(items: Iterable<I>, func: T.(I) -> Unit): T {
  items.forEach { item -> func(item) }
  return this
}
