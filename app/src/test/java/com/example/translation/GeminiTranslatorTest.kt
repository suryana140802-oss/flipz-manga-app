package com.example.translation

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.*
import java.lang.Exception

@RunWith(RobolectricTestRunner::class)
class GeminiTranslatorTest {
    @Test
    fun testInitialization() {
        try {
            val translator = GeminiTranslator()
            println("Initialization successful")
        } catch (e: Exception) {
            e.printStackTrace()
            fail("Failed to initialize: ${e.message}")
        }
    }
}
