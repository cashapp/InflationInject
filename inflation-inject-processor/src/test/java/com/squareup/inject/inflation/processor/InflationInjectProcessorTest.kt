package com.squareup.inject.inflation.processor

import com.google.common.truth.Truth.assertAbout
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory.javaSource
import com.google.testing.compile.JavaSourcesSubjectFactory.javaSources
import org.junit.Ignore
import org.junit.Test

class InflationInjectProcessorTest {
  @Test fun simple() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(@Assisted Context context, @Assisted AttributeSet attrs, Long foo) {
          super(context, attrs);
        }
      }
    """)
    val inputModule = JavaFileObjects.forSourceString("test.TestModule", """
      package test;

      import com.squareup.inject.inflation.InflationModule;
      import dagger.Module;

      @InflationModule
      @Module(includes = InflationInject_TestModule.class)
      abstract class TestModule {}
    """)

    val expectedFactory = JavaFileObjects.forSourceString("test.TestView_AssistedFactory", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import com.squareup.inject.inflation.ViewFactory;
      import java.lang.Long;
      import java.lang.Override;
      import javax.inject.Inject;
      import javax.inject.Provider;

      public final class TestView_AssistedFactory implements ViewFactory {
        private final Provider<Long> foo;

        @Inject public Test_AssistedFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public View create(Context context, AttributeSet attrs) {
          return new TestView(context, attrs, foo.get());
        }
      }
    """)
    val expectedModule = JavaFileObjects.forSourceString("test.InflationModule_TestModule", """
      package test;

      import com.squareup.inject.inflation.ViewFactory;
      import dagger.Binds;
      import dagger.Module;
      import dagger.multibindings.IntoMap;
      import dagger.multibindings.StringKey;

      @Module
      public abstract class InflationInject_TestModule {
        private InflationInject_TestModule() {}

        @Binds
        @IntoMap
        @StringKey("test.TestView")
        abstract ViewFactory bind_test_TestView(TestView_AssistedFactory factory);
      }
    """)

    assertAbout(javaSources())
        .that(listOf(inputView, inputModule))
        .processedWith(InflationInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedFactory, expectedModule)
  }

  @Test fun assistedParametersLast() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(Long foo, @Assisted Context context, @Assisted AttributeSet attrs) {
          super(context, attrs);
        }
      }
    """)

    val expectedFactory = JavaFileObjects.forSourceString("test.TestView_AssistedFactory", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import com.squareup.inject.inflation.ViewFactory;
      import java.lang.Long;
      import java.lang.Override;
      import javax.inject.Inject;
      import javax.inject.Provider;

      public final class TestView_AssistedFactory implements ViewFactory {
        private final Provider<Long> foo;

        @Inject public Test_AssistedFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public View create(Context context, AttributeSet attrs) {
          return new TestView(foo.get(), context, attrs);
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedFactory)
  }

  @Test fun contextAndAttributeSetSwapped() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(@Assisted AttributeSet attrs, @Assisted Context context, Long foo) {
          super(context, attrs);
        }
      }
    """)

    val expectedFactory = JavaFileObjects.forSourceString("test.TestView_AssistedFactory", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import com.squareup.inject.inflation.ViewFactory;
      import java.lang.Long;
      import java.lang.Override;
      import javax.inject.Inject;
      import javax.inject.Provider;

      public final class TestView_AssistedFactory implements ViewFactory {
        private final Provider<Long> foo;

        @Inject public Test_AssistedFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public View create(Context context, AttributeSet attrs) {
          return new TestView(attrs, context, foo.get());
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedFactory)
  }

  @Ignore("Not implemented")
  @Test fun typeDoesNotExtendView() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(@Assisted AttributeSet attrs, @Assisted Context context, Long foo) {
          super(context, attrs);
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining("Something about not extending View")
        .`in`(inputView).onLine(10)
  }

  @Test fun typeExtendsViewSubclass() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.widget.LinearLayout;
      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.inflation.InflationInject;

      class TestView extends LinearLayout {
        @InflationInject
        TestView(@Assisted Context context, @Assisted AttributeSet attrs, Long foo) {
          super(context, attrs);
        }
      }
    """)

    val expectedFactory = JavaFileObjects.forSourceString("test.TestView_AssistedFactory", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import com.squareup.inject.inflation.ViewFactory;
      import java.lang.Long;
      import java.lang.Override;
      import javax.inject.Inject;
      import javax.inject.Provider;

      public final class TestView_AssistedFactory implements ViewFactory {
        private final Provider<Long> foo;

        @Inject public Test_AssistedFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public View create(Context context, AttributeSet attrs) {
          return new TestView(context, attrs, foo.get());
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedFactory)
  }

  @Test fun baseAndSubtypeInjection() {
    val longView = JavaFileObjects.forSourceString("test.LongView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.widget.LinearLayout;
      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.inflation.InflationInject;

      class LongView extends LinearLayout {
        @InflationInject
        LongView(@Assisted Context context, @Assisted AttributeSet attrs, Long foo) {
          super(context, attrs);
        }
      }
    """)
    val stringView = JavaFileObjects.forSourceString("test.StringView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.widget.LinearLayout;
      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.inflation.InflationInject;

      class StringView extends LongView {
        @InflationInject
        StringView(@Assisted Context context, @Assisted AttributeSet attrs, String foo) {
          super(context, attrs, Long.parseLong(foo));
        }
      }
    """)

    val expectedLongFactory = JavaFileObjects.forSourceString("test.LongView_AssistedFactory", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import com.squareup.inject.inflation.ViewFactory;
      import java.lang.Long;
      import java.lang.Override;
      import javax.inject.Inject;
      import javax.inject.Provider;

      public final class LongView_AssistedFactory implements ViewFactory {
        private final Provider<Long> foo;

        @Inject public LongView_AssistedFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public View create(Context context, AttributeSet attrs) {
          return new LongView(context, attrs, foo.get());
        }
      }
    """)
    val expectedStringFactory = JavaFileObjects.forSourceString("test.StringView_AssistedFactory", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import com.squareup.inject.inflation.ViewFactory;
      import java.lang.Override;
      import java.lang.String;
      import javax.inject.Inject;
      import javax.inject.Provider;

      public final class StringView_AssistedFactory implements ViewFactory {
        private final Provider<String> foo;

        @Inject public LongView_AssistedFactory(Provider<String> foo) {
          this.foo = foo;
        }

        @Override public View create(Context context, AttributeSet attrs) {
          return new StringView(context, attrs, foo.get());
        }
      }
    """)

    assertAbout(javaSources())
        .that(listOf(longView, stringView))
        .processedWith(InflationInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedLongFactory, expectedStringFactory)
  }


  @Test fun constructorMissingAssistedParametersFails() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.view.View;
      import com.squareup.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(Long foo) {
          super(null);
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining("""
          Inflation injection requires Context and AttributeSet @Assisted parameters.
              Found:
                []
              Expected:
                [android.content.Context, android.util.AttributeSet]
          """.trimIndent())
        .`in`(inputView).onLine(9)
  }

  @Test fun constructorExtraAssistedParameterFails() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(@Assisted Context context, @Assisted AttributeSet attrs, @Assisted String hey, Long foo) {
          super(context, attrs);
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining("""
          Inflation injection requires Context and AttributeSet @Assisted parameters.
              Found:
                [android.content.Context, android.util.AttributeSet, java.lang.String]
              Expected:
                [android.content.Context, android.util.AttributeSet]
          """.trimIndent())
        .`in`(inputView).onLine(12)
  }

  @Test fun constructorMissingContextFails() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.util.AttributeSet;
      import android.view.View;
      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(@Assisted AttributeSet attrs, Long foo) {
          super(null, attrs);
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining("""
          Inflation injection requires Context and AttributeSet @Assisted parameters.
              Found:
                [android.util.AttributeSet]
              Expected:
                [android.content.Context, android.util.AttributeSet]
          """.trimIndent())
        .`in`(inputView).onLine(11)
  }

  @Test fun constructorMissingAttributeSetFails() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.content.Context;
      import android.view.View;
      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(@Assisted Context context, Long foo) {
          super(context, null);
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining("""
          Inflation injection requires Context and AttributeSet @Assisted parameters.
              Found:
                [android.content.Context]
              Expected:
                [android.content.Context, android.util.AttributeSet]
          """.trimIndent())
        .`in`(inputView).onLine(11)
  }

  @Test fun constructorMissingProvidedParametersWarns() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(@Assisted Context context, @Assisted AttributeSet attrs) {
          super(context, attrs);
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .compilesWithoutError()
        .withWarningContaining("Inflation injection requires at least one non-@Assisted parameter.")
        .`in`(inputView).onLine(12)
        // .and().generatesNoFiles()
  }

  @Test fun privateConstructorFails() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.view.View;
      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        private TestView(@Assisted Context context, @Assisted AttributeSet attrs, Long foo) {
          super(context, attrs);
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining("@InflationInject constructor must not be private.")
        .`in`(inputView).onLine(10)
  }

  @Test fun nestedPrivateTypeFails() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.view.View;
      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.inflation.InflationInject;

      class Outer {
        private static class TestView extends View {
          @InflationInject
          TestView(@Assisted Context context, @Assisted AttributeSet attrs, Long foo) {
            super(context, attrs);
          }
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining("@InflationInject-using types must not be private")
        .`in`(inputView).onLine(9)
  }

  @Test fun nestedNonStaticFails() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.view.View;
      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.inflation.InflationInject;

      class Outer {
        class TestView extends View {
          @InflationInject
          TestView(@Assisted Context context, @Assisted AttributeSet attrs, Long foo) {
            super(context, attrs);
          }
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining("Nested @InflationInject-using types must be static")
        .`in`(inputView).onLine(9)
  }

  @Test fun multipleInflationInjectConstructorsFails() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.view.View;
      import com.squareup.inject.assisted.Assisted;
      import com.squareup.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(@Assisted Context context, @Assisted AttributeSet attrs, Long foo) {
          super(context, attrs);
        }

        @InflationInject
        TestView(@Assisted Context context, @Assisted AttributeSet attrs, String foo) {
          super(context, attrs);
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining("Multiple @InflationInject-annotated constructors found.")
        .`in`(inputView).onLine(8)
  }

  @Test fun moduleWithoutModuleAnnotationFails() {
    val moduleOne = JavaFileObjects.forSourceString("test.OneModule", """
      package test;

      import com.squareup.inject.inflation.InflationModule;

      @InflationModule
      abstract class OneModule {}
    """)

    assertAbout(javaSource())
        .that(moduleOne)
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining("@InflationModule must also be annotated as a Dagger @Module")
        .`in`(moduleOne).onLine(7)
  }

  @Ignore("Not yet validated.")
  @Test fun moduleWithNoIncludesFails() {
    val moduleOne = JavaFileObjects.forSourceString("test.OneModule", """
      package test;

      import com.squareup.inject.inflation.InflationModule;
      import dagger.Module;

      @InflationModule
      @Module
      abstract class OneModule {}
    """)

    assertAbout(javaSource())
        .that(moduleOne)
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining(
            "@InflationModule must @Module(include = InflationInject_OneModule.class).")
        .`in`(moduleOne).onLine(9)
  }

  @Ignore("Not yet validated.")
  @Test fun moduleWithoutIncludeFails() {
    val moduleOne = JavaFileObjects.forSourceString("test.OneModule", """
      package test;

      import com.squareup.inject.inflation.InflationModule;
      import dagger.Module;

      @InflationModule
      @Module(includes = TwoModule.class)
      abstract class OneModule {}

      @Module
      abstract class TwoModule {}
    """)

    assertAbout(javaSource())
        .that(moduleOne)
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining(
            "@InflationModule must @Module(include = InflationInject_OneModule.class).")
        .`in`(moduleOne).onLine(9)
  }

  @Test fun multipleModulesFails() {
    val moduleOne = JavaFileObjects.forSourceString("test.OneModule", """
      package test;

      import com.squareup.inject.inflation.InflationModule;
      import dagger.Module;

      @InflationModule
      @Module(includes = AssistedInject_OneModule.class)
      abstract class OneModule {}
    """)
    val moduleTwo = JavaFileObjects.forSourceString("test.TwoModule", """
      package test;

      import com.squareup.inject.inflation.InflationModule;
      import dagger.Module;

      @InflationModule
      @Module(includes = AssistedInject_TwoModule.class)
      abstract class TwoModule {}
    """)

    assertAbout(javaSources())
        .that(listOf(moduleOne, moduleTwo))
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining("Multiple @InflationModule-annotated modules found.")
        .`in`(moduleOne).onLine(9)
        .and()
        .withErrorContaining("Multiple @InflationModule-annotated modules found.")
        .`in`(moduleTwo).onLine(9)
  }

  // TODO module and no inflation injects (what do we do here? bind empty map? fail?)
}
