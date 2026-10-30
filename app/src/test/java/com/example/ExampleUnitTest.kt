package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun geminiProvider_supportsMultiVersionModels() {
    val models = com.example.data.provider.GeminiProvider.KNOWN_MODELS
    assertTrue(models.contains("gemini-2.5-flash"))
    assertTrue(models.contains("gemini-2.0-flash"))
    assertTrue(models.contains("gemini-1.5-flash"))
    assertTrue(models.contains("gemini-1.5-pro"))
    assertTrue(models.contains("gemini-2.0-flash-lite"))
  }

  @Test
  fun geminiProvider_normalizesOutdatedModelNames() {
    assertEquals("gemini-2.0-flash", com.example.data.provider.GeminiProvider.normalizeModel("gemini-1.5-flash"))
    assertEquals("gemini-2.0-flash", com.example.data.provider.GeminiProvider.normalizeModel("models/gemini-1.5-flash"))
    assertEquals("gemini-2.0-flash", com.example.data.provider.GeminiProvider.normalizeModel(""))
    assertEquals("gemini-2.5-flash", com.example.data.provider.GeminiProvider.normalizeModel("gemini-2.5-flash"))
  }

  @Test
  fun geminiProvider_determinesCorrectEndpointVersions() {
    val endpoints20 = com.example.data.provider.GeminiProvider.getGenerateEndpoints("gemini-2.0-flash", "testKey")
    assertTrue(endpoints20.first().contains("/v1beta/models/gemini-2.0-flash"))

    val endpoints15 = com.example.data.provider.GeminiProvider.getGenerateEndpoints("gemini-1.5-flash", "testKey")
    assertTrue(endpoints15.first().contains("/v1/models/gemini-1.5-flash"))
  }
}
