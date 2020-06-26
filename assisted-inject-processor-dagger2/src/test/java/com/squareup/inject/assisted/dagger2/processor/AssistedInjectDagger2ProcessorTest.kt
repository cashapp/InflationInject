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
package com.squareup.inject.assisted.dagger2.processor

import com.google.common.collect.Collections2
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
  value = "com.squareup.inject.assisted.dagger2.processor.AssistedInjectDagger2Processor",
  comments = "https://github.com/square/AssistedInject"
)
"""

class AssistedInjectDagger2ProcessorTest {
  @Test fun simple() {
    val test = JavaFileObjects.forSourceString("test.Test", """
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
    val testFactory = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      class Test_AssistedFactory implements Test.Factory {}
    """)
    val module = JavaFileObjects.forSourceString("test.TestModule", """
      package test;

      import com.squareup.inject.assisted.dagger2.AssistedModule;
      import dagger.Module;

      @AssistedModule
      @Module(includes = AssistedInject_TestModule.class)
      abstract class TestModule {}
    """)

    val expected = JavaFileObjects.forSourceString("test.AssistedInject_TestModule", """
      package test;

      import dagger.Binds;
      import dagger.Module;
      import $GENERATED_TYPE;

      @Module
      $GENERATED_ANNOTATION
      abstract class AssistedInject_TestModule {
        private AssistedInject_TestModule() {}

        @Binds abstract Test.Factory bind_test_Test(Test_AssistedFactory factory);
      }
    """)

    assertAbout(javaSources())
        .that(listOf(test, testFactory, module))
        .processedWith(AssistedInjectDagger2Processor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun moduleMethodsAreSorted() {
    val one = JavaFileObjects.forSourceString("test.One", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class One {
        @AssistedInject
        One(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {}
      }

      class One_AssistedFactory implements One.Factory {}
    """)
    val two = JavaFileObjects.forSourceString("test.Two", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Two {
        @AssistedInject
        Two(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {}
      }

      class Two_AssistedFactory implements Two.Factory {}
    """)
    val three = JavaFileObjects.forSourceString("test.Three", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Three {
        @AssistedInject
        Three(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {}
      }

      class Three_AssistedFactory implements Three.Factory {}
    """)
    val module = JavaFileObjects.forSourceString("test.TestModule", """
      package test;

      import com.squareup.inject.assisted.dagger2.AssistedModule;
      import dagger.Module;

      @AssistedModule
      @Module(includes = AssistedInject_TestModule.class)
      abstract class TestModule {}
    """)

    val expected = JavaFileObjects.forSourceString("test.AssistedInject_TestModule", """
      package test;

      import dagger.Binds;
      import dagger.Module;
      import $GENERATED_TYPE;

      @Module
      $GENERATED_ANNOTATION
      abstract class AssistedInject_TestModule {
        private AssistedInject_TestModule() {}

        @Binds abstract One.Factory bind_test_One(One_AssistedFactory factory);

        @Binds abstract Three.Factory bind_test_Three(Three_AssistedFactory factory);

        @Binds abstract Two.Factory bind_test_Two(Two_AssistedFactory factory);
      }
    """)

    for (items in Collections2.permutations(listOf(one, two, three))) {
      assertAbout(javaSources())
          .that(items + module)
          .processedWith(AssistedInjectDagger2Processor())
          .compilesWithoutError()
          .and()
          .generatesSources(expected)
    }
  }

  @Test fun public() {
    val test = JavaFileObjects.forSourceString("test.Test", """
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
    val testFactory = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      class Test_AssistedFactory implements Test.Factory {}
    """)
    val module = JavaFileObjects.forSourceString("test.TestModule", """
      package test;

      import com.squareup.inject.assisted.dagger2.AssistedModule;
      import dagger.Module;

      @AssistedModule
      @Module(includes = AssistedInject_TestModule.class)
      public abstract class TestModule {}
    """)

    val expected = JavaFileObjects.forSourceString("test.AssistedInject_TestModule", """
      package test;

      import dagger.Binds;
      import dagger.Module;
      import $GENERATED_TYPE;

      @Module
      $GENERATED_ANNOTATION
      public abstract class AssistedInject_TestModule {
        private AssistedInject_TestModule() {}

        @Binds abstract Test.Factory bind_test_Test(Test_AssistedFactory factory);
      }
    """)

    assertAbout(javaSources())
        .that(listOf(test, testFactory, module))
        .processedWith(AssistedInjectDagger2Processor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun nested() {
    val test = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Outer {
        static class Test {
          @AssistedInject
          Test(Long foo, @Assisted String bar) {}

          @AssistedInject.Factory
          interface Factory {}
        }
      }
    """)
    val testFactory = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      class Outer${'$'}Test_AssistedFactory implements Outer.Test.Factory {}
    """)
    val module = JavaFileObjects.forSourceString("test.TestModule", """
      package test;

      import com.squareup.inject.assisted.dagger2.AssistedModule;
      import dagger.Module;

      @AssistedModule
      @Module(includes = AssistedInject_TestModule.class)
      public abstract class TestModule {}
    """)

    val expected = JavaFileObjects.forSourceString("test.AssistedInject_TestModule", """
      package test;

      import dagger.Binds;
      import dagger.Module;
      import $GENERATED_TYPE;

      @Module
      $GENERATED_ANNOTATION
      public abstract class AssistedInject_TestModule {
        private AssistedInject_TestModule() {}

        @Binds abstract Outer.Test.Factory bind_test_Outer${'$'}Test(Outer${'$'}Test_AssistedFactory factory);
      }
    """)

    assertAbout(javaSources())
        .that(listOf(test, testFactory, module))
        .processedWith(AssistedInjectDagger2Processor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun moduleGeneratedByOtherProcessor() {
    val test = JavaFileObjects.forSourceString("test.Test", """
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
    val testFactory = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      class Test_AssistedFactory implements Test.Factory {}
    """)
    val moduleGeneratingProcessor = SourceGeneratingProcessor("test.TestModule", """
      package test;

      import com.squareup.inject.assisted.dagger2.AssistedModule;
      import dagger.Module;

      @AssistedModule
      @Module(includes = AssistedInject_TestModule.class)
      public abstract class TestModule {}
    """)

    val expected = JavaFileObjects.forSourceString("test.AssistedInject_TestModule", """
      package test;

      import dagger.Binds;
      import dagger.Module;
      import $GENERATED_TYPE;

      @Module
      $GENERATED_ANNOTATION
      public abstract class AssistedInject_TestModule {
        private AssistedInject_TestModule() {}

        @Binds abstract Test.Factory bind_test_Test(Test_AssistedFactory factory);
      }
    """)

    assertAbout(javaSources())
        .that(listOf(test, testFactory))
        .processedWith(AssistedInjectDagger2Processor(), moduleGeneratingProcessor)
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }

  @Test fun installInAnnotationCopiedToGeneratedModule() {
    val test = JavaFileObjects.forSourceString("test.Test", """
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
    val testFactory = JavaFileObjects.forSourceString("test.Test_AssistedFactory", """
      package test;

      class Test_AssistedFactory implements Test.Factory {}
    """)
    val module = JavaFileObjects.forSourceString("test.TestModule", """
      package test;

      import com.squareup.inject.assisted.dagger2.AssistedModule;
      import dagger.Module;
      import dagger.hilt.InstallIn;

      @Module
      @AssistedModule
      @InstallIn(String.class)
      abstract class TestModule {}
    """)

    val expected = JavaFileObjects.forSourceString("test.AssistedInject_TestModule", """
      package test;

      import dagger.Binds;
      import dagger.Module;
      import dagger.hilt.InstallIn;
      import java.lang.String;
      import $GENERATED_TYPE;

      @Module
      $GENERATED_ANNOTATION
      @InstallIn(String.class)
      abstract class AssistedInject_TestModule {
        private AssistedInject_TestModule() {}

        @Binds abstract Test.Factory bind_test_Test(Test_AssistedFactory factory);
      }
    """)

    assertAbout(javaSources())
        .that(listOf(hiltInstallIn, test, testFactory, module))
        .processedWith(AssistedInjectDagger2Processor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected)
  }


  @Test fun moduleWithoutModuleAnnotationFails() {
    val moduleOne = JavaFileObjects.forSourceString("test.OneModule", """
      package test;

      import com.squareup.inject.assisted.dagger2.AssistedModule;

      @AssistedModule
      abstract class OneModule {}
    """)

    assertAbout(javaSource())
        .that(moduleOne)
        .processedWith(AssistedInjectDagger2Processor())
        .failsToCompile()
        .withErrorContaining("@AssistedModule must also be annotated as a Dagger @Module")
        .`in`(moduleOne).onLine(7)
  }

  @Test fun nestedModule() {
    val test = JavaFileObjects.forSourceString("test.Test", """
      package test;

      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.assisted.AssistedInject;

      class Test {
        @AssistedInject
        Test(Long foo, @Assisted String bar) {}

        @AssistedInject.Factory
        interface Factory {}
      }

      class Test_AssistedFactory implements Test.Factory {}
    """)
    val module = JavaFileObjects.forSourceString("test.TestModule", """
      package test;

      import com.squareup.inject.assisted.dagger2.AssistedModule;
      import dagger.Module;

      class Outer {
          @AssistedModule
          @Module(includes = AssistedInject_Outer_TestModule.class)
          public abstract class TestModule {}
      }
    """)
    val expected = JavaFileObjects.forSourceString("test.AssistedInject_TestModule", """
      package test;

      import dagger.Binds;
      import dagger.Module;
      import $GENERATED_TYPE;

      @Module
      $GENERATED_ANNOTATION
      public abstract class AssistedInject_Outer_TestModule {
        private AssistedInject_Outer_TestModule() {}

        @Binds abstract Test.Factory bind_test_Test(Test_AssistedFactory factory);
      }
    """)

    assertAbout(javaSources())
      .that(listOf(test, module))
      .processedWith(AssistedInjectDagger2Processor())
      .compilesWithoutError()
      .and()
      .generatesSources(expected)
  }

  @Test fun moduleWithNoIncludesFails() {
    val moduleOne = JavaFileObjects.forSourceString("test.OneModule", """
      package test;

      import com.squareup.inject.assisted.dagger2.AssistedModule;
      import dagger.Module;

      @AssistedModule
      @Module
      abstract class OneModule {}
    """)

    assertAbout(javaSource())
        .that(moduleOne)
        .processedWith(AssistedInjectDagger2Processor())
        .failsToCompile()
        .withErrorContaining("@AssistedModule's @Module must include AssistedInject_OneModule")
        .`in`(moduleOne).onLine(9)
  }

  @Test fun moduleWithoutIncludeFails() {
    val moduleOne = JavaFileObjects.forSourceString("test.OneModule", """
      package test;

      import com.squareup.inject.assisted.dagger2.AssistedModule;
      import dagger.Module;

      @AssistedModule
      @Module(includes = TwoModule.class)
      abstract class OneModule {}

      @Module
      abstract class TwoModule {}
    """)

    assertAbout(javaSource())
        .that(moduleOne)
        .processedWith(AssistedInjectDagger2Processor())
        .failsToCompile()
        .withErrorContaining("@AssistedModule's @Module must include AssistedInject_OneModule")
        .`in`(moduleOne).onLine(9)
  }

  @Test fun moduleWithInstallInWithIncludeFails() {
    val module = JavaFileObjects.forSourceString("test.TestModule", """
      package test;

      import com.squareup.inject.assisted.dagger2.AssistedModule;
      import dagger.Module;
      import dagger.hilt.InstallIn;

      @AssistedModule
      @InstallIn(String.class)
      @Module(includes = AssistedInject_TestModule.class)
      abstract class TestModule {}
    """)

    assertAbout(javaSources())
        .that(listOf(hiltInstallIn, module))
        .processedWith(AssistedInjectDagger2Processor())
        .failsToCompile()
        .withErrorContaining(
            "@AssistedModule's @Module must not include AssistedInject_TestModule if @InstallIn is used")
        .`in`(module).onLine(11)
  }

  @Test fun multipleModulesFails() {
    val moduleOne = JavaFileObjects.forSourceString("test.OneModule", """
      package test;

      import com.squareup.inject.assisted.dagger2.AssistedModule;
      import dagger.Module;

      @AssistedModule
      @Module(includes = AssistedInject_OneModule.class)
      abstract class OneModule {}
    """)
    val moduleTwo = JavaFileObjects.forSourceString("test.TwoModule", """
      package test;

      import com.squareup.inject.assisted.dagger2.AssistedModule;
      import dagger.Module;

      @AssistedModule
      @Module(includes = AssistedInject_TwoModule.class)
      abstract class TwoModule {}
    """)

    assertAbout(javaSources())
        .that(listOf(moduleOne, moduleTwo))
        .processedWith(AssistedInjectDagger2Processor())
        .failsToCompile()
        .withErrorContaining("Multiple @AssistedModule-annotated modules found.")
        .`in`(moduleOne).onLine(9)
        .and()
        .withErrorContaining("Multiple @AssistedModule-annotated modules found.")
        .`in`(moduleTwo).onLine(9)
  }

  @Ignore("No easy way to test this")
  @Test fun multipleModulesAcrossRoundsFails() {
  }
}
