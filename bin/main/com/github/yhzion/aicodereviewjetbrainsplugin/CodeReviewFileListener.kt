package com.github.yhzion.aicodereviewjetbrainsplugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener

class CodeReviewFileListener(private val project: Project) : VirtualFileListener {
    override fun contentsChanged(event: VirtualFileEvent) {
        if (event.file.extension in CodeReviewSettings.instance.FILE_EXTENSIONS.split("|")) {
            runCodeReview()
        }
    }

    private fun runCodeReview() {
        // CodeReviewAction의 로직을 여기에 구현
    }
}