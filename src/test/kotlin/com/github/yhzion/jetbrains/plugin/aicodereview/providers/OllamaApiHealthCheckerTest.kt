package com.github.yhzion.jetbrains.plugin.aicodereview.providers

import com.github.yhzion.jetbrains.plugin.aicodereview.utils.Response
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val url = "http://localhost:11434"

class OllamaApiHealthCheckerTest {

    @Test
    fun testIsHealthyReturnsTrueForSuccessfulResponse() {
        val response = Response(statusCode = 200, body = "Ollama is running")
        val healthChecker = OllamaApiHealthChecker(url)
        val result = healthChecker.isHealthy(response)
        assertTrue(result)
    }

    @Test
    fun testIsHealthyReturnsFalseForUnsuccessfulResponseCode() {
        val response = Response(statusCode = 500, body = "Ollama is running")
        val healthChecker = OllamaApiHealthChecker(url)
        val result = healthChecker.isHealthy(response)
        assertFalse(result)
    }

    @Test
    fun testIsHealthyReturnsFalseForIncorrectBody() {
        val response = Response(statusCode = 200, body = "Ollama is down")
        val healthChecker = OllamaApiHealthChecker(url)
        val result = healthChecker.isHealthy(response)
        assertFalse(result)
    }
}