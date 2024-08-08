package com.github.yhzion.jetbrains.plugin.reviewcodechanges.settings

import com.github.yhzion.jetbrains.plugin.reviewcodechanges.CodeReviewSettings
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import javax.swing.*
import java.awt.event.ItemEvent

class CodeReviewSettingsComponent {
    private val endpointField = JBTextField()
    private val maxTokensField = JBTextField()
    private val fileExtensionsField = JBTextField()
    private val modelField = JBTextField()
    private val apiKeyField = JBTextField()
    private val responsePathField = JBTextField()
    private val anthropicVersionField = JBTextField()
    private val promptField = JTextArea(5, 50).apply {
        lineWrap = true
        wrapStyleWord = true
    }
    private val promptScrollPane = JBScrollPane(promptField)

    private val apiKeyLabel = JLabel("API Key")
    private val anthropicVersionLabel = JLabel("ANTHROPIC_VERSION")

    private val panel: JPanel

    private val serviceProviders = listOf("claude", "chatgpt", "gemini", "groq", "ollama")
    private val serviceProviderComboBox = JComboBox(serviceProviders.toTypedArray())

    private val presets = mapOf(
        "claude" to Preset(
            "https://api.anthropic.com/v1/messages",
            "claude-3-5-sonnet-20240620",
            ".content[0].text",
            "2023-06-01"
        ),
        "chatgpt" to Preset(
            "https://api.openai.com/v1/chat/completions",
            "gpt-4o-mini",
            ".choices[0].message.content"
        ),
        "gemini" to Preset(
            "https://generativelanguage.googleapis.com/v1/models/\${MODEL}:generateContent?key=\${API_KEY}",
            "gemini-1.5-flash",
            ".candidates[0].content.parts[0].text"
        ),
        "groq" to Preset(
            "https://api.groq.com/openai/v1/chat/completions",
            "llama-3.1-8b-instant",
            ".choices[0].message.content"
        ),
        "ollama" to Preset(
            "http://localhost:11434/api/chat",
            "gemma2",
            ".message.content"
        )
    )


    init {
        serviceProviderComboBox.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                updateFieldsFromPreset()
            }
        }

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Endpoint", endpointField)
            .addLabeledComponent("Max Tokens", maxTokensField)
            .addLabeledComponent("File Extensions", fileExtensionsField)
            .addLabeledComponent("Service Provider", serviceProviderComboBox)
            .addLabeledComponent("Model", modelField)
            .addLabeledComponent(apiKeyLabel, apiKeyField)
            .addLabeledComponent(anthropicVersionLabel, anthropicVersionField)
            .addLabeledComponent("Prompt", promptScrollPane)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        updateFieldVisibility()
    }

    private fun updateFieldsFromPreset() {
        val selectedProvider = serviceProviderComboBox.selectedItem as String
        val preset = presets[selectedProvider]
        preset?.let {
            endpointField.text = it.endpoint
            modelField.text = it.model
            responsePathField.text = it.responsePath
            anthropicVersionField.text = it.anthropicVersion ?: ""
        }
        updateFieldVisibility()
    }

    private fun updateFieldVisibility() {
        val selectedProvider = serviceProviderComboBox.selectedItem as String
        apiKeyLabel.isVisible = selectedProvider != "ollama"
        apiKeyField.isVisible = selectedProvider != "ollama"
        anthropicVersionLabel.isVisible = selectedProvider == "claude"
        anthropicVersionField.isVisible = selectedProvider == "claude"
    }

    fun apply(settings: CodeReviewSettings) {
        settings.ENDPOINT = endpointField.text
        settings.MAX_TOKENS = maxTokensField.text.toIntOrNull() ?: 4096
        settings.FILE_EXTENSIONS = fileExtensionsField.text
        settings.SERVICE_PROVIDER = serviceProviderComboBox.selectedItem as String
        settings.MODEL = modelField.text
        settings.API_KEY = apiKeyField.text
        settings.ANTHROPIC_VERSION = anthropicVersionField.text
        settings.PROMPT = promptField.text
    }

    fun reset(settings: CodeReviewSettings) {
        endpointField.text = settings.ENDPOINT
        maxTokensField.text = settings.MAX_TOKENS.toString()
        fileExtensionsField.text = settings.FILE_EXTENSIONS
        serviceProviderComboBox.selectedItem = settings.SERVICE_PROVIDER
        modelField.text = settings.MODEL
        apiKeyField.text = settings.API_KEY
        anthropicVersionField.text = settings.ANTHROPIC_VERSION
        promptField.text = settings.PROMPT
        updateFieldVisibility()
    }

    fun isModified(settings: CodeReviewSettings): Boolean {
        return endpointField.text != settings.ENDPOINT ||
                maxTokensField.text.toIntOrNull() != settings.MAX_TOKENS ||
                fileExtensionsField.text != settings.FILE_EXTENSIONS ||
                serviceProviderComboBox.selectedItem != settings.SERVICE_PROVIDER ||
                modelField.text != settings.MODEL ||
                apiKeyField.text != settings.API_KEY ||
                anthropicVersionField.text != settings.ANTHROPIC_VERSION ||
                promptField.text != settings.PROMPT
    }

    fun getPanel(): JPanel = panel
}

data class Preset(
    val endpoint: String,
    val model: String,
    val responsePath: String,
    val anthropicVersion: String? = null
)