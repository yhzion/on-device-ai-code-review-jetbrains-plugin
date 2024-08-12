package com.github.yhzion.jetbrains.plugin.aicodereview.services

import com.github.yhzion.jetbrains.plugin.aicodereview.AICodeReviewSettings
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
import okio.Buffer
import java.util.concurrent.TimeUnit

class AICodeReviewService(private val project: Project) {

    private val settings = AICodeReviewSettings.instance
    private val client = OkHttpClient.Builder()
        .connectTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
//            println("HTTP ${request.method} ${request.url}")
//            println("Request headers: ${request.headers}")
//            println("Response code: ${response.code}")
//            println("Response headers: ${response.headers}")
            response
        }
        .build()

    suspend fun reviewChangedFiles(progressCallback: suspend (String) -> Unit): List<FileReviewResult> = withContext(Dispatchers.IO) {
        val changedFiles = getChangedFiles()
        progressCallback("Found ${changedFiles.size} changed files\n")
        // Append changed filename list
        // ex) Found 3 changed files
        //    - file1
        //    - file2
        //    - file3
        changedFiles.forEach { file ->
            progressCallback("- ${file.name}\n")
        }

        progressCallback("\n\n\n")

        changedFiles.mapNotNull { file ->
            val fullContent = file.contentsToByteArray().toString(Charsets.UTF_8)
            val changedContent = getChangedContent(file)
            if (changedContent.isNotEmpty()) {
                val review = requestReview(file.name, fullContent, changedContent, progressCallback)
                FileReviewResult(file.name, "\n\n\n" + review)
            } else {
                progressCallback("# No significant changes in ${file.name}\n")
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

    private suspend fun requestReview(fileName: String, fullContent: String, changedContent: String, progressCallback: suspend (String) -> Unit): String = withContext(Dispatchers.IO) {
        val PROMPT_START = "You must response in {PREFERRED_LANGUAGE}: ["
        val PROMPT_END = "]"
        val dynamicPromptContent = settings.PROMPT
        val dynamicPrompt = "$PROMPT_START\n$dynamicPromptContent\n$PROMPT_END".replace("{PREFERRED_LANGUAGE}", settings.PREFERRED_LANGUAGE)

        val requestBody = createRequestBody(dynamicPrompt, fullContent, changedContent)
        val endpoint = getEndpointForServiceProvider(settings.SERVICE_PROVIDER)

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .apply {
                when (settings.SERVICE_PROVIDER) {
                    "claude" -> addHeader("x-api-key", settings.CLAUDE_API_KEY)
                    "openai", "groq" -> addHeader("Authorization", "Bearer ${settings.OPENAI_API_KEY}")
                }
            }
            .build()

        val result = StringBuilder()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("API call failed: ${response.code}")

            val bufferedSource = response.body?.source() ?: throw Exception("Empty response")
            val buffer = Buffer()

            while (!bufferedSource.exhausted()) {
                bufferedSource.read(buffer, settings.STREAMING_CHUNK_SIZE.toLong())
                val chunk = buffer.readUtf8()
                val jsonChunk = JSONObject(chunk)

                // 서비스 프로바이더에 맞는 RESPONSE_PATH 사용
                val responsePath = getResponsePathForServiceProvider(settings.SERVICE_PROVIDER)
                val extractedContent = extractContentFromJson(jsonChunk, responsePath)

                // 실시간 업데이트
                withContext(Dispatchers.Main) {
                    progressCallback(extractedContent) // 이 함수는 UI를 업데이트하는 메서드입니다.
                }

                result.append(extractedContent)
            }
        }

        return@withContext result.toString()
    }

    private fun createRequestBody(prompt: String, fullContent: String, changedContent: String): RequestBody {
//        println("Prompt '$prompt'")
        val json = JSONObject().apply {
            put("model", settings.MODEL)
            put("stream", true)
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
        }

        return current.toString()
    }

    private fun getResponsePathForServiceProvider(serviceProvider: String): String {
        return when (serviceProvider) {
            "ollama" -> settings.OLLAMA_RESPONSE_PATH
            "claude" -> settings.CLAUDE_RESPONSE_PATH
            "gemini" -> settings.GEMINI_RESPONSE_PATH
            "groq" -> settings.GROQ_RESPONSE_PATH
            "openai" -> settings.OPENAI_RESPONSE_PATH
            else -> throw IllegalArgumentException("Unknown service provider: $serviceProvider")
        }
    }

    private fun getEndpointForServiceProvider(serviceProvider: String): String {
        return when (serviceProvider) {
            "ollama" -> settings.OLLAMA_ENDPOINT
            "claude" -> settings.CLAUDE_ENDPOINT
            "gemini" -> settings.GEMINI_ENDPOINT.replace("{MODEL}", settings.MODEL).replace("{API_KEY}", settings.GEMINI_API_KEY)
            "groq" -> settings.GROQ_ENDPOINT
            "openai" -> settings.OPENAI_ENDPOINT
            else -> throw IllegalArgumentException("Unknown service provider: $serviceProvider")
        }
    }
}

data class FileReviewResult(val fileName: String, val review: String)
