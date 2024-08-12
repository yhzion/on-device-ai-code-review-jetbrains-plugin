package com.github.yhzion.jetbrains.plugin.aicodereview.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class AICodeReviewToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val reviewToolWindow = AICodeReviewToolWindow(project, toolWindow)
        val content = ContentFactory.getInstance().createContent(reviewToolWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}