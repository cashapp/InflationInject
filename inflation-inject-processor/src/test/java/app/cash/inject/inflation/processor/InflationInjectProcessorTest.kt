package app.cash.inject.inflation.processor

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
  value = "app.cash.inject.inflation.processor.InflationInjectProcessor",
  comments = "https://github.com/cashapp/InflationInject"
)
"""

class InflationInjectProcessorTest {
  @Test fun simple() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(@Inflated Context context, @Inflated AttributeSet attrs, Long foo) {
          super(context, attrs);
        }
      }
    """)
    val inputModule = JavaFileObjects.forSourceString("test.TestModule", """
      package test;

      import app.cash.inject.inflation.InflationModule;
      import dagger.Module;

      @InflationModule
      @Module(includes = InflationInject_TestModule.class)
      abstract class TestModule {}
    """)

    val expectedFactory = JavaFileObjects.forSourceString("test.TestView_InflationFactory", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import app.cash.inject.inflation.ViewFactory;
      import java.lang.Long;
      import java.lang.Override;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class TestView_InflationFactory implements ViewFactory {
        private final Provider<Long> foo;

        @Inject public Test_InflationFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public View create(Context context, AttributeSet attrs) {
          return new TestView(context, attrs, foo.get());
        }
      }
    """)
    val expectedModule = JavaFileObjects.forSourceString("test.InflationModule_TestModule", """
      package test;

      import app.cash.inject.inflation.ViewFactory;
      import dagger.Binds;
      import dagger.Module;
      import dagger.multibindings.IntoMap;
      import dagger.multibindings.StringKey;
      import $GENERATED_TYPE;

      @Module
      $GENERATED_ANNOTATION
      abstract class InflationInject_TestModule {
        private InflationInject_TestModule() {}

        @Binds
        @IntoMap
        @StringKey("test.TestView")
        abstract ViewFactory bind_test_TestView(TestView_InflationFactory factory);
      }
    """)

    assertAbout(javaSources())
        .that(listOf(inputView, inputModule))
        .processedWith(InflationInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedFactory, expectedModule)
  }

  @Test fun injectDaggerAssistedFactoryDoesNotUseProvider() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import dagger.assisted.AssistedFactory;
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(@Inflated Context context, @Inflated AttributeSet attrs, Other.Factory foo) {
          super(context, attrs);
        }
      }

      class Other {
        Other(String a, String b) {}

        @AssistedFactory
        interface Factory {
          Other create(String b);
        }
      }
    """)
    val inputModule = JavaFileObjects.forSourceString("test.TestModule", """
      package test;

      import app.cash.inject.inflation.InflationModule;
      import dagger.Module;

      @InflationModule
      @Module(includes = InflationInject_TestModule.class)
      abstract class TestModule {}
    """)

    val expectedFactory = JavaFileObjects.forSourceString("test.TestView_InflationFactory", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import app.cash.inject.inflation.ViewFactory;
      import java.lang.Override;
      import $GENERATED_TYPE;
      import javax.inject.Inject;

      $GENERATED_ANNOTATION
      public final class TestView_InflationFactory implements ViewFactory {
        private final Other.Factory foo;

        @Inject public TestView_InflationFactory(Other.Factory foo) {
          this.foo = foo;
        }

        @Override public View create(Context context, AttributeSet attrs) {
          return new TestView(context, attrs, foo);
        }
      }
    """)
    val expectedModule = JavaFileObjects.forSourceString("test.InflationModule_TestModule", """
      package test;

      import app.cash.inject.inflation.ViewFactory;
      import dagger.Binds;
      import dagger.Module;
      import dagger.multibindings.IntoMap;
      import dagger.multibindings.StringKey;
      import $GENERATED_TYPE;

      @Module
      $GENERATED_ANNOTATION
      abstract class InflationInject_TestModule {
        private InflationInject_TestModule() {}

        @Binds
        @IntoMap
        @StringKey("test.TestView")
        abstract ViewFactory bind_test_TestView(TestView_InflationFactory factory);
      }
    """)

    assertAbout(javaSources())
        .that(listOf(inputView, inputModule))
        .processedWith(InflationInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedFactory, expectedModule)
  }

  @Test fun public() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(@Inflated Context context, @Inflated AttributeSet attrs, Long foo) {
          super(context, attrs);
        }
      }
    """)
    val inputModule = JavaFileObjects.forSourceString("test.TestModule", """
      package test;

      import app.cash.inject.inflation.InflationModule;
      import dagger.Module;

      @InflationModule
      @Module(includes = InflationInject_TestModule.class)
      public abstract class TestModule {}
    """)

    val expectedModule = JavaFileObjects.forSourceString("test.InflationModule_TestModule", """
      package test;

      import app.cash.inject.inflation.ViewFactory;
      import dagger.Binds;
      import dagger.Module;
      import dagger.multibindings.IntoMap;
      import dagger.multibindings.StringKey;
      import $GENERATED_TYPE;

      @Module
      $GENERATED_ANNOTATION
      public abstract class InflationInject_TestModule {
        private InflationInject_TestModule() {}

        @Binds
        @IntoMap
        @StringKey("test.TestView")
        abstract ViewFactory bind_test_TestView(TestView_InflationFactory factory);
      }
    """)

    assertAbout(javaSources())
        .that(listOf(inputView, inputModule))
        .processedWith(InflationInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedModule)
  }

  @Test fun nested() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class Outer {
        static class TestView extends View {
          @InflationInject
          TestView(@Inflated Context context, @Inflated AttributeSet attrs, Long foo) {
            super(context, attrs);
          }
        }
      }
    """)
    val inputModule = JavaFileObjects.forSourceString("test.TestModule", """
      package test;

      import app.cash.inject.inflation.InflationModule;
      import dagger.Module;

      @InflationModule
      @Module(includes = InflationInject_TestModule.class)
      abstract class TestModule {}
    """)

    val expectedFactory = JavaFileObjects.forSourceString("test.TestView_InflationFactory", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import app.cash.inject.inflation.ViewFactory;
      import java.lang.Long;
      import java.lang.Override;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class Outer${'$'}TestView_InflationFactory implements ViewFactory {
        private final Provider<Long> foo;

        @Inject public Test_InflationFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public View create(Context context, AttributeSet attrs) {
          return new Outer.TestView(context, attrs, foo.get());
        }
      }
    """)
    val expectedModule = JavaFileObjects.forSourceString("test.InflationModule_TestModule", """
      package test;

      import app.cash.inject.inflation.ViewFactory;
      import dagger.Binds;
      import dagger.Module;
      import dagger.multibindings.IntoMap;
      import dagger.multibindings.StringKey;
      import $GENERATED_TYPE;

      @Module
      $GENERATED_ANNOTATION
      abstract class InflationInject_TestModule {
        private InflationInject_TestModule() {}

        @Binds
        @IntoMap
        @StringKey("test.Outer${'$'}TestView")
        abstract ViewFactory bind_test_Outer${'$'}TestView(Outer${'$'}TestView_InflationFactory factory);
      }
    """)

    assertAbout(javaSources())
        .that(listOf(inputView, inputModule))
        .processedWith(InflationInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedFactory, expectedModule)
  }

  @Ignore("Not handled properly. https://github.com/cashapp/InflationInject/issues/64")
  @Test fun parameterized() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class TestView<T> extends View {
        @InflationInject
        TestView(@Inflated Context context, @Inflated AttributeSet attrs, Long foo) {
          super(context, attrs);
        }
      }
    """)
    val inputModule = JavaFileObjects.forSourceString("test.TestModule", """
      package test;

      import app.cash.inject.inflation.InflationModule;
      import dagger.Module;

      @InflationModule
      @Module(includes = InflationInject_TestModule.class)
      abstract class TestModule {}
    """)

    val expectedFactory = JavaFileObjects.forSourceString("test.TestView_InflationFactory", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import app.cash.inject.inflation.ViewFactory;
      import java.lang.Long;
      import java.lang.Override;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class TestView_InflationFactory implements ViewFactory {
        private final Provider<Long> foo;

        @Inject public Test_InflationFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public View create(Context context, AttributeSet attrs) {
          return new TestView<?>(context, attrs, foo.get());
        }
      }
    """)
    val expectedModule = JavaFileObjects.forSourceString("test.InflationModule_TestModule", """
      package test;

      import app.cash.inject.inflation.ViewFactory;
      import dagger.Binds;
      import dagger.Module;
      import dagger.multibindings.IntoMap;
      import dagger.multibindings.StringKey;
      import $GENERATED_TYPE;

      @Module
      $GENERATED_ANNOTATION
      abstract class InflationInject_TestModule {
        private InflationInject_TestModule() {}

        @Binds
        @IntoMap
        @StringKey("test.TestView")
        abstract ViewFactory bind_test_TestView(TestView_InflationFactory factory);
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
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(Long foo, @Inflated Context context, @Inflated AttributeSet attrs) {
          super(context, attrs);
        }
      }
    """)

    val expectedFactory = JavaFileObjects.forSourceString("test.TestView_InflationFactory", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import app.cash.inject.inflation.ViewFactory;
      import java.lang.Long;
      import java.lang.Override;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class TestView_InflationFactory implements ViewFactory {
        private final Provider<Long> foo;

        @Inject public Test_InflationFactory(Provider<Long> foo) {
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
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(@Inflated AttributeSet attrs, @Inflated Context context, Long foo) {
          super(context, attrs);
        }
      }
    """)

    val expectedFactory = JavaFileObjects.forSourceString("test.TestView_InflationFactory", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import app.cash.inject.inflation.ViewFactory;
      import java.lang.Long;
      import java.lang.Override;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class TestView_InflationFactory implements ViewFactory {
        private final Provider<Long> foo;

        @Inject public Test_InflationFactory(Provider<Long> foo) {
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

  @Test fun typeDoesNotExtendView() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class TestView {
        @InflationInject
        TestView(@Inflated AttributeSet attrs, @Inflated Context context, Long foo) {
          super(context, attrs);
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining("@InflationInject-using types must be subtypes of View")
        .`in`(inputView).onLine(9)
  }

  @Test fun typeExtendsViewSubclass() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.widget.LinearLayout;
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class TestView extends LinearLayout {
        @InflationInject
        TestView(@Inflated Context context, @Inflated AttributeSet attrs, Long foo) {
          super(context, attrs);
        }
      }
    """)

    val expectedFactory = JavaFileObjects.forSourceString("test.TestView_InflationFactory", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import app.cash.inject.inflation.ViewFactory;
      import java.lang.Long;
      import java.lang.Override;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class TestView_InflationFactory implements ViewFactory {
        private final Provider<Long> foo;

        @Inject public Test_InflationFactory(Provider<Long> foo) {
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
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class LongView extends LinearLayout {
        @InflationInject
        LongView(@Inflated Context context, @Inflated AttributeSet attrs, Long foo) {
          super(context, attrs);
        }
      }
    """)
    val stringView = JavaFileObjects.forSourceString("test.StringView", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.widget.LinearLayout;
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class StringView extends LongView {
        @InflationInject
        StringView(@Inflated Context context, @Inflated AttributeSet attrs, String foo) {
          super(context, attrs, Long.parseLong(foo));
        }
      }
    """)

    val expectedLongFactory = JavaFileObjects.forSourceString("test.LongView_InflationFactory", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import app.cash.inject.inflation.ViewFactory;
      import java.lang.Long;
      import java.lang.Override;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class LongView_InflationFactory implements ViewFactory {
        private final Provider<Long> foo;

        @Inject public LongView_InflationFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public View create(Context context, AttributeSet attrs) {
          return new LongView(context, attrs, foo.get());
        }
      }
    """)
    val expectedStringFactory = JavaFileObjects.forSourceString("test.StringView_InflationFactory", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import app.cash.inject.inflation.ViewFactory;
      import java.lang.Override;
      import java.lang.String;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class StringView_InflationFactory implements ViewFactory {
        private final Provider<String> foo;

        @Inject public LongView_InflationFactory(Provider<String> foo) {
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
      import app.cash.inject.inflation.InflationInject;

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
          Inflation injection requires Context and AttributeSet @Inflated parameters.
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
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(@Inflated Context context, @Inflated AttributeSet attrs, @Inflated String hey, Long foo) {
          super(context, attrs);
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining("""
          Inflation injection requires Context and AttributeSet @Inflated parameters.
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
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(@Inflated AttributeSet attrs, Long foo) {
          super(null, attrs);
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining("""
          Inflation injection requires Context and AttributeSet @Inflated parameters.
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
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(@Inflated Context context, Long foo) {
          super(context, null);
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining("""
          Inflation injection requires Context and AttributeSet @Inflated parameters.
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
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(@Inflated Context context, @Inflated AttributeSet attrs) {
          super(context, attrs);
        }
      }
    """)

    assertAbout(javaSource())
        .that(inputView)
        .processedWith(InflationInjectProcessor())
        .compilesWithoutError()
        .withWarningContaining("Inflation injection requires at least one non-@Inflated parameter.")
        .`in`(inputView).onLine(12)
        // .and().generatesNoFiles()
  }

  @Test fun privateConstructorFails() {
    val inputView = JavaFileObjects.forSourceString("test.TestView", """
      package test;

      import android.view.View;
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        private TestView(@Inflated Context context, @Inflated AttributeSet attrs, Long foo) {
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
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class Outer {
        private static class TestView extends View {
          @InflationInject
          TestView(@Inflated Context context, @Inflated AttributeSet attrs, Long foo) {
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
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class Outer {
        class TestView extends View {
          @InflationInject
          TestView(@Inflated Context context, @Inflated AttributeSet attrs, Long foo) {
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
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class TestView extends View {
        @InflationInject
        TestView(@Inflated Context context, @Inflated AttributeSet attrs, Long foo) {
          super(context, attrs);
        }

        @InflationInject
        TestView(@Inflated Context context, @Inflated AttributeSet attrs, String foo) {
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

      import app.cash.inject.inflation.InflationModule;

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

  @Test fun moduleWithNoIncludesFails() {
    val moduleOne = JavaFileObjects.forSourceString("test.OneModule", """
      package test;

      import app.cash.inject.inflation.InflationModule;
      import dagger.Module;

      @InflationModule
      @Module
      abstract class OneModule {}
    """)

    assertAbout(javaSource())
        .that(moduleOne)
        .processedWith(InflationInjectProcessor())
        .failsToCompile()
        .withErrorContaining("@InflationModule's @Module must include InflationInject_OneModule")
        .`in`(moduleOne).onLine(9)
  }

  @Test fun moduleWithoutIncludeFails() {
    val moduleOne = JavaFileObjects.forSourceString("test.OneModule", """
      package test;

      import app.cash.inject.inflation.InflationModule;
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
        .withErrorContaining("@InflationModule's @Module must include InflationInject_OneModule")
        .`in`(moduleOne).onLine(9)
  }

  @Test fun multipleModulesFails() {
    val moduleOne = JavaFileObjects.forSourceString("test.OneModule", """
      package test;

      import app.cash.inject.inflation.InflationModule;
      import dagger.Module;

      @InflationModule
      @Module(includes = InflationInject_OneModule.class)
      abstract class OneModule {}
    """)
    val moduleTwo = JavaFileObjects.forSourceString("test.TwoModule", """
      package test;

      import app.cash.inject.inflation.InflationModule;
      import dagger.Module;

      @InflationModule
      @Module(includes = InflationInject_TwoModule.class)
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

  @Ignore("No easy way to test this")
  @Test fun multipleModulesAcrossRoundsFails() {
  }

  @Test fun multipleViewsStableOrder() {
    val inputViewA = JavaFileObjects.forSourceString("test.TestViewA", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class TestViewA extends View {
        @InflationInject
        TestViewA(@Inflated Context context, @Inflated AttributeSet attrs, Long foo) {
          super(context, attrs);
        }
      }
    """)
    val inputViewB = JavaFileObjects.forSourceString("test.TestViewA", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import app.cash.inject.inflation.Inflated;
      import app.cash.inject.inflation.InflationInject;

      class TestViewB extends View {
        @InflationInject
        TestViewB(@Inflated Context context, @Inflated AttributeSet attrs, Long foo) {
          super(context, attrs);
        }
      }
    """)
    val inputModule = JavaFileObjects.forSourceString("test.TestModule", """
      package test;

      import app.cash.inject.inflation.InflationModule;
      import dagger.Module;

      @InflationModule
      @Module(includes = InflationInject_TestModule.class)
      abstract class TestModule {}
    """)

    val expectedFactoryA = JavaFileObjects.forSourceString("test.TestViewA_InflationFactory", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import app.cash.inject.inflation.ViewFactory;
      import java.lang.Long;
      import java.lang.Override;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class TestViewA_InflationFactory implements ViewFactory {
        private final Provider<Long> foo;

        @Inject public TestViewA_InflationFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public View create(Context context, AttributeSet attrs) {
          return new TestViewA(context, attrs, foo.get());
        }
      }
    """)
    val expectedFactoryB = JavaFileObjects.forSourceString("test.TestViewB_InflationFactory", """
      package test;

      import android.content.Context;
      import android.util.AttributeSet;
      import android.view.View;
      import app.cash.inject.inflation.ViewFactory;
      import java.lang.Long;
      import java.lang.Override;
      import $GENERATED_TYPE;
      import javax.inject.Inject;
      import javax.inject.Provider;

      $GENERATED_ANNOTATION
      public final class TestViewB_InflationFactory implements ViewFactory {
        private final Provider<Long> foo;

        @Inject public TestViewB_InflationFactory(Provider<Long> foo) {
          this.foo = foo;
        }

        @Override public View create(Context context, AttributeSet attrs) {
          return new TestViewB(context, attrs, foo.get());
        }
      }
    """)
    val expectedModule = JavaFileObjects.forSourceString("test.InflationModule_TestModule", """
      package test;

      import app.cash.inject.inflation.ViewFactory;
      import dagger.Binds;
      import dagger.Module;
      import dagger.multibindings.IntoMap;
      import dagger.multibindings.StringKey;
      import $GENERATED_TYPE;

      @Module
      $GENERATED_ANNOTATION
      abstract class InflationInject_TestModule {
        private InflationInject_TestModule() {}

        @Binds
        @IntoMap
        @StringKey("test.TestViewA")
        abstract ViewFactory bind_test_TestViewA(TestViewA_InflationFactory factory);

        @Binds
        @IntoMap
        @StringKey("test.TestViewB")
        abstract ViewFactory bind_test_TestViewB(TestViewB_InflationFactory factory);
      }
    """)

    assertAbout(javaSources())
      .that(listOf(inputViewA, inputViewB, inputModule))
      .processedWith(InflationInjectProcessor())
      .compilesWithoutError()
      .and()
      .generatesSources(expectedFactoryA, expectedFactoryB, expectedModule)

    assertAbout(javaSources())
      .that(listOf(inputViewB, inputViewA, inputModule)) // Inputs passed in reverse order.
      .processedWith(InflationInjectProcessor())
      .compilesWithoutError()
      .and()
      .generatesSources(expectedFactoryA, expectedFactoryB, expectedModule)
  }

  // TODO module and no inflation injects (what do we do here? bind empty map? fail?)
}
