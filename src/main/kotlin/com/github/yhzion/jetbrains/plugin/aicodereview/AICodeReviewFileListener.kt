package com.github.yhzion.jetbrains.plugin.aicodereview

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener

class AICodeReviewFileListener(private val project: Project) : VirtualFileListener {
    override fun contentsChanged(event: VirtualFileEvent) {
        if (event.file.extension in AICodeReviewSettings.instance.FILE_EXTENSIONS.split("|")) {
            runCodeReview()
        }
    }

    private fun runCodeReview() {
        
    }
}