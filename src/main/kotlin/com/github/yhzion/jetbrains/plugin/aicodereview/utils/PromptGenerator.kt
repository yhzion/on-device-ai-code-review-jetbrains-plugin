package com.github.yhzion.jetbrains.plugin.aicodereview.utils

import com.github.yhzion.jetbrains.plugin.aicodereview.AICodeReviewSettings

object PromptGenerator {
    fun generatePrompt(fileName: String, fullContent: String, changedContent: String, settings: AICodeReviewSettings): String {
        val content = settings.PROMPT
        val preferredLanguage = settings.PREFERRED_LANGUAGE
        val prompt = "Review the following code snippet in $preferredLanguage: $fileName"
        return "$prompt\n$content\n\nFull file content:\n$fullContent\n\nChanged content:\n$changedContent"
    }
}
