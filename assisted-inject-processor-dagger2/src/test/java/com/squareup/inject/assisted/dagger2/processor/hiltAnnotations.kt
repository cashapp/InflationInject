package com.squareup.inject.assisted.dagger2.processor

import com.google.testing.compile.JavaFileObjects

val hiltInstallIn = JavaFileObjects.forSourceString("dagger.hilt.InstallIn", """
  package dagger.hilt;

  import static java.lang.annotation.RetentionPolicy.CLASS;
  import static java.lang.annotation.ElementType.TYPE;

  import java.lang.annotation.Retention;
  import java.lang.annotation.Target;

  @Target(TYPE)
  @Retention(CLASS)
  public @interface InstallIn {
    Class<?>[] value();
  }
  """.trimIndent())
