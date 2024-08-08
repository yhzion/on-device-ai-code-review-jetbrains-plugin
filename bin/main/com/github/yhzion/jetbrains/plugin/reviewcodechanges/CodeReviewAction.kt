package com.github.yhzion.jetbrains.plugin.reviewcodechanges

import com.github.yhzion.jetbrains.plugin.reviewcodechanges.services.CodeReviewService
import com.github.yhzion.jetbrains.plugin.reviewcodechanges.services.FileReviewResult
import com.github.yhzion.jetbrains.plugin.reviewcodechanges.toolWindow.AICodeReviewToolWindow
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.ToolWindowManager
import kotlinx.coroutines.runBlocking

class CodeReviewAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val reviewService = CodeReviewService(project)

        // Get the AI Code Review tool window
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("AI Code Review")

        runBlocking {
            val results = reviewService.reviewChangedFiles { progress ->
                // 진행 상황을 Tool Window에 업데이트
                ApplicationManager.getApplication().invokeLater {
                    toolWindow?.let {
                        val content = it.contentManager.getContent(0)
                        val reviewToolWindow = content?.component as? AICodeReviewToolWindow
                        reviewToolWindow?.setReviewResult("$progress\n${reviewToolWindow.getCurrentContent()}")
                    }
                }
            }

            // 최종 결과를 Tool Window에 표시
            ApplicationManager.getApplication().invokeLater {
                toolWindow?.let {
                    it.show {
                        val content = it.contentManager.getContent(0)
                        val reviewToolWindow = content?.component as? AICodeReviewToolWindow
                        reviewToolWindow?.setReviewResult(formatResults(results))
                    }
                }
            }
        }
    }

    private fun formatResults(results: List<FileReviewResult>): String {
        return results.joinToString("\n\n") { result ->
            "File: ${result.fileName}\n${result.review}"
        }
    }
}