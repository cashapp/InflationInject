package com.squareup.inject.inflation.processor

import com.google.common.truth.Truth.assertAbout
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory.javaSource
import com.google.testing.compile.JavaSourcesSubjectFactory.javaSources
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

        @Override public TestView create(Context context, AttributeSet attrs) {
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
        abstract ViewFactory<?> bind_test_TestView(TestView_AssistedFactory factory);
      }
    """)

    assertAbout(javaSources())
        .that(listOf(inputView, inputModule))
        .processedWith(InflationInjectProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedFactory, expectedModule)
  }

  // TODO context and attrs last params
  // TODO context and attrs first but swapped
  // TODO type does not extend view fails
  // TODO constructor missing assisted arguments fails
  // TODO constructor missing provided arguments fails
  // TODO constructor assisted arguments are not Context and AttributeSet fails
  // TODO constructor private fails
  // TODO type nested and private fails
  // TODO type nested and not static fails
  // TODO multiple modules fails
  // TODO module and no inflation injects (what do we do here? bind empty map? fail?)
}
