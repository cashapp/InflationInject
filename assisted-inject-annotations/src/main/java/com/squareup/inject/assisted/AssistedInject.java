/*
 * Copyright (C) 2018 Square, Inc.
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
package com.squareup.inject.assisted;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Identifies a constructor participating in assisted injection.
 * <p>
 * Injectable constructors are annotated with {@code @AssistedInject} and accept two more
 * dependencies as arguments. {@code @AssistedInject} can apply to at most one constructor per
 * class.
 * <p>
 * Arguments can be a mix of <em>assisted</em> or <em>provided</em>. Assisted arguments are those
 * annotated with {@link Assisted @Assisted} and will be supplied by the caller. Provided arguments
 * are all others and will be supplied by your dependency injector. At least one assisted and one
 * provided argument must be present.
 * <p>
 * Both assisted and provided arguments can have qualifier annotations. Since assisted and provider
 * arguments are resolved separately, the same qualifier can be used for both on a single
 * constructor.
 * <p>
 * Each type with an {@code @AssistedInject}-annotated constructor must also contain a nested
 * interface annotated with {@link Factory @AssistedInject.Factory}. The interface must have a
 * single method that returns the enclosing type and arguments which match the assisted arguments
 * of the enclosing {@code @AssistedInject}-annotated constructor.
 */
@Target(CONSTRUCTOR) @Retention(CLASS)
public @interface AssistedInject {
  @Target(TYPE) @Retention(CLASS)
  @interface Factory {
  }
}
