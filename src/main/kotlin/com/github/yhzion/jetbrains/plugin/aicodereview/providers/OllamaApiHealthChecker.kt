package com.github.yhzion.jetbrains.plugin.aicodereview.providers

import com.github.yhzion.jetbrains.plugin.aicodereview.utils.ApiHealthChecker
import com.github.yhzion.jetbrains.plugin.aicodereview.utils.Response

open class OllamaApiHealthChecker(customUrl: String) : ApiHealthChecker(customUrl = customUrl) {
    override fun isHealthy(response: Response): Boolean {
        return response.statusCode == 200 && response.body == "Ollama is running"
    }
    override fun getDefaultUrl(): String {
        return "http://localhost:11434"
    }
}