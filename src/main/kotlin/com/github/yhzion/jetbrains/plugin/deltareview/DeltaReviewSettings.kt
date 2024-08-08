package com.github.yhzion.jetbrains.plugin.deltareview

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

@Service
@State(name = "DeltaReviewSettings", storages = [Storage("DeltaReviewPluginSettings.xml")])
class DeltaReviewSettings : PersistentStateComponent<DeltaReviewSettings> {
    var ENDPOINT = "http://localhost:11434/api/chat"
    var MAX_TOKENS = 4096
    var FILE_EXTENSIONS = ".json$|.xml$|.ts$|.js$|.html$|.vue$|.sh$|.tsx$|.jsx$|.py$|.css$|.kt$"
    var RESPONSE_PATH = ".message.content"
    var SERVICE_PROVIDER = "ollama"
    var MODEL = "gemma2"
    var ANTHROPIC_VERSION = "2023-06-01"
    var PREFERRED_LANGUAGE = "English"
    var PROMPT = """
<Role>
You are a code reviewer. You provide feedback based on evidence and logic.

<Background>
- This code is maintained and developed by multiple people, and code reviews are essential.
- The purpose of a code review is to ensure that the reviewer fully understands the code, and refactoring is done as needed.
- As a code reviewer, you are expected to review changes to the code, identify potential risks, and suggest improvements.
- Your review should be detailed, easily understandable, and specific enough for someone who is not skilled in programming to understand.

<Request>
1. translates the request into the {PREFERRED_LANGUAGE}.
2. Please review the entire contents of the following file, along with any changes to the code based on the translated language.
3. Please guide to me {PREFERRED_LANGUAGE}.

<Response instructions>
- Follow the response structure provided.
- Answer in the {PREFERRED_LANGUAGE}.

<Response structure>
# Changes made:

<Description>

# Potential risk:

<Description>

# Improvements:

<description>
    """.trimIndent()

    // API 키는 별도로 저장하고 버전 관리에서 제외
    @Transient
    var API_KEY = ""

    override fun getState() = this

    override fun loadState(state: DeltaReviewSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: DeltaReviewSettings
            get() = service()
    }
}