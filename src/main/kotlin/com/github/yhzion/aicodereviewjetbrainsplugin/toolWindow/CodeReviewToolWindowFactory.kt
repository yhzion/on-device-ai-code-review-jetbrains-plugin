package com.github.yhzion.aicodereviewjetbrainsplugin.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class CodeReviewToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = CodeReviewToolWindowContent(toolWindow)
        val content = ContentFactory.getInstance().createContent(toolWindowContent.content, "", false)
        toolWindow.contentManager.addContent(content)
    }
}