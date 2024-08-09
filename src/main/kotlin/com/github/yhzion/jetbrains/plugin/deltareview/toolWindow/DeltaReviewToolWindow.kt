package com.github.yhzion.jetbrains.plugin.deltareview.toolWindow

import com.github.yhzion.jetbrains.plugin.deltareview.services.DeltaReviewService
import com.github.yhzion.jetbrains.plugin.deltareview.services.FileReviewResult
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import org.intellij.plugins.markdown.ui.preview.jcef.MarkdownJCEFHtmlPanel
import kotlinx.coroutines.*
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel

import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel

class DeltaReviewToolWindow(private val project: Project, toolWindow: ToolWindow) {
    private val mainPanel: JPanel = JPanel(BorderLayout())
    private val markdownPanel: MarkdownHtmlPanel = MarkdownJCEFHtmlPanel(project, null)
    private val runReviewButton: JButton = JButton("Request a review")
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    init {
        mainPanel.add(markdownPanel.component, BorderLayout.CENTER)
        mainPanel.add(runReviewButton, BorderLayout.NORTH)

        runReviewButton.addActionListener {
            runCodeReview()
        }
    }

    private fun runCodeReview() {
        val reviewService = DeltaReviewService(project)

        scope.launch {
            try {
                val results = reviewService.reviewChangedFiles { progress ->
                    withContext(Dispatchers.Main) {
                        appendReviewResult(progress)
                    }
                }
                withContext(Dispatchers.Main) {
                    appendReviewResult("Review Results:\n")
                    appendReviewResult(formatResults(results))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendReviewResult("Error occurred: ${e.message}\n")
                }
            }
        }
    }

    private fun formatResults(results: List<FileReviewResult>): String {
        return results.joinToString("\n\n") { result ->
            "### File: ${result.fileName}\n${result.review}"
        }
    }

    private fun appendReviewResult(text: String) {
        val currentContent = getCurrentContent()

        // 마크다운 텍스트 출력
        println("Markdown to append:\n$text")

        // 마크다운을 HTML로 변환
        val markdownToHtml = markdownToHtml(text)
        println("Converted HTML:\n$markdownToHtml")

        // 두 번째 인자로는 0을 전달하거나, 필요에 따라 인덱스를 지정합니다.
        val newContent = currentContent + "\n" + markdownToHtml
        markdownPanel.setHtml(newContent, 0)

        // 변환된 HTML을 출력
        println("Updated content:\n$newContent")
    }

    fun markdownToHtml(markdown: String): String {
        // Flexmark 설정
        val options = MutableDataSet()
        val parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()

        // 마크다운을 파싱해서 HTML로 변환
        val document = parser.parse(markdown)
        return renderer.render(document)
    }

    fun setReviewResult(result: String) {
        // 두 번째 인자로는 0을 전달하거나, 필요에 따라 인덱스를 지정합니다.
        markdownPanel.setHtml(result, 0)

        // 디버그를 위한 로그 출력
        println("Setting review result with content:\n$result")
    }

    fun getCurrentContent(): String {
        // 실제로 내용을 가져오는 메서드가 무엇인지 확인하고 작성
        // 이 예시에서는 패널의 HTML 내용을 가져온다고 가정합니다.
        val currentContent = markdownPanel.component.toolTipText ?: ""  // 예시로 마크다운 패널의 도구 설명 텍스트를 가져옴
        println("Current content fetched:\n$currentContent")  // 디버그를 위한 로그 출력
        return currentContent
    }

    fun getContent() = mainPanel
}