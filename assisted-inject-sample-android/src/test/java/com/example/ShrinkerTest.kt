package com.example

import com.google.common.truth.Truth.assertThat
import com.jakewharton.dex.DexParser
import org.junit.Test
import java.io.File

class ShrinkerTest {
  @Test fun customViewFactoryVerticallyInlined() {
    val apkPath = File(System.getProperty("apkPath")!!)
    val parser = DexParser.fromFile(apkPath)
    val actual = parser.list().filter { it.declaringType == "com.example.Greeter\$Factory" }
    assertThat(actual).apply {
      if (BuildConfig.DEBUG) {
        isNotEmpty()
      } else {
        isEmpty()
      }
    }
  }
}
