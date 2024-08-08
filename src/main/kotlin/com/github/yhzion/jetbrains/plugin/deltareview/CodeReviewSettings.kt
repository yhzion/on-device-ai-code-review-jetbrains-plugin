package com.github.yhzion.jetbrains.plugin.deltareview

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

@Service
@State(name = "DeltaReviewSettings", storages = [Storage("DeltaReviewPluginSettings.xml")])

class CodeReviewSettings : PersistentStateComponent<CodeReviewSettings> {
    var ENDPOINT = "http://localhost:11434/api/chat"
    var MAX_TOKENS = 4096
    var FILE_EXTENSIONS = ".json$|.xml$|.ts$|.js$|.html$|.vue$|.sh$|.tsx$|.jsx$|.py$|.css$"
    var RESPONSE_PATH = ".message.content"
    var SERVICE_PROVIDER = "ollama"
    var MODEL = "gemma2"
    var ANTHROPIC_VERSION = "2023-06-01"
    var PROMPT = """
        <역할>
        당신은 코드 리뷰어입니다. 당신은 증거와 논리에 기반하여 피드백을 제공합니다.

        <배경>
        - 이 코드는 여러 사람이 유지보수하고 개발되며, 코드 리뷰가 필수적입니다.
        - 코드 리뷰의 목적은 리뷰어가 코드를 완전히 이해했는지 확인하는 것이며, 리팩토링은 필요에 따라 진행됩니다.
        - 코드 리뷰어로서 당신은 코드의 변경 사항을 검토하고, 잠재적 위험을 식별하며, 개선 방안을 제시해야 합니다.
        - 리뷰는 프로그래밍에 능숙하지 않은 사람도 이해할 수 있도록 상세하고, 쉽게 이해할 수 있으며, 구체적이어야 합니다.

        <요청>
        다음 파일의 전체 내용과 함께 변경된 사항을 검토해 주세요.
        가능하면 한국어로 가이드를 제시해 주세요.

        <응답 지침>
        - 제공된 응답 구조를 따르세요.

        <응답 구조>
        # 변경된 사항:
        <설명>

        # 잠재적 위험:
        <설명>

        # 개선 방안:
        <설명>
    """.trimIndent()

    // API 키는 별도로 저장하고 버전 관리에서 제외
    @Transient
    var API_KEY = ""

    override fun getState() = this

    override fun loadState(state: CodeReviewSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: CodeReviewSettings
            get() = service()
    }
}