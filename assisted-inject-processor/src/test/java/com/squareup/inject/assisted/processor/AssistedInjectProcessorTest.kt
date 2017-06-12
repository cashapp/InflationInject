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

class AssistedInjectProcessorTest {
  @Test fun simple() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;

      class Test {
        Test(Long foo, @Assisted String bar) {}

        @Assisted.Factory
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
      import javax.inject.Inject;
      import javax.inject.Provider;

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

      class Test {
        Test(String foo, @Assisted String bar) {}

        @Assisted.Factory
        interface Factory {
          Test create(String bar);
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Override;
      import java.lang.String;
      import javax.inject.Inject;
      import javax.inject.Provider;

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

  @Test fun providedQualifier() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import javax.inject.Qualifier;

      class Test {
        Test(@Id String foo, @Assisted String bar) {}

        @Assisted.Factory
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
      import javax.inject.Inject;
      import javax.inject.Provider;

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
      import javax.inject.Qualifier;

      class Test {
        Test(String foo, @Assisted @Id String bar) {}

        @Assisted.Factory
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
      import javax.inject.Inject;
      import javax.inject.Provider;

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
      import javax.inject.Named;

      class Test {
        Test(@Named("foo") String foo, @Assisted String bar) {}

        @Assisted.Factory
        interface Factory {
          Test create(String bar);
        }
      }
    """)

    val expected = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      import java.lang.Override;
      import java.lang.String;
      import javax.inject.Inject;
      import javax.inject.Named;
      import javax.inject.Provider;

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
      import javax.inject.Qualifier;

      class Test {
        Test(@Id String foo, @Assisted @Id String bar) {}

        @Assisted.Factory
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
      import javax.inject.Inject;
      import javax.inject.Provider;

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

  @Test fun noAssistedParametersFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;

      class Test {
        Test(String foo) {}

        @Assisted.Factory
        interface Factory {
          Test create();
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("Assisted injection requires at least one @Assisted parameter")
        .`in`(input).onLine(7)
  }

  @Test fun allAssistedParametersFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;

      class Test {
        Test(@Assisted String foo) {}

        @Assisted.Factory
        interface Factory {
          Test create(String foo);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("Assisted injection requires at least one non-@Assisted parameter.")
        .`in`(input).onLine(7)
  }

  @Test fun twoAssistedInjectConstructorsFails() {
    // TODO make this a valid case if the non-assisted parameters match?
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;

      class Test {
        Test(Long foo, @Assisted String bar) {}
        Test(Long foo, @Assisted Integer bar) {}

        @Assisted.Factory
        interface Factory {
          Test create(String bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("Multiple constructors define @Assisted parameters.")
        .`in`(input).onLine(6)
  }

  @Test fun noAssistedFactoryFails() {
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
        .withErrorContaining("No nested @Assisted.Factory found.")
        .`in`(input).onLine(6)
  }

  @Test fun twoAssistedFactoriesFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;

      class Test {
        Test(Long foo, @Assisted String bar) {}

        @Assisted.Factory
        interface FactoryOne {
          Test create(String bar);
        }

        @Assisted.Factory
        interface FactoryTwo {
          Test create(String bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("Multiple @Assisted.Factory types found.")
        .`in`(input).onLine(6)
  }

  @Test fun factorySignatureMismatchFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;

      class Test {
        Test(Long foo, @Assisted String bar) {}

        @Assisted.Factory
        interface Factory {
          Test create(Long bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        // TODO validate whole message
        .withErrorContaining(
            "Factory method parameters do not match constructor @Assisted parameters.")
        .`in`(input).onLine(11)
  }

  @Test fun factorySignatureWithQualifierMismatchOnFactoryFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import javax.inject.Qualifier;

      class Test {
        Test(Long foo, @Assisted String bar) {}

        @Assisted.Factory
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
        // TODO validate whole message
        .withErrorContaining(
            "Factory method parameters do not match constructor @Assisted parameters.")
        .`in`(input).onLine(12)
  }

  @Test fun factorySignatureWithQualifierMismatchOnConstructorFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import javax.inject.Qualifier;

      class Test {
        Test(Long foo, @Assisted @Id String bar) {}

        @Assisted.Factory
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
        // TODO validate whole message
        .withErrorContaining(
            "Factory method parameters do not match constructor @Assisted parameters.")
        .`in`(input).onLine(12)
  }

  @Test fun emptyFactoryFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;

      class Test {
        Test(Long foo, @Assisted String bar) {}

        @Assisted.Factory
        interface Factory {}
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("Factory interface does not define a factory method.")
        .`in`(input).onLine(10)
  }

  @Test fun nonInterfaceFactoryFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;

      class Test {
        Test(Long foo, @Assisted String bar) {}

        @Assisted.Factory
        abstract class Factory {
          abstract Test create(String bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("@Assisted.Factory must be an interface.")
        .`in`(input).onLine(10)
  }

  @Test fun multipleMethodsInFactoryFails() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;

      class Test {
        Test(Long foo, @Assisted String bar) {}

        @Assisted.Factory
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
        .`in`(input).onLine(10)
  }

  @Test fun factoryReturnsWrongType() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;

      class Test {
        Test(Long foo, @Assisted String bar) {}

        @Assisted.Factory
        interface Factory {
          Runnable create(String bar);
        }
      }
    """)

    assertAbout(javaSource())
        .that(input)
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("Factory method returns incorrect type.")
        .`in`(input).onLine(11)
  }

  @Test fun defaultMethod() {
    val input = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import java.util.Optional;

      class Test {
        Test(Long foo, @Assisted String bar) {}

        @Assisted.Factory
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
      import java.util.Optional;

      class Test {
        Test(Long foo, @Assisted String bar) {}

        @Assisted.Factory
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

      class Test {
        Test(Long foo, @Assisted String bar) {}

        @Assisted.Factory
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

      class Test {
        Test(Long foo, @Assisted String bar) {}

        @Assisted.Factory
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

      class Test {
        Test(Long foo, @Assisted String bar) {}
      }
    """)
    val factory = JavaFileObjects.forSourceString("test.Factory", """
      package test;

      import com.squareup.inject.assisted.Assisted;

      @Assisted.Factory
      interface Factory {
        Test create(String bar);
      }
    """)

    assertAbout(javaSources())
        .that(listOf(test, factory))
        .processedWith(AssistedInjectProcessor())
        .failsToCompile()
        .withErrorContaining("@Assisted.Factory must be declared as a nested type.")
        .`in`(factory).onLine(7)
  }
}
