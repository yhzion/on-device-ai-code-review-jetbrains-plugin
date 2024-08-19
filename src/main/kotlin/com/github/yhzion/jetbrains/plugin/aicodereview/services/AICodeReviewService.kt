package com.github.yhzion.jetbrains.plugin.aicodereview.services

import com.github.yhzion.jetbrains.plugin.aicodereview.*
import com.github.yhzion.jetbrains.plugin.aicodereview.utils.*
import com.intellij.notification.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.*
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.*

class AICodeReviewService(private val project: Project) {

    companion object {
        private const val TIMEOUT = 300L
    }

    private val settings = AICodeReviewSettings.instance

    suspend fun reviewChangedFiles(progressCallback: suspend (String) -> Unit): List<FileReviewResult> = withContext(Dispatchers.IO) {
        val changedFiles = getChangedFiles()
        progressCallback("Found ${changedFiles.size} changed files\n")
        changedFiles.forEach { file ->
            progressCallback("- ${file.name}\n")
        }

        progressCallback("\n\n")

        val reviewedFiles = changedFiles.mapNotNull { file ->
            val fullContent = file.contentsToByteArray().toString(Charsets.UTF_8)
            val changedContent = getChangedContent(file)
            if (changedContent.isNotEmpty()) {
                val filename = file.name
                progressCallback("\n\n# $filename\n\n")
                val review = ClientUtils.requestReview(file.name, fullContent, changedContent, settings, progressCallback)
                FileReviewResult(file.name, "\n\n\n" + review)
            } else {
                progressCallback("# No significant changes in ${file.name}\n")
                null
            }
        }

        // Notification 추가
        val reviewedCount = reviewedFiles.size
        val notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("AI Code Review Notifications")
        val notification = notificationGroup.createNotification(
            "AI code review completed",
            "$reviewedCount files have been reviewed.",
            NotificationType.INFORMATION
        )
        Notifications.Bus.notify(notification)

        return@withContext reviewedFiles
    }

    private fun getChangedFiles(): List<VirtualFile> {
        val changeListManager = ChangeListManager.getInstance(project)
        val regex = Regex(settings.FILE_EXTENSIONS)

        return changeListManager.affectedFiles.filter { file ->
            regex.containsMatchIn(file.name)
        }
    }

    private fun getChangedContent(file: VirtualFile): String {
        val changeListManager = ChangeListManager.getInstance(project)
        val change: Change? = changeListManager.getChange(file)

        return when (val contentRevision = change?.afterRevision) {
            is ContentRevision -> contentRevision.content ?: ""
            else -> ""
        }
    }


}

data class FileReviewResult(val fileName: String, val review: String)
