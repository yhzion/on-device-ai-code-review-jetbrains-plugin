package com.github.yhzion.jetbrains.plugin.aicodereview.utils

import com.github.yhzion.jetbrains.plugin.aicodereview.AICodeReviewSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ClientUtils {
    private const val TIMEOUT = 300L

    private fun createHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                response
            }
            .build()
    }

    private suspend fun executeRequest(
        client: OkHttpClient,
        request: Request,
        settings: AICodeReviewSettings,
        progressCallback: suspend (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val result = StringBuilder()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("API call failed: ${response.code}")

            val bufferedSource = response.body?.source() ?: throw Exception("Empty response")
            val buffer = Buffer()
            progressCallback("\n")
            while (!bufferedSource.exhausted()) {
                bufferedSource.read(buffer, settings.STREAMING_CHUNK_SIZE.toLong())
                var chunk = buffer.readUtf8()
                val dataPrefix = "data:"
                val dataPrefixIndex = chunk.indexOf(dataPrefix)
                if (dataPrefixIndex != -1) {
                    chunk = chunk.substring(dataPrefixIndex + dataPrefix.length)
                }

                val isArray = chunk.startsWith("[") && chunk.endsWith("]") && chunk.count { it == '{' } > 1
                val jsonChunk = if (isArray) {
                    val jsonArray = JSONArray(chunk)
                    jsonArray.getJSONObject(0)
                } else {
                    JSONObject(chunk)
                }

                val responsePath = ServiceProviderUtils.getResponsePathForServiceProvider(settings.SERVICE_PROVIDER)
                val extractedContent = JsonUtils.extractContentFromJson(jsonChunk, responsePath)

                withContext(Dispatchers.Main) {
                    progressCallback(extractedContent)
                }

                result.append(extractedContent)
            }
        }

        return@withContext result.toString()
    }

    private fun createRequestBody(prompt: String, settings: AICodeReviewSettings): RequestBody {
        return ServiceProviderUtils.createRequestBodyForServiceProvider(prompt, settings)
    }

    suspend fun requestReview(
        fileName: String,
        fullContent: String,
        changedContent: String,
        settings: AICodeReviewSettings,
        progressCallback: suspend (String) -> Unit
    ): String {
        val prompt = PromptGenerator.generatePrompt(fileName, fullContent, changedContent, settings)
        val requestBody = createRequestBody(prompt, settings)
        val endpoint = ServiceProviderUtils.getEndpointForServiceProvider(settings.SERVICE_PROVIDER)
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .apply {
                val headers = ServiceProviderUtils.getHeadersForServiceProvider(settings.SERVICE_PROVIDER, settings)
                headers.forEach { (key, value) ->
                    addHeader(key, value)
                }
            }
            .build()

        return executeRequest(createHttpClient(), request, settings, progressCallback)
    }
}
