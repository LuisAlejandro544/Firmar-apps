package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.KeystoreGenerator
import com.example.crypto.KeystoreParams
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Keystore Creator", appName)
  }

  @Test
  fun `generate keystore succeeds and creates file with fingerprints`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val params = KeystoreParams(
      title = "Test Key",
      fileName = "test_key.jks",
      alias = "testalias",
      storePassword = "password123",
      keyPassword = "password123",
      keySize = 2048,
      validityYears = 25
    )
    val result = KeystoreGenerator.generateKeystore(context, params)
    assertTrue("La generación de keystore falló: ${result.exceptionOrNull()?.message}", result.isSuccess)
    val entity = result.getOrThrow()
    assertEquals("test_key.jks", entity.fileName)
    assertEquals("testalias", entity.alias)
    assertTrue(File(entity.filePath).exists())
    assertTrue(entity.sha256Fingerprint.isNotEmpty())
  }
}
