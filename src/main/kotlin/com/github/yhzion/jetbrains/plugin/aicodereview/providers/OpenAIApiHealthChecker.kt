package com.github.yhzion.jetbrains.plugin.aicodereview.providers

import com.github.yhzion.jetbrains.plugin.aicodereview.utils.ApiHealthChecker
import com.github.yhzion.jetbrains.plugin.aicodereview.utils.Response

class OpenAIApiHealthChecker() : ApiHealthChecker() {

    override fun isHealthy(response: Response): Boolean {
        // 상태 코드가 401일 때 헬스 체크가 성공으로 간주됨
        return response.statusCode == 401
    }
    override fun getDefaultUrl(): String {
        return "https://api.openai.com/v1/chat/completions"
    }
}
