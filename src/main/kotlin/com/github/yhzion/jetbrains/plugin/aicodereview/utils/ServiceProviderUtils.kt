package com.github.yhzion.jetbrains.plugin.aicodereview.utils

import com.github.yhzion.jetbrains.plugin.aicodereview.AICodeReviewSettings
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object ServiceProviderUtils {
    fun getResponsePathForServiceProvider(serviceProvider: String): String {
        val settings = AICodeReviewSettings.instance
        return when (serviceProvider) {
            "ollama" -> settings.OLLAMA_RESPONSE_PATH
            "claude" -> settings.CLAUDE_RESPONSE_PATH
            "gemini" -> settings.GEMINI_RESPONSE_PATH
            "groq" -> settings.GROQ_RESPONSE_PATH
            "openai" -> settings.OPENAI_RESPONSE_PATH
            else -> throw IllegalArgumentException("Unknown service provider: $serviceProvider")
        }
    }

    fun getEndpointForServiceProvider(serviceProvider: String): String {
        val settings = AICodeReviewSettings.instance
        return when (serviceProvider) {
            "ollama" -> settings.OLLAMA_ENDPOINT
            "claude" -> settings.CLAUDE_ENDPOINT
            "gemini" -> settings.GEMINI_ENDPOINT.replace("{MODEL}", settings.MODEL).replace("{API_KEY}", settings.GEMINI_API_KEY)
            "groq" -> settings.GROQ_ENDPOINT
            "openai" -> settings.OPENAI_ENDPOINT
            else -> throw IllegalArgumentException("Unknown service provider: $serviceProvider")
        }
    }

    fun getHeadersForServiceProvider(serviceProvider: String, settings: AICodeReviewSettings): Map<String, String> {
        return when (serviceProvider) {
            "claude" -> mapOf("x-api-key" to settings.CLAUDE_API_KEY)
            "openai" -> mapOf("Authorization" to "Bearer ${settings.OPENAI_API_KEY}")
            "groq" -> mapOf("Authorization" to "Bearer ${settings.GROQ_API_KEY}")
            else -> emptyMap()
        }
    }


    fun createRequestBodyForServiceProvider(prompt: String, settings: AICodeReviewSettings): RequestBody {
        val json = JSONObject().apply {
            when (settings.SERVICE_PROVIDER) {
                "gemini" -> {
                    put("contents", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().apply {
                            put("text", prompt)
                        }))
                    }))
                }
                "groq" -> {
                    put("stream", false)
                    put("model", settings.MODEL)
                    put("max_tokens", settings.MAX_TOKENS)
                    put("messages", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    }))
                }
                else -> {
                    put("stream", true)
                    put("model", settings.MODEL)
                    put("max_tokens", settings.MAX_TOKENS)
                    put("messages", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    }))
                }
            }
        }
        return json.toString().toRequestBody("application/json".toMediaType())
    }
}
