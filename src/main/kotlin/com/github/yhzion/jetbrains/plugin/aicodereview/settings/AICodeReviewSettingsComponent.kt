package com.github.yhzion.jetbrains.plugin.aicodereview.settings

import com.github.yhzion.jetbrains.plugin.aicodereview.AICodeReviewBundle
import com.github.yhzion.jetbrains.plugin.aicodereview.AICodeReviewSettings
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import java.awt.Color
import javax.swing.*
import java.awt.ComponentOrientation
import java.awt.FlowLayout
import java.awt.event.ItemEvent

class AICodeReviewSettingsComponent {
    private val ollamaEndpointField = JBTextField()
    private val claudeApiKeyField = JPasswordField()
    private val geminiApiKeyField = JPasswordField()
    private val groqApiKeyField = JPasswordField()
    private val openAiApiKeyField = JPasswordField()
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

    private val claudeApiKeyLabel = JLabel(AICodeReviewBundle.message("plugin.settings.claudeApiKey"))
    private val geminiApiKeyLabel = JLabel(AICodeReviewBundle.message("plugin.settings.geminiApiKey"))
    private val groqApiKeyLabel = JLabel(AICodeReviewBundle.message("plugin.settings.groqApiKey"))
    private val openAiApiKeyLabel = JLabel(AICodeReviewBundle.message("plugin.settings.openAiApiKey"))
    private val ollamaEndpointLabel = JLabel(AICodeReviewBundle.message("plugin.settings.ollamaEndpoint"))
    private val anthropicVersionLabel = JLabel(AICodeReviewBundle.message("plugin.settings.anthropicVersion"))

    private val claudePanel = createPasswordFieldWithToggle(claudeApiKeyField)
    private val geminiPanel = createPasswordFieldWithToggle(geminiApiKeyField)
    private val groqPanel = createPasswordFieldWithToggle(groqApiKeyField)
    private val openAiPanel = createPasswordFieldWithToggle(openAiApiKeyField)

    private val panel: JPanel

    private val serviceProviders = listOf("ollama", "claude", "openai", "gemini", "groq")
    private val serviceProviderComboBox = ComboBox(serviceProviders.toTypedArray())

    private val languages = loadLanguagesFromFile("language_list.json")
    private val languageNames = languages.map { it.name }
    private val preferredLanguageComboBox = ComboBox(languageNames.toTypedArray())

    private val infoIconLabel = JLabel("ⓘ").apply {
        toolTipText = AICodeReviewBundle.message("plugin.settings.preferredLanguageNoteLabel")
        foreground = Color.GRAY // 원하는 색상으로 설정 가능
    }

    private val preferredLanguagePanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
        add(preferredLanguageComboBox)
        add(infoIconLabel)
    }

    init {
        serviceProviderComboBox.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                updateFieldsForSelectedProvider()
                updateFieldVisibility()
            }
        }

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(AICodeReviewBundle.message("plugin.settings.maxTokens"), maxTokensField)
            .addLabeledComponent(AICodeReviewBundle.message("plugin.settings.fileExtensions"), fileExtensionsField)
            .addLabeledComponent(AICodeReviewBundle.message("plugin.settings.serviceProvider"), serviceProviderComboBox)
            .addLabeledComponent(AICodeReviewBundle.message("plugin.settings.model"), modelField)
            .addLabeledComponent(ollamaEndpointLabel, ollamaEndpointField)
            .addLabeledComponent(claudeApiKeyLabel, claudePanel)
            .addLabeledComponent(geminiApiKeyLabel, geminiPanel)
            .addLabeledComponent(groqApiKeyLabel, groqPanel)
            .addLabeledComponent(openAiApiKeyLabel, openAiPanel)
            .addLabeledComponent(anthropicVersionLabel, anthropicVersionField)
            .addLabeledComponent(
                AICodeReviewBundle.message("plugin.settings.preferredLanguage"),
                preferredLanguagePanel
            )
            .addLabeledComponent(AICodeReviewBundle.message("plugin.settings.responsePath"), responsePathField)
            .addLabeledComponent(AICodeReviewBundle.message("plugin.settings.prompt"), promptScrollPane)
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
                responsePathField.text = AICodeReviewSettings.instance.CLAUDE_RESPONSE_PATH
            }

            "openai" -> {
                modelField.text = "gpt-4o-mini"
                responsePathField.text = AICodeReviewSettings.instance.OPENAI_RESPONSE_PATH
            }

            "gemini" -> {
                modelField.text = "gemini-1.5-flash"
                responsePathField.text = AICodeReviewSettings.instance.GEMINI_RESPONSE_PATH
            }

            "groq" -> {
                modelField.text = "gemma2-9b-it"
                responsePathField.text = AICodeReviewSettings.instance.GROQ_RESPONSE_PATH
            }

            "ollama" -> {
                modelField.text = "gemma2"
                responsePathField.text = AICodeReviewSettings.instance.OLLAMA_RESPONSE_PATH
            }
        }
    }

    private fun updateFieldVisibility() {
        val selectedProvider = serviceProviderComboBox.selectedItem as String
        val preferredLanguage = preferredLanguageComboBox.selectedItem as String
        val preferredLanguageCode = languages.find { it.name == preferredLanguage }?.code ?: "en"

        // UI 컴포넌트의 방향성을 언어에 맞게 설정
        applyComponentOrientationBasedOnLanguage(preferredLanguageCode, panel)

        ollamaEndpointLabel.isVisible = selectedProvider == "ollama"
        ollamaEndpointField.isVisible = selectedProvider == "ollama"

        claudeApiKeyLabel.isVisible = selectedProvider == "claude"
        claudePanel.isVisible = selectedProvider == "claude"

        geminiApiKeyLabel.isVisible = selectedProvider == "gemini"
        geminiPanel.isVisible = selectedProvider == "gemini"

        groqApiKeyLabel.isVisible = selectedProvider == "groq"
        groqPanel.isVisible = selectedProvider == "groq"

        openAiApiKeyLabel.isVisible = selectedProvider == "openai"
        openAiPanel.isVisible = selectedProvider == "openai"

        anthropicVersionLabel.isVisible = selectedProvider == "claude"
        anthropicVersionField.isVisible = selectedProvider == "claude"
    }

    private fun createPasswordFieldWithToggle(passwordField: JPasswordField): JPanel {
        val showHideButton = JButton(AICodeReviewBundle.message("plugin.settings.show")).apply {
            addActionListener {
                if (passwordField.echoCharIsSet()) {
                    passwordField.echoChar = '\u0000'
                    text = AICodeReviewBundle.message("plugin.settings.hide")
                } else {
                    passwordField.echoChar = '*'
                    text = AICodeReviewBundle.message("plugin.settings.show")
                }
            }
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(passwordField)
            add(showHideButton)
        }
    }

    private fun applyComponentOrientationBasedOnLanguage(languageCode: String, component: JComponent) {
        val isRtl = when (languageCode) {
            "ar", "he", "fa", "ur" -> true
            else -> false
        }
        component.componentOrientation =
            if (isRtl) ComponentOrientation.RIGHT_TO_LEFT else ComponentOrientation.LEFT_TO_RIGHT

        // RTL 적용을 위해 HTML 속성 추가
        val dirAttribute = if (isRtl) "rtl" else "ltr"
        component.putClientProperty("html", "<div dir=\"$dirAttribute\"></div>")
    }

    fun apply(settings: AICodeReviewSettings) {
        settings.OLLAMA_ENDPOINT = ollamaEndpointField.text
        settings.CLAUDE_API_KEY = String(claudeApiKeyField.password)
        settings.GEMINI_API_KEY = String(geminiApiKeyField.password)
        settings.GROQ_API_KEY = String(groqApiKeyField.password)
        settings.OPENAI_API_KEY = String(openAiApiKeyField.password)
        settings.MAX_TOKENS = maxTokensField.text.toIntOrNull() ?: 4096
        settings.FILE_EXTENSIONS = fileExtensionsField.text
        settings.SERVICE_PROVIDER = serviceProviderComboBox.selectedItem as String
        settings.MODEL = modelField.text
        settings.ANTHROPIC_VERSION = anthropicVersionField.text
        settings.PROMPT = promptField.text
        settings.PREFERRED_LANGUAGE = preferredLanguageComboBox.selectedItem as String
        settings.USE_STREAMING = true
        settings.STREAMING_CHUNK_SIZE = 8192


        // 서비스 제공자별 RESPONSE_PATH 설정 적용
        when (settings.SERVICE_PROVIDER) {
            "ollama" -> settings.OLLAMA_RESPONSE_PATH = responsePathField.text
            "claude" -> settings.CLAUDE_RESPONSE_PATH = responsePathField.text
            "gemini" -> settings.GEMINI_RESPONSE_PATH = responsePathField.text
            "groq" -> settings.GROQ_RESPONSE_PATH = responsePathField.text
            "openai" -> settings.OPENAI_RESPONSE_PATH = responsePathField.text
        }

        updateFieldVisibility()
    }

    fun reset(settings: AICodeReviewSettings) {
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

    fun isModified(settings: AICodeReviewSettings): Boolean {
        val isResponsePathModified = when (settings.SERVICE_PROVIDER) {
            "ollama" -> responsePathField.text != settings.OLLAMA_RESPONSE_PATH
            "claude" -> responsePathField.text != settings.CLAUDE_RESPONSE_PATH
            "gemini" -> responsePathField.text != settings.GEMINI_RESPONSE_PATH
            "groq" -> responsePathField.text != settings.GROQ_RESPONSE_PATH
            "openai" -> responsePathField.text != settings.OPENAI_RESPONSE_PATH
            else -> false
        }

        return ollamaEndpointField.text != settings.OLLAMA_ENDPOINT ||
                String(claudeApiKeyField.password) != settings.CLAUDE_API_KEY ||
                String(geminiApiKeyField.password) != settings.GEMINI_API_KEY ||
                String(groqApiKeyField.password) != settings.GROQ_API_KEY ||
                String(openAiApiKeyField.password) != settings.OPENAI_API_KEY ||
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