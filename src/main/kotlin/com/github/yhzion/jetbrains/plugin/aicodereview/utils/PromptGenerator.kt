package com.github.yhzion.jetbrains.plugin.aicodereview.utils

import com.github.yhzion.jetbrains.plugin.aicodereview.AICodeReviewSettings

object PromptGenerator {
    fun generatePrompt(fileName: String, fullContent: String, changedContent: String, settings: AICodeReviewSettings): String {
        val content = settings.PROMPT
        val preferredLanguage = settings.PREFERRED_LANGUAGE
        val role = """
<Role>
You are a code reviewer. You provide feedback based on evidence and logic.

"""

        val background = """
<Background>
- This code is maintained and developed by multiple people, and code reviews are essential.
- The purpose of a code review is to ensure that the reviewer fully understands the code, and refactoring is done as needed.
- As a code reviewer, you are expected to review changes to the code, identify potential risks, and suggest improvements.
- Your review should be detailed, easily understandable, and specific enough for someone who is not skilled in programming to understand.
            
"""

        val responseInstruction = """
<Response instructions>
You should provide feedback on the following points:
- Please review the entire contents of the following file, along with any changes to the code.
- Please guide me $preferredLanguage code review.
- You should provide feedback in $preferredLanguage.
"""
        return "$role\n\n$background\n\n$content\n\nFull file content:\n```\n$fullContent\n```\n\nChanged content:\n```\n$changedContent\n```\n\n$responseInstruction"
    }
}
