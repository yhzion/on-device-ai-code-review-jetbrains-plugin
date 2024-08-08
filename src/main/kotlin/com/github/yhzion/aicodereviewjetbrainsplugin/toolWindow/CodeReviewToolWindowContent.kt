package com.github.yhzion.aicodereviewjetbrainsplugin.toolWindow

import com.intellij.openapi.wm.ToolWindow
import javax.swing.JComponent
import javax.swing.JPanel

class CodeReviewToolWindowContent(toolWindow: ToolWindow) {
    private val myToolWindowContent: JPanel = JPanel()

    init {
        // TODO: Add components to display code review results
    }

    val content: JComponent
        get() = myToolWindowContent
}