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

import app.cash.inject.inflation.Inflated
import app.cash.inject.inflation.processor.internal.hasAnnotation
import javax.lang.model.element.VariableElement

/** Associates a [Key] with its desired use as assisted or not. */
data class DependencyRequest(
  val key: Key,
  /** True when fulfilled by the caller. Otherwise fulfilled by a JSR 330 provider. */
  val isAssisted: Boolean,
  val name: String
) {
  override fun toString() = (if (isAssisted) "@Inflated " else "") + "$key $name"
}

fun VariableElement.asDependencyRequest() =
    DependencyRequest(asKey(), hasAnnotation<Inflated>(), simpleName.toString())
