package com.github.yhzion.jetbrains.plugin.aicodereview.providers

import com.github.yhzion.jetbrains.plugin.aicodereview.utils.ApiHealthChecker
import com.github.yhzion.jetbrains.plugin.aicodereview.utils.Response
import java.net.HttpURLConnection
import java.net.URL

class ClaudeApiHealthChecker : ApiHealthChecker() {

    override fun isHealthy(response: Response): Boolean {
        // 상태 코드가 405일 때 헬스 체크가 성공으로 간주됨
        return response.statusCode == 405
    }

    override fun getDefaultUrl(): String {
        return "https://api.anthropic.com/v1/messages"
    }
}