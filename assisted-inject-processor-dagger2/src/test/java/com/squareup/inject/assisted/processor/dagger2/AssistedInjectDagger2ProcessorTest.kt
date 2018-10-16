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
package com.squareup.inject.assisted.processor.dagger2

import com.google.common.truth.Truth.assertAbout
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory.javaSource
import com.google.testing.compile.JavaSourcesSubjectFactory.javaSources
import com.squareup.inject.assisted.dagger2.processor.AssistedInjectDagger2Processor
import org.junit.Ignore
import org.junit.Test

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

      @Module
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

      @Module
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

      @Module
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
