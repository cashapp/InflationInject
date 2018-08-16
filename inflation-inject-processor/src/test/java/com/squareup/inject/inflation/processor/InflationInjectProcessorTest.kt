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
          return new TestView(foo.get(), context, attrs);
        }
      }
    """)

    assertAbout(javaSources())
        .that(listOf(inputView, inputModule))
        .processedWith(InflationInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedFactory)
  }

  @Ignore("Override has parameter ordering based on constructor and not factory")
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
          return new TestView(attrs, context, foo.get());
        }
      }
    """)

    assertAbout(javaSources())
        .that(listOf(inputView, inputModule))
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

    assertAbout(javaSources())
        .that(listOf(inputView, inputModule))
        .processedWith(InflationInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedFactory)
  }

  @Ignore("Not implemented")
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
        .withErrorContaining("Something about missing required assisted params")
        .`in`(inputView).onLine(9)
  }

  @Ignore("Not implemented")
  @Test fun constructorExtraAssistedParameterFails() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
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
        .withErrorContaining("Something about missing required assisted params")
        .`in`(inputView).onLine(9)
  }

  @Ignore("Not implemented")
  @Test fun constructorMissingContextFails() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.util.AttributeSet;
      import android.view.View;
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
        .withErrorContaining("Something about missing required assisted params")
        .`in`(inputView).onLine(9)
  }

  @Ignore("Not implemented")
  @Test fun constructorMissingAttributeSetFails() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.content.Context;
      import android.view.View;
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
        .withErrorContaining("Something about missing required assisted params")
        .`in`(inputView).onLine(9)
  }

  @Ignore("Not implemented")
  @Test fun constructorMissingProvidedParametersWarns() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.view.View;
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
        .withWarningContaining("Something about @InflationInject not being needed")
        .`in`(inputView).onLine(9)
        // .and().generatesNoFiles()
  }

  @Ignore("Not implemented")
  @Test fun privateConstructorFails() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.view.View;
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
        .withErrorContaining("Something about private constructor")
        .`in`(inputView).onLine(9)
  }

  @Ignore("Not implemented")
  @Test fun nestedPrivateTypeFails() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.view.View;
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
        .withErrorContaining("Something about private type")
        .`in`(inputView).onLine(8)
  }

  @Ignore("Not implemented")
  @Test fun nestedNonStaticFails() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.view.View;
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
        .withErrorContaining("Something about nested non-static type")
        .`in`(inputView).onLine(8)
  }

  @Ignore("Not implemented")
  @Test fun multipleInflationInjectConstructorsFails() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.view.View;
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
        .withErrorContaining("Something about multiple constructors")
        .`in`(inputView).onLine(9)
  }

  // TODO multiple modules fails
  // TODO module and no inflation injects (what do we do here? bind empty map? fail?)
}
