package com.github.yhzion.jetbrains.plugin.deltareview.toolWindow

import DeltaReviewToolWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class DeltaReviewToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val reviewToolWindow = DeltaReviewToolWindow(project, toolWindow)
        val content = ContentFactory.getInstance().createContent(reviewToolWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}