package app.cash.inject.inflation

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(sdk = [26])
@RunWith(RobolectricTestRunner::class)
class InflationInjectFactory2Test {
  private val context = RuntimeEnvironment.systemContext

  @Test fun viewFactoryInMap() {
    val expected = View(context)
    val factories = mapOf("com.example.View" to ViewFactory { _, _ -> expected })
    val inflater = InflationInjectFactory(factories, ThrowingFactory2)
    val actual = inflater.onCreateView(null, "com.example.View", context, null)
    assertSame(expected, actual)
  }

  @Test fun viewFactoryMissingWithDelegateDelegates() {
    val expected = View(context)
    val factories = emptyMap<String, ViewFactory>()
    val delegate = object : LayoutInflater.Factory2 {
      override fun onCreateView(parent: View?, name: String, context: Context,
          attrs: AttributeSet?) = expected

      override fun onCreateView(name: String, context: Context, attrs: AttributeSet?): View {
        throw AssertionError()
      }

    }
    val inflater = InflationInjectFactory(factories, delegate)
    val actual = inflater.onCreateView(null, "com.example.View", context, null)
    assertSame(expected, actual)
  }

  @Test fun viewFactoryMissingWithoutDelegateReturnsNull() {
    val factories = emptyMap<String, ViewFactory>()
    val inflater = InflationInjectFactory(factories, null)
    val actual = inflater.onCreateView(null, "com.example.View", context, null)
    assertNull(actual)
  }
}

private object ThrowingFactory2 : LayoutInflater.Factory2 {
  override fun onCreateView(parent: View?, name: String, context: Context,
      attrs: AttributeSet?): View? {
    throw AssertionError()
  }

  override fun onCreateView(name: String, context: Context, attrs: AttributeSet?): View? {
    throw AssertionError()
  }
}
