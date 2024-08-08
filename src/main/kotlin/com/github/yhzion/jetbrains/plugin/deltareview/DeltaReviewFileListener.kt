package com.github.yhzion.jetbrains.plugin.deltareview

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener

class DeltaReviewFileListener(private val project: Project) : VirtualFileListener {
    override fun contentsChanged(event: VirtualFileEvent) {
        if (event.file.extension in DeltaReviewSettings.instance.FILE_EXTENSIONS.split("|")) {
            runCodeReview()
        }
    }

    private fun runCodeReview() {
        // CodeReviewAction의 로직을 여기에 구현
    }
}