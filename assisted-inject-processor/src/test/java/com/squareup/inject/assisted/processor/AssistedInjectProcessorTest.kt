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

import com.google.common.truth.Truth.assertAbout
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory.javaSource
import com.google.testing.compile.JavaSourcesSubjectFactory.javaSources
import org.junit.Ignore
import org.junit.Test

private val GENERATED_TYPE = try {
  Class.forName("javax.annotation.processing.Generated")
  "javax.annotation.processing.Generated"
} catch (_: ClassNotFoundException) {
  "javax.annotation.Generated"
}

private const val GENERATED_ANNOTATION = """
@Generated(
  value = "com.squareup.inject.assisted.processor.AssistedInjectProcessor",
  comments = "https://github.com/square/AssistedInject"
)
"""

class AssistedInjectProcessorTest {
  @Test fun simple() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String bar);
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Long;
      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        private final Provider<Long> foo;

        @Inject public Test_AssistedFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public Test create(String bar) {
          return new Test(foo.get(), bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun provider() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;
      import javax.inject.Provider;

      class Test {
        @AssistedInject
        Test(Provider<Long> foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String bar);
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Long;
      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        private final Provider<Long> foo;

        @Inject public Test_AssistedFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public Test create(String bar) {
          return new Test(foo, bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun primitive() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String bar);
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Long;
      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        private final Provider<Long> foo;

        @Inject public Test_AssistedFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public Test create(String bar) {
          return new Test(foo.get(), bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun providedAndAssistedSameType() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(String foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String bar);
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        private final Provider<String> foo;

        @Inject public Test_AssistedFactory(Provider<String> foo) {
          this.foo = foo;
        }

        @Override public Test create(String bar) {
          return new Test(foo.get(), bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun duplicateProvided() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(String foo, @Assisted String bar, String baz) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String bar);
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        private final Provider<String> foo;
        private final Provider<String> baz;

        @Inject public Test_AssistedFactory(Provider<String> foo, Provider<String> baz) {
          this.foo = foo;
          this.baz = baz;
        }

        @Override public Test create(String bar) {
          return new Test(foo.get(), bar, baz.get());
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun duplicateAssisted() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(@Assisted String foo, String bar, @Assisted String baz) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String foo, String baz);
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        private final Provider<String> bar;

        @Inject public Test_AssistedFactory(Provider<String> bar) {
          this.bar = bar;
        }

        @Override public Test create(String foo, String baz) {
          return new Test(foo, bar.get(), baz);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun duplicateAssistedOutOfOrder() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(@Assisted String foo, String bar, @Assisted String baz) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String baz, String foo);
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        private final Provider<String> bar;

        @Inject public Test_AssistedFactory(Provider<String> bar) {
          this.bar = bar;
        }

        @Override public Test create(String baz, String foo) {
          return new Test(foo, bar.get(), baz);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun duplicateAssistedNameMismatch() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(@Assisted String foo, String bar, @Assisted String baz) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String foo, String fizz);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("""
          Factory method parameters do not match constructor @Assisted parameters. Both parameter type and name must match.
            Declared by constructor, unmatched in factory method:
             * java.lang.String baz
            Declared by factory method, unmatched in constructor:
             * java.lang.String fizz
        """.trimIndent())
        .`in`(input).onLine(13)
  }

  @Test
  fun singleNameMismatch() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(@Assisted String foo, String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String baz);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
  }


  @Test
  fun multipleRequestForSingleFactoryArgMismatch() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(@Assisted String faz, @Assisted String baz, String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String foo);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("""
          Factory method parameters do not match constructor @Assisted parameters. Both parameter type and name must match.
            Declared by constructor, unmatched in factory method:
             * java.lang.String baz
             * java.lang.String faz
            Declared by factory method, unmatched in constructor:
             * java.lang.String foo
        """.trimIndent())
        .`in`(input).onLine(13)
  }

  @Test
  fun multipleSameTypeNameMismatch() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(@Assisted String foo, @Assisted String faz, String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String foo, String baz);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("""
          Factory method parameters do not match constructor @Assisted parameters. Both parameter type and name must match.
            Declared by constructor, unmatched in factory method:
             * java.lang.String faz
            Declared by factory method, unmatched in constructor:
             * java.lang.String baz
        """.trimIndent())
        .`in`(input).onLine(13)
  }

  @Test fun providedQualifier() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;
      import javax.inject.Qualifier;

      class Test {
        @AssistedInject
        Test(@Id String foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String bar);
        }
      }

      @Qualifier
      @interface Id {}
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        private final Provider<String> foo;

        @Inject public Test_AssistedFactory(@Id Provider<String> foo) {
          this.foo = foo;
        }

        @Override public Test create(String bar) {
          return new Test(foo.get(), bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun assistedQualifier() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;
      import javax.inject.Qualifier;

      class Test {
        @AssistedInject
        Test(String foo, @Assisted @Id String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(@Id String bar);
        }
      }

      @Qualifier
      @interface Id {}
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        private final Provider<String> foo;

        @Inject public Test_AssistedFactory(Provider<String> foo) {
          this.foo = foo;
        }

        @Override public Test create(String bar) {
          return new Test(foo.get(), bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun nameQualifier() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;
      import javax.inject.Named;

      class Test {
        @AssistedInject
        Test(@Named("foo") String foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String bar);
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Named;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        private final Provider<String> foo;

        @Inject public Test_AssistedFactory(@Named("foo") Provider<String> foo) {
          this.foo = foo;
        }

        @Override public Test create(String bar) {
          return new Test(foo.get(), bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun providedAndAssistedQualifierSameType() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;
      import javax.inject.Qualifier;

      class Test {
        @AssistedInject
        Test(@Id String foo, @Assisted @Id String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(@Id String bar);
        }
      }

      @Qualifier
      @interface Id {}
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        private final Provider<String> foo;

        @Inject public Test_AssistedFactory(@Id Provider<String> foo) {
          this.foo = foo;
        }

        @Override public Test create(String bar) {
          return new Test(foo.get(), bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun noAssistedParametersWarns() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(String foo) {}

        @AssistedInject.Factory
        interface Factory {
          Test create();
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        private final Provider<String> foo;

        @Inject public Test_AssistedFactory(Provider<String> foo) {
          this.foo = foo;
        }

        @Override public Test create() {
          return new Test(foo.get());
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
        .withWarningContaining(
            "Assisted injection without at least one @Assisted parameter can use @Inject")
        .`in`(input).onLine(9)
  }

  @Test fun allAssistedParametersWarns() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(@Assisted String foo) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String foo);
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        @Inject public Test_AssistedFactory() {
        }

        @Override public Test create(String foo) {
          return new Test(foo);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
        .withWarningContaining(
            "Assisted injection without at least one non-@Assisted parameter doesn't need a factory")
        .`in`(input).onLine(9)
  }

  @Test fun twoAssistedInjectConstructorsFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}
        @AssistedInject
        Test(Long foo, @Assisted Integer bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("Multiple @AssistedInject-annotated constructors found.")
        .`in`(input).onLine(7)
  }

  @Test fun noAssistedFactoryFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("No nested @AssistedInject.Factory found.")
        .`in`(input).onLine(7)
  }

  @Test fun twoAssistedFactoriesFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface FactoryOne {
          Test create(String bar);
        }

        @AssistedInject.Factory
        interface FactoryTwo {
          Test create(String bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("Multiple @AssistedInject.Factory types found.")
        .`in`(input).onLine(7)
  }

  @Test fun factorySignatureMismatchFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(Long bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("""
          Factory method parameters do not match constructor @Assisted parameters. Both parameter type and name must match.
            Declared by constructor, unmatched in factory method:
             * java.lang.String bar
            Declared by factory method, unmatched in constructor:
             * java.lang.Long bar
        """.trimIndent())
        .`in`(input).onLine(13)
  }

  @Test fun factorySignatureMismatchWithFactoryMethodDefinedElsewhere() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory extends TestFactory {
        }
      }

      interface TestFactory {
        Test create(Long bar);
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("""
          Factory method parameters do not match constructor @Assisted parameters. Both parameter type and name must match.
            Declared by constructor, unmatched in factory method:
             * java.lang.String bar
            Declared by factory method, unmatched in constructor:
             * java.lang.Long bar
        """.trimIndent())
        .`in`(input).onLine(9)
  }

  @Test fun factorySignatureWithQualifierMismatchOnFactoryFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;
      import javax.inject.Qualifier;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(@Id String bar);
        }
      }

      @Qualifier
      @interface Id {}
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("""
          Factory method parameters do not match constructor @Assisted parameters. Both parameter type and name must match.
            Declared by constructor, unmatched in factory method:
             * java.lang.String bar
            Declared by factory method, unmatched in constructor:
             * @test.Id java.lang.String bar
        """.trimIndent())
        .`in`(input).onLine(14)
  }

  @Test fun factorySignatureWithQualifierMismatchOnConstructorFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;
      import javax.inject.Qualifier;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted @Id String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String bar);
        }
      }

      @Qualifier
      @interface Id {}
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("""
          Factory method parameters do not match constructor @Assisted parameters. Both parameter type and name must match.
            Declared by constructor, unmatched in factory method:
             * @test.Id java.lang.String bar
            Declared by factory method, unmatched in constructor:
             * java.lang.String bar
        """.trimIndent())
        .`in`(input).onLine(14)
  }

  @Test fun emptyFactoryFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {}
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("Factory interface does not define a factory method.")
        .`in`(input).onLine(12)
  }

  @Test fun nonInterfaceFactoryFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        abstract class Factory {
          abstract Test create(String bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("@AssistedInject.Factory must be an interface.")
        .`in`(input).onLine(12)
  }

  @Test fun multipleMethodsInFactoryFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String bar);
          Test create(Object bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("Factory interface defines multiple factory methods.")
        .`in`(input).onLine(12)
  }

  @Test fun multipleConstructorsWithoutAssistedInjectFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        Test(Long foo, String bar) {}
        Test(Long foo, Integer bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining(
            "Assisted injection requires an @AssistedInject-annotated constructor " +
                "with at least one @Assisted parameter.")
        .`in`(input).onLine(6)
  }

  // No known way to correctly check if a method return type is assignable to the injected
  // constructor type.
  @Ignore
  @Test fun factoryReturnsWrongTypeFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Runnable create(String bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining(
            "Factory method returns incorrect type. Must be Test or one of its supertypes.")
        .`in`(input).onLine(13)
  }

  @Test fun factoryReturnsAssignableType() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test implements TestBase {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {
          TestBase create(String bar);
        }
      }

      interface TestBase {}
    """)

    // Ensure a covariant return type is not used which creates two methods in the bytecode.
    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Long;
      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        private final Provider<Long> foo;

        @Inject public Test_AssistedFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public TestBase create(String bar) {
          return new Test(foo.get(), bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun assistedParameterOrderDifferentInFactory() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar, @Assisted Long baz) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(Long baz, String bar);
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Long;
      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        private final Provider<Long> foo;

        @Inject public Test_AssistedFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public Test create(Long baz, String bar) {
          return new Test(foo.get(), bar, baz);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun defaultMethod() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;
      import java.util.Optional;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String bar);

          default Test create(Optional<String> maybeBar) {
            return create(maybeBar.orElse("whatever"));
          }
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
  }

  @Ignore("Requires Java 9")
  @Test fun defaultAndPrivateMethod() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;
      import java.util.Optional;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String bar);

          default Test create(Optional<String> maybeBar) {
            return create(getBar(maybeBar));
          }

          private String getBar(Optional<String> maybeBar) {
            return maybeBar.orElse("whatever");
          }
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
  }

  @Test fun staticMethod() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String bar);

          static String getDefaultBar() {
            return "whatever";
          }
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
  }

  @Ignore("Requires Java 9")
  @Test fun privateStaticMethod() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String bar);

          static String getDefaultBar() {
            return getDefaultBarHelper();
          }

          private static String getDefaultBarHelper() {
            return "whatever";
          }
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
  }

  @Test fun factoryOnTopLevelTypeFails() {
    val test = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}
      }
    """)
    val factory = JavaFileObjects.forSourceString("test.Factory", """
      package test;

      import com.squareup.inject.assisted.AssistedInject;

      @AssistedInject.Factory
      interface Factory {
        Test create(String bar);
      }
    """)

    assertAbout(javaSources())
        .that(listOf(test, factory))
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("@AssistedInject.Factory must be declared as a nested type.")
        .`in`(factory).onLine(7)
  }

  @Test fun multipleTypeBounds() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test <T extends Test.A & Test.B> {
        interface A {}
        interface B {}

        @AssistedInject
        Test(Long foo, @Assisted T bar) {}

        @AssistedInject.Factory
        interface Factory {
          <T extends Test.A & Test.B> Test<T> create(T bar);
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Long;
      import java.lang.Override;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        private final Provider<Long> foo;

        @Inject
        public Test_AssistedFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override
        public <T extends Test.A & Test.B> Test<T> create(T bar) {
          return new Test<T>(
              foo.get(),
              bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun nested() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Outer {
        static class Test {
          @AssistedInject
          Test(Long foo, @Assisted String bar) {}

          @AssistedInject.Factory
          interface Factory {
            Test create(String bar);
          }
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Long;
      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Outer${'$'}Test_AssistedFactory implements Outer.Test.Factory {
        private final Provider<Long> foo;

        @Inject public Outer${'$'}Test_AssistedFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public Outer.Test create(String bar) {
          return new Outer.Test(foo.get(), bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun factoryMethodInSupertype() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      interface BaseThing {
        interface Factory {
          BaseThing create(String bar);
        }
      }

      class Test implements BaseThing {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory extends BaseThing.Factory {
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Long;
      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        private final Provider<Long> foo;

        @Inject public Test_AssistedFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public BaseThing create(String bar) {
          return new Test(foo.get(), bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun factoryMethodInSupertypeWithGenerics() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      interface BaseThing<T> {
        interface Factory<T> {
          BaseThing<T> create(T bar);
        }
      }

      class Test implements BaseThing<String> {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory extends BaseThing.Factory<String> {
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Long;
      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        private final Provider<Long> foo;

        @Inject public Test_AssistedFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public BaseThing<String> create(String bar) {
          return new Test(foo.get(), bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Ignore("https://github.com/square/AssistedInject/issues/116")
  @Test fun factoryMethodUsesConcreteGenericForClassTypeParameter() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      public final class Test<A> {
        @AssistedInject
        public Test(@Assisted String dependency) {}

        @AssistedInject.Factory
        interface Factory {
          Test<String> create(String dependency);
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;

      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        @Inject public Test_AssistedFactory() {}

        @Override public Test<String> create(String dependency) {
          return new Test<String>(dependency);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun nestedMustBeStatic() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Outer {
        class Test {
          @AssistedInject
          Test(Long foo, @Assisted String bar) {}

          @AssistedInject.Factory
          interface Factory {
            Test create(String bar);
          }
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("Nested @AssistedInject-using types must be static")
        .`in`(input).onLine(8)
  }

  @Test fun nestedCannotBePrivate() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Outer {
        private static class Test {
          @AssistedInject
          Test(Long foo, @Assisted String bar) {}

          @AssistedInject.Factory
          interface Factory {
            Test create(String bar);
          }
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("@AssistedInject-using types must not be private")
        .`in`(input).onLine(8)
  }

  @Test fun constructorCannotBePrivate() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        private Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {
          Test create(String bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("@AssistedInject constructor must not be private.")
        .`in`(input).onLine(9)
  }

  @Test fun factoryCannotBePrivate() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        private interface Factory {
          Test create(String bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("@AssistedInject.Factory must not be private.")
        .`in`(input).onLine(12)
  }

  @Test fun assistedFailsIfUsedOnRegularMethodNotCalledCopy() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;

      class Test {
        void foo(Long foo, @Assisted String bar) {}
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("@Assisted is only supported on constructor parameters")
        .`in`(input).onLine(7)
  }

  @Test fun assistedIsAllowedOnMethodCalledCopy() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;
      
      public final class Test {
        @com.squareup.inject.assisted.AssistedInject()
        public Test(@com.squareup.inject.assisted.Assisted()
        java.lang.String foo) {
          super();
        }
        
        public final test.Test copy(@com.squareup.inject.assisted.Assisted()
        java.lang.String foo) {
          return null;
        }
        
        @com.squareup.inject.assisted.AssistedInject.Factory()
        public static abstract interface Factory {
          @org.jetbrains.annotations.NotNull()
          public abstract test.Test create(@org.jetbrains.annotations.NotNull()
          java.lang.String foo);
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;
      
      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      
      $GENERATED_ANNOTATION
      public final class Test_AssistedFactory implements Test.Factory {
        @Inject
        public Test_AssistedFactory() {
        }
      
        @Override
        public Test create(String foo) {
          return new Test(
              foo);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun assistedFailsIfUsedOnBareConstructor() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;

      class Test {
        Test(Long foo, @Assisted String bar) {}
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining(
            "@Assisted parameter use requires a constructor annotation such as @AssistedInject or @InflationInject")
        .`in`(input).onLine(7)
  }

  @Test fun assistedFailsIfUsedWithInject() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import javax.inject.Inject;

      class Test {
        @Inject
        Test(Long foo, @Assisted String bar) {}
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining(
            "@Assisted parameter does not work with @Inject! Use @AssistedInject or @InflationInject")
        .`in`(input).onLine(9)
  }
}
