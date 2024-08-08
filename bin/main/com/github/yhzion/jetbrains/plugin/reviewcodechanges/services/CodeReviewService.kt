package com.github.yhzion.jetbrains.plugin.reviewcodechanges.services

import com.github.yhzion.jetbrains.plugin.reviewcodechanges.CodeReviewSettings
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

class CodeReviewService(private val project: Project) {

    private val settings = CodeReviewSettings.instance
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
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
        val requestBody = when (settings.SERVICE_PROVIDER) {
            "gemini" -> createGeminiRequestBody(fullContent, changedContent)
            else -> createDefaultRequestBody(fullContent, changedContent)
        }

        val request = Request.Builder()
            .url(settings.ENDPOINT)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .apply {
                when (settings.SERVICE_PROVIDER) {
                    "claude" -> {
                        addHeader("x-api-key", settings.API_KEY)
                        addHeader("anthropic-version", settings.ANTHROPIC_VERSION)
                    }
                    "chatgpt", "groq" -> addHeader("Authorization", "Bearer ${settings.API_KEY}")
                }
            }
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("API call failed: ${response.code}")

            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            val jsonResponse = JSONObject(responseBody)

            // RESPONSE_PATH를 사용하여 JSON에서 데이터를 추출
            extractContentFromJson(jsonResponse, settings.RESPONSE_PATH)
        }
    }

    private fun createGeminiRequestBody(fullContent: String, changedContent: String): RequestBody {
        val json = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", "${settings.PROMPT}\n\nFull file content:\n$fullContent\n\nChanged content:\n$changedContent")
                }))
            }))
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", settings.MAX_TOKENS)
            })
        }
        return json.toString().toRequestBody("application/json".toMediaType())
    }

    private fun createDefaultRequestBody(fullContent: String, changedContent: String): RequestBody {
        val json = JSONObject().apply {
            put("model", settings.MODEL)
            put("stream", false)
            put("max_tokens", settings.MAX_TOKENS)
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", "${settings.PROMPT}\n\nFull file content:\n$fullContent\n\nChanged content:\n$changedContent")
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
        }

        return current.toString()
    }
}

data class FileReviewResult(val fileName: String, val review: String)