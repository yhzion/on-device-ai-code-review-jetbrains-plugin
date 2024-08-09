package com.github.yhzion.jetbrains.plugin.deltareview.settings

import com.github.yhzion.jetbrains.plugin.deltareview.DeltaReviewSettings
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import javax.swing.*
import java.awt.event.ItemEvent

class CodeReviewSettingsComponent {
    private val ollamaEndpointField = JBTextField()
    private val claudeApiKeyField = JBTextField()
    private val geminiApiKeyField = JBTextField()
    private val groqApiKeyField = JBTextField()
    private val openAiApiKeyField = JBTextField()
    private val maxTokensField = JBTextField()
    private val fileExtensionsField = JBTextField()
    private val modelField = JBTextField()
    private val responsePathField = JBTextField()
    private val anthropicVersionField = JBTextField()
    private val promptField = JTextArea(5, 50).apply {
        lineWrap = true
        wrapStyleWord = true
    }
    private val promptScrollPane = JBScrollPane(promptField)

    private val claudeApiKeyLabel = JLabel("Claude API Key")
    private val geminiApiKeyLabel = JLabel("Gemini API Key")
    private val groqApiKeyLabel = JLabel("Groq API Key")
    private val openAiApiKeyLabel = JLabel("OpenAI API Key")
    private val ollamaEndpointLabel = JLabel("Ollama Endpoint")
    private val anthropicVersionLabel = JLabel("Anthropic version")

    private val panel: JPanel

    private val serviceProviders = listOf("ollama", "claude", "openai", "gemini", "groq")
    private val serviceProviderComboBox = ComboBox(serviceProviders.toTypedArray())

    private val languages = loadLanguagesFromFile("language_list.json")
    private val languageNames = languages.map { it.name }
    private val preferredLanguageComboBox = ComboBox(languageNames.toTypedArray())

    init {
        serviceProviderComboBox.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                updateFieldsForSelectedProvider()
                updateFieldVisibility()
            }
        }

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Max tokens", maxTokensField)
            .addLabeledComponent("File extensions", fileExtensionsField)
            .addLabeledComponent("Service provider", serviceProviderComboBox)
            .addLabeledComponent("Model", modelField)
            .addLabeledComponent(ollamaEndpointLabel, ollamaEndpointField)
            .addLabeledComponent(claudeApiKeyLabel, claudeApiKeyField)
            .addLabeledComponent(geminiApiKeyLabel, geminiApiKeyField)
            .addLabeledComponent(groqApiKeyLabel, groqApiKeyField)
            .addLabeledComponent(openAiApiKeyLabel, openAiApiKeyField)
            .addLabeledComponent(anthropicVersionLabel, anthropicVersionField)
            .addLabeledComponent("Preferred language", preferredLanguageComboBox)
            .addLabeledComponent("Response path", responsePathField)
            .addLabeledComponent("Prompt", promptScrollPane)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        updateFieldVisibility()
    }

    private fun updateFieldsForSelectedProvider() {
        val selectedProvider = serviceProviderComboBox.selectedItem as String

        when (selectedProvider) {
            "claude" -> {
                modelField.text = "claude-3-5-sonnet-20240620"
                anthropicVersionField.text = "2023-06-01"
                responsePathField.text = DeltaReviewSettings.instance.CLAUDE_RESPONSE_PATH
            }

            "openai" -> {
                modelField.text = "gpt-4o-mini"
                responsePathField.text = DeltaReviewSettings.instance.OPENAI_RESPONSE_PATH
            }

            "gemini" -> {
                modelField.text = "gemini-1.5-flash"
                responsePathField.text = DeltaReviewSettings.instance.GEMINI_RESPONSE_PATH
            }

            "groq" -> {
                modelField.text = "llama-3.1-70b-versatile"
                responsePathField.text = DeltaReviewSettings.instance.GROQ_RESPONSE_PATH
            }

            "ollama" -> {
                modelField.text = "mistral-nemo"
                responsePathField.text = DeltaReviewSettings.instance.OLLAMA_RESPONSE_PATH
            }
        }
    }

    private fun updateFieldVisibility() {
        val selectedProvider = serviceProviderComboBox.selectedItem as String
        ollamaEndpointLabel.isVisible = selectedProvider == "ollama"
        ollamaEndpointField.isVisible = selectedProvider == "ollama"
        claudeApiKeyLabel.isVisible = selectedProvider == "claude"
        claudeApiKeyField.isVisible = selectedProvider == "claude"
        geminiApiKeyLabel.isVisible = selectedProvider == "gemini"
        geminiApiKeyField.isVisible = selectedProvider == "gemini"
        groqApiKeyLabel.isVisible = selectedProvider == "groq"
        groqApiKeyField.isVisible = selectedProvider == "groq"
        openAiApiKeyLabel.isVisible = selectedProvider == "openai"
        openAiApiKeyField.isVisible = selectedProvider == "openai"
        anthropicVersionLabel.isVisible = selectedProvider == "claude"
        anthropicVersionField.isVisible = selectedProvider == "claude"
    }

    fun apply(settings: DeltaReviewSettings) {
        settings.OLLAMA_ENDPOINT = ollamaEndpointField.text
        settings.CLAUDE_API_KEY = claudeApiKeyField.text
        settings.GEMINI_API_KEY = geminiApiKeyField.text
        settings.GROQ_API_KEY = groqApiKeyField.text
        settings.OPENAI_API_KEY = openAiApiKeyField.text
        settings.MAX_TOKENS = maxTokensField.text.toIntOrNull() ?: 4096
        settings.FILE_EXTENSIONS = fileExtensionsField.text
        settings.SERVICE_PROVIDER = serviceProviderComboBox.selectedItem as String
        settings.MODEL = modelField.text
        settings.ANTHROPIC_VERSION = anthropicVersionField.text
        settings.PROMPT = promptField.text
        settings.PREFERRED_LANGUAGE = preferredLanguageComboBox.selectedItem as String

        // 서비스 제공자별 RESPONSE_PATH 설정 적용
        when (settings.SERVICE_PROVIDER) {
            "ollama" -> settings.OLLAMA_RESPONSE_PATH = responsePathField.text
            "claude" -> settings.CLAUDE_RESPONSE_PATH = responsePathField.text
            "gemini" -> settings.GEMINI_RESPONSE_PATH = responsePathField.text
            "groq" -> settings.GROQ_RESPONSE_PATH = responsePathField.text
            "openai" -> settings.OPENAI_RESPONSE_PATH = responsePathField.text
        }
    }

    fun reset(settings: DeltaReviewSettings) {
        ollamaEndpointField.text = settings.OLLAMA_ENDPOINT
        claudeApiKeyField.text = settings.CLAUDE_API_KEY
        geminiApiKeyField.text = settings.GEMINI_API_KEY
        groqApiKeyField.text = settings.GROQ_API_KEY
        openAiApiKeyField.text = settings.OPENAI_API_KEY
        maxTokensField.text = settings.MAX_TOKENS.toString()
        fileExtensionsField.text = settings.FILE_EXTENSIONS
        serviceProviderComboBox.selectedItem = settings.SERVICE_PROVIDER
        modelField.text = settings.MODEL
        anthropicVersionField.text = settings.ANTHROPIC_VERSION
        promptField.text = settings.PROMPT
        preferredLanguageComboBox.selectedItem = settings.PREFERRED_LANGUAGE

        // 서비스 제공자별 RESPONSE_PATH 설정 복구
        when (settings.SERVICE_PROVIDER) {
            "ollama" -> responsePathField.text = settings.OLLAMA_RESPONSE_PATH
            "claude" -> responsePathField.text = settings.CLAUDE_RESPONSE_PATH
            "gemini" -> responsePathField.text = settings.GEMINI_RESPONSE_PATH
            "groq" -> responsePathField.text = settings.GROQ_RESPONSE_PATH
            "openai" -> responsePathField.text = settings.OPENAI_RESPONSE_PATH
        }
        updateFieldVisibility()
    }

    fun isModified(settings: DeltaReviewSettings): Boolean {
        val isResponsePathModified = when (settings.SERVICE_PROVIDER) {
            "ollama" -> responsePathField.text != settings.OLLAMA_RESPONSE_PATH
            "claude" -> responsePathField.text != settings.CLAUDE_RESPONSE_PATH
            "gemini" -> responsePathField.text != settings.GEMINI_RESPONSE_PATH
            "groq" -> responsePathField.text != settings.GROQ_RESPONSE_PATH
            "openai" -> responsePathField.text != settings.OPENAI_RESPONSE_PATH
            else -> false
        }

        return ollamaEndpointField.text != settings.OLLAMA_ENDPOINT ||
                claudeApiKeyField.text != settings.CLAUDE_API_KEY ||
                geminiApiKeyField.text != settings.GEMINI_API_KEY ||
                groqApiKeyField.text != settings.GROQ_API_KEY ||
                openAiApiKeyField.text != settings.OPENAI_API_KEY ||
                maxTokensField.text.toIntOrNull() != settings.MAX_TOKENS ||
                fileExtensionsField.text != settings.FILE_EXTENSIONS ||
                serviceProviderComboBox.selectedItem != settings.SERVICE_PROVIDER ||
                modelField.text != settings.MODEL ||
                anthropicVersionField.text != settings.ANTHROPIC_VERSION ||
                promptField.text != settings.PROMPT ||
                preferredLanguageComboBox.selectedItem != settings.PREFERRED_LANGUAGE ||
                isResponsePathModified
    }

    fun getPanel(): JPanel = panel
}