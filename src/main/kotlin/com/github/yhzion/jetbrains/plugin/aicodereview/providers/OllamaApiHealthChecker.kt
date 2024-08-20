package com.github.yhzion.jetbrains.plugin.aicodereview.providers

import com.github.yhzion.jetbrains.plugin.aicodereview.utils.ApiHealthChecker
import com.github.yhzion.jetbrains.plugin.aicodereview.utils.Response

open class OllamaApiHealthChecker(url: String) : ApiHealthChecker(url) {

    public override fun isHealthy(response: Response): Boolean {
        return response.statusCode == 200 && response.body == "Ollama is running"
    }
}