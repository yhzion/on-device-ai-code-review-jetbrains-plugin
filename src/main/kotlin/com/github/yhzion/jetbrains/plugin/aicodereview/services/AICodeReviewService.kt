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
            response
        }
        .build()

    suspend fun reviewChangedFiles(progressCallback: suspend (String) -> Unit): List<FileReviewResult> = withContext(Dispatchers.IO) {
        val changedFiles = getChangedFiles()
        progressCallback("Found ${changedFiles.size} changed files\n")
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
        var PROMPT_START = """
        <Role>
You are a code reviewer. You must respond in {PREFERRED_LANGUAGE} only.

<Instructions>
- Your response should be entirely in {PREFERRED_LANGUAGE}. No part of the response should be in another language.
- The title of the response should be "${fileName} Review".
- The response should include sections titled <Changes>, <Potential Risks>, and <Improvements>.
- If the code review includes file content, provide detailed feedback for each section.

<Request>
Please guide me in {PREFERRED_LANGUAGE}:

<Response format>
- Every sentence and all content must be in {PREFERRED_LANGUAGE}.
- The opening subject of the response should be "${fileName} Review".
- The response should be structured as follows:
  - Title: ${fileName} Review
  - Changes
  - Potential Risks
  - Improvements

<Language>
Remember: Only {PREFERRED_LANGUAGE} should be used in your response.
        """.trimIndent()
        var PROMPT_END = """
            
            """
        // Replace {PREFERRED_LANGUAGE} with the settings.PREFERRED_LANGUAGE
        PROMPT_START = PROMPT_START.replace("{PREFERRED_LANGUAGE}", settings.PREFERRED_LANGUAGE)

        val dynamicPromptContent = settings.PROMPT
        val dynamicPrompt = "$PROMPT_START\n$dynamicPromptContent\n$PROMPT_END"

        val requestBody = createRequestBody(dynamicPrompt, fullContent, changedContent)
        val endpoint = getEndpointForServiceProvider(settings.SERVICE_PROVIDER)

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .apply {
                when (settings.SERVICE_PROVIDER) {
                    "claude" -> addHeader("x-api-key", settings.CLAUDE_API_KEY)
                    "openai" -> addHeader("Authorization", "Bearer ${settings.OPENAI_API_KEY}")
                    "groq" -> addHeader("Authorization", "Bearer ${settings.GROQ_API_KEY}")
                }
            }
            .build()

        val result = StringBuilder()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("API call failed: ${response.code}")

            val bufferedSource = response.body?.source() ?: throw Exception("Empty response")
            val buffer = Buffer()
            //println("Streaming enabled: ${settings.USE_STREAMING}")
            while (!bufferedSource.exhausted()) {
                //println("Reading chunk")
                bufferedSource.read(buffer, settings.STREAMING_CHUNK_SIZE.toLong())
                //println("Buffer size: ${buffer.size+10000}")
                var chunk = buffer.readUtf8()


                // chunk is startsWith data: then remove it
                val dataPrefix = "data:"
                val dataPrefixIndex = chunk.indexOf(dataPrefix)
                if (dataPrefixIndex != -1) {
                    chunk = chunk.substring(dataPrefixIndex + dataPrefix.length)
                }

                //println("Chunk: $chunk")


                val isArray = chunk.startsWith("[") && chunk.endsWith("]") && chunk.count { it == '{' } > 1
                val jsonChunk = if (isArray) {
                    val jsonArray = JSONArray(chunk)
                    jsonArray.getJSONObject(0)
                } else {
                    //println("Chunk is not an array. It's an object.")
                    JSONObject(chunk)
                }

                //println("JSON Chunk: $jsonChunk")
                // 서비스 프로바이더에 맞는 RESPONSE_PATH 사용
                val responsePath = getResponsePathForServiceProvider(settings.SERVICE_PROVIDER)
                //println("Response path: $responsePath")
                val extractedContent = extractContentFromJson(jsonChunk, responsePath)
                //println("Extracted content: $extractedContent")

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
        val json = JSONObject().apply {
            if (settings.SERVICE_PROVIDER == "gemini") {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", prompt)
                    }))
                }))
            } else {
                if(settings.SERVICE_PROVIDER == "groq") {
                    put("stream", false)
                } else {
                    put("stream", true)
                }
                put("model", settings.MODEL)

                put("max_tokens", settings.MAX_TOKENS)
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", "$prompt\n\nFull file content:\n$fullContent\n\nChanged content:\n$changedContent")
                }))
            }
        }
        return json.toString().toRequestBody("application/json".toMediaType())
    }

    private fun extractContentFromJson(json: JSONObject, path: String): String {
        var current: Any = json
        val keys = path.split('.').filter { it.isNotEmpty() }
        //println("Keys: $keys")

        for (key in keys) {
            //println("Current: $key")
            if (current is JSONObject) {
                //println("current: $current")
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
