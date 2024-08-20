package com.github.yhzion.jetbrains.plugin.aicodereview.providers

import com.github.yhzion.jetbrains.plugin.aicodereview.utils.ApiHealthChecker
import com.github.yhzion.jetbrains.plugin.aicodereview.utils.Response
import java.net.HttpURLConnection
import java.net.URL

class GroqApiHealthChecker(url: String) : ApiHealthChecker(url) {

    override fun isHealthy(response: Response): Boolean {
        // 상태 코드가 500 이상이 아니면 헬스 체크가 성공으로 간주됨
        return response.statusCode < 500
    }
}