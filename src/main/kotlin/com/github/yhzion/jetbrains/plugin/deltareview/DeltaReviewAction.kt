package com.github.yhzion.jetbrains.plugin.deltareview

import com.github.yhzion.jetbrains.plugin.deltareview.services.CodeReviewService
import com.github.yhzion.jetbrains.plugin.deltareview.services.FileReviewResult
import com.github.yhzion.jetbrains.plugin.deltareview.toolWindow.DeltaReviewToolWindow
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.ToolWindowManager
import kotlinx.coroutines.runBlocking

class DeltaReviewAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val reviewService = CodeReviewService(project)

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Delta review")

        runBlocking {
            val results = reviewService.reviewChangedFiles { progress ->
                ApplicationManager.getApplication().invokeLater {
                    toolWindow?.let {
                        val content = it.contentManager.getContent(0)
                        val reviewToolWindow = content?.component as? DeltaReviewToolWindow
                        reviewToolWindow?.setReviewResult("$progress\n${reviewToolWindow.getCurrentContent()}")
                    }
                }
            }

            ApplicationManager.getApplication().invokeLater {
                toolWindow?.let {
                    it.show {
                        val content = it.contentManager.getContent(0)
                        val reviewToolWindow = content?.component as? DeltaReviewToolWindow
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