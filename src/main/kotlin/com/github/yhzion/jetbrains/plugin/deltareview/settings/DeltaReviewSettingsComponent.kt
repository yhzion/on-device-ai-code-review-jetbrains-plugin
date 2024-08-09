package com.github.yhzion.jetbrains.plugin.deltareview.settings

import com.github.yhzion.jetbrains.plugin.deltareview.DeltaReviewBundle
import com.github.yhzion.jetbrains.plugin.deltareview.DeltaReviewBundle.message
import com.github.yhzion.jetbrains.plugin.deltareview.DeltaReviewSettings
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import javax.swing.*
import java.awt.event.ItemEvent

class DeltaReviewSettingsComponent {
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

    private val claudeApiKeyLabel = JLabel(message("plugin.settings.claudeApiKey"))
    private val geminiApiKeyLabel = JLabel(message("plugin.settings.geminiApiKey"))
    private val groqApiKeyLabel = JLabel(message("plugin.settings.groqApiKey"))
    private val openAiApiKeyLabel = JLabel(message("plugin.settings.openAiApiKey"))
    private val ollamaEndpointLabel = JLabel(message("plugin.settings.ollamaEndpoint"))
    private val anthropicVersionLabel = JLabel(message("plugin.settings.anthropicVersion"))

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
            .addLabeledComponent(message("plugin.settings.maxTokens"), maxTokensField)
            .addLabeledComponent(message("plugin.settings.fileExtensions"), fileExtensionsField)
            .addLabeledComponent(message("plugin.settings.serviceProvider"), serviceProviderComboBox)
            .addLabeledComponent(message("plugin.settings.model"), modelField)
            .addLabeledComponent(ollamaEndpointLabel, ollamaEndpointField)  // 이미 라벨에 대한 국제화 처리가 되어 있다고 가정
            .addLabeledComponent(claudeApiKeyLabel, claudeApiKeyField)  // 이미 라벨에 대한 국제화 처리가 되어 있다고 가정
            .addLabeledComponent(geminiApiKeyLabel, geminiApiKeyField)  // 이미 라벨에 대한 국제화 처리가 되어 있다고 가정
            .addLabeledComponent(groqApiKeyLabel, groqApiKeyField)  // 이미 라벨에 대한 국제화 처리가 되어 있다고 가정
            .addLabeledComponent(openAiApiKeyLabel, openAiApiKeyField)  // 이미 라벨에 대한 국제화 처리가 되어 있다고 가정
            .addLabeledComponent(anthropicVersionLabel, anthropicVersionField)  // 이미 라벨에 대한 국제화 처리가 되어 있다고 가정
            .addLabeledComponent(
                message("plugin.settings.preferredLanguage"),
                preferredLanguageComboBox
            )
            .addLabeledComponent(message("plugin.settings.responsePath"), responsePathField)
            .addLabeledComponent(message("plugin.settings.prompt"), promptScrollPane)
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