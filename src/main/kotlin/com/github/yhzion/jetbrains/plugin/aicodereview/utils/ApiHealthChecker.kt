package com.github.yhzion.jetbrains.plugin.aicodereview.utils

import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

abstract class ApiHealthChecker(
    private val url: String,
    private val interval: Int = 30
) {
    private val callbacks: MutableList<(Boolean) -> Unit> = mutableListOf()
    private var isRunning: Boolean = false

    init {
        startHealthCheck()
    }

    protected abstract fun isHealthy(response: Response): Boolean

    private suspend fun checkApiHealth() {
        try {
            val response = sendRequest(url)
            val result = isHealthy(response)
            callbacks.forEach { it(result) }
        } catch (e: Exception) {
            callbacks.forEach { it(false) }
        }
    }

    private suspend fun sendRequest(url: String): Response {
        val urlObj = URL(url)
        val connection = withContext(Dispatchers.IO) {
            urlObj.openConnection()
        } as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }

            Response(responseCode, responseMessage)
        } finally {
            connection.disconnect()
        }
    }

    fun subscribe(callback: (Boolean) -> Unit) {
        callbacks.add(callback)
    }

    fun unsubscribe(callback: (Boolean) -> Unit) {
        callbacks.remove(callback)
    }

    private fun startHealthCheck() {
        isRunning = true
        CoroutineScope(Dispatchers.IO).launch {
            while (isRunning) {
                checkApiHealth()
                delay(interval * 1000L)
            }
        }
    }

    fun stopHealthCheck() {
        isRunning = false
    }
}