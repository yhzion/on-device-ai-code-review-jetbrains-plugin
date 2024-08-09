package com.github.yhzion.jetbrains.plugin.deltareview.services

import com.github.yhzion.jetbrains.plugin.deltareview.DeltaReviewSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class DeltaReviewService(private val project: Project) {

    private val settings = DeltaReviewSettings.instance
    private val client = OkHttpClient.Builder()
        .connectTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            println("HTTP ${request.method} ${request.url}")
            println("Request headers: ${request.headers}")
            println("Response code: ${response.code}")
            println("Response headers: ${response.headers}")
            response
        }
        .build()

    suspend fun reviewChangedFiles(progressCallback: suspend (String) -> Unit): List<FileReviewResult> = withContext(Dispatchers.IO) {
        val changedFiles = getChangedFiles()
        progressCallback("Found ${changedFiles.size} changed files")

        changedFiles.mapNotNull { file ->
            progressCallback("Reviewing file: ${file.name}")
            val fullContent = file.contentsToByteArray().toString(Charsets.UTF_8)
            val changedContent = getChangedContent(file)
            if (changedContent.isNotEmpty()) {
                progressCallback("Sending review request for ${file.name}")
                val review = requestReview(file.name, fullContent, changedContent)
                progressCallback("Received review for ${file.name}")
                FileReviewResult(file.name, review)
            } else {
                progressCallback("Skipping ${file.name} (no changes)")
                null
            }
        }
    }

    private fun getChangedFiles(): List<VirtualFile> {
        val changeListManager = ChangeListManager.getInstance(project)
        val regex = Regex(settings.FILE_EXTENSIONS)

        return changeListManager.affectedFiles.filter { file ->
            regex.containsMatchIn(file.name)
        }
    }

    private fun getChangedContent(file: VirtualFile): String {
        val changeListManager = ChangeListManager.getInstance(project)
        val change: Change? = changeListManager.getChange(file)

        return when (val contentRevision = change?.afterRevision) {
            is ContentRevision -> contentRevision.content ?: ""
            else -> ""
        }
    }

    private suspend fun requestReview(fileName: String, fullContent: String, changedContent: String): String = withContext(Dispatchers.IO) {
        val dynamicPrompt = settings.PROMPT.replace("{PREFERRED_LANGUAGE}", settings.PREFERRED_LANGUAGE)
        println("Dynamic prompt: $dynamicPrompt")

        val requestBody = when (settings.SERVICE_PROVIDER) {
            "gemini" -> createGeminiRequestBody(dynamicPrompt, fullContent, changedContent)
            else -> createDefaultRequestBody(dynamicPrompt, fullContent, changedContent)
        }

        val endpoint = when (settings.SERVICE_PROVIDER) {
            "ollama" -> settings.OLLAMA_ENDPOINT
            "claude" -> settings.CLAUDE_ENDPOINT
            "gemini" -> settings.GEMINI_ENDPOINT.replace("{MODEL}", settings.MODEL).replace("{API_KEY}", settings.GEMINI_API_KEY)
            "groq" -> settings.GROQ_ENDPOINT
            "openai" -> settings.OPENAI_ENDPOINT
            else -> throw IllegalArgumentException("Unknown service provider: ${settings.SERVICE_PROVIDER}")
        }

        val responsePath = when (settings.SERVICE_PROVIDER) {
            "ollama" -> settings.OLLAMA_RESPONSE_PATH
            "claude" -> settings.CLAUDE_RESPONSE_PATH
            "gemini" -> settings.GEMINI_RESPONSE_PATH
            "groq" -> settings.GROQ_RESPONSE_PATH
            "openai" -> settings.OPENAI_RESPONSE_PATH
            else -> throw IllegalArgumentException("Unknown service provider: ${settings.SERVICE_PROVIDER}")
        }

        println(endpoint)

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .apply {
                when (settings.SERVICE_PROVIDER) {
                    "claude" -> {
                        addHeader("x-api-key", settings.CLAUDE_API_KEY)
                        addHeader("anthropic-version", settings.ANTHROPIC_VERSION)
                    }
                    "openai", "groq" -> addHeader("Authorization", "Bearer ${settings.OPENAI_API_KEY}")
                }
            }
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("API call failed: ${response.code}")

            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            val jsonResponse = JSONObject(responseBody)

            // 전체 JSON 응답 로그
            println("Full JSON response: $jsonResponse")

            // 추출할 경로 확인 로그
            println("Response path: $responsePath")

            // 추출된 결과 로그
            val extractedContent = extractContentFromJson(jsonResponse, responsePath)
            println("Extracted content: $extractedContent")

            extractContentFromJson(jsonResponse, responsePath)
        }
    }

    private fun createGeminiRequestBody(prompt: String, fullContent: String, changedContent: String): RequestBody {
        val json = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", "$prompt\n\nFull file content:\n$fullContent\n\nChanged content:\n$changedContent")
                }))
            }))
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", settings.MAX_TOKENS)
            })
        }
        return json.toString().toRequestBody("application/json".toMediaType())
    }

    private fun createDefaultRequestBody(prompt: String, fullContent: String, changedContent: String): RequestBody {
        val json = JSONObject().apply {
            put("model", settings.MODEL)
            put("stream", false)
            put("max_tokens", settings.MAX_TOKENS)
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", "$prompt\n\nFull file content:\n$fullContent\n\nChanged content:\n$changedContent")
            }))
        }
        return json.toString().toRequestBody("application/json".toMediaType())
    }

    private fun extractContentFromJson(json: JSONObject, path: String): String {
        var current: Any = json
        val keys = path.split('.').filter { it.isNotEmpty() }

        for (key in keys) {
            if (current is JSONObject) {
                current = if (key.endsWith("]")) {
                    val arrayKey = key.substringBefore('[')
                    val index = key.substringAfter('[').substringBefore(']').toInt()
                    current.getJSONArray(arrayKey).get(index)
                } else {
                    current.get(key)
                }
            } else {
                throw IllegalArgumentException("Unexpected JSON structure for path: $path")
            }
            // 로그 추가
            println("Current JSON node after processing key '$key': $current")
        }

        return current.toString()
    }
}

data class FileReviewResult(val fileName: String, val review: String)