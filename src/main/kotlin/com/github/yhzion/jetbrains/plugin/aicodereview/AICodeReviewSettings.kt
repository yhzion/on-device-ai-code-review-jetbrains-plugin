package com.github.yhzion.jetbrains.plugin.aicodereview

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import com.github.yhzion.jetbrains.plugin.aicodereview.settings.AICodeReviewSettingsConfigurable

@Service
@State(name = "AICodeReviewSettings", storages = [Storage("AICodeReviewPluginSettings.xml")])
class AICodeReviewSettings : PersistentStateComponent<AICodeReviewSettings> {
    var OLLAMA_ENDPOINT = "http://localhost:11434/api/chat"
    var CLAUDE_ENDPOINT = "https://api.anthropic.com/v1/messages"
    var GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1/models/{MODEL}:generateContent?key={API_KEY}"
    var GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
    var OPENAI_ENDPOINT = "https://api.openai.com/v1/chat/completions"

    var CLAUDE_API_KEY = ""
    var GEMINI_API_KEY = ""
    var GROQ_API_KEY = ""
    var OPENAI_API_KEY = ""

    var MAX_TOKENS = 4096
    var FILE_EXTENSIONS = ".json$|.xml$|.ts$|.js$|.html$|.vue$|.sh$|.tsx$|.jsx$|.py$|.css$|.kt$"

    var OLLAMA_RESPONSE_PATH = ".message.content"
    var CLAUDE_RESPONSE_PATH = ".content[0].text"
    var GEMINI_RESPONSE_PATH = ".candidates[0].content.parts[0].text"
    var GROQ_RESPONSE_PATH = ".choices[0].message.content"
    var OPENAI_RESPONSE_PATH = ".choices[0].message.content"

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

    override fun getState() = this

    override fun loadState(state: AICodeReviewSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun notifySettingsChanged() {
        ApplicationManager.getApplication().messageBus.syncPublisher(AICodeReviewSettingsConfigurable.SETTINGS_CHANGED_TOPIC).settingsChanged(this)
    }

    companion object {
        val instance: AICodeReviewSettings
            get() = service()
    }
}