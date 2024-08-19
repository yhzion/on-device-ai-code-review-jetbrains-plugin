package com.github.yhzion.jetbrains.plugin.aicodereview.toolWindow

import com.github.yhzion.jetbrains.plugin.aicodereview.AICodeReviewBundle
import com.github.yhzion.jetbrains.plugin.aicodereview.AICodeReviewSettings
import com.github.yhzion.jetbrains.plugin.aicodereview.services.AICodeReviewService
import com.github.yhzion.jetbrains.plugin.aicodereview.settings.AICodeReviewSettingsConfigurable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import org.intellij.plugins.markdown.ui.preview.jcef.MarkdownJCEFHtmlPanel
import kotlinx.coroutines.*
import com.intellij.ui.JBColor
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel
import java.awt.*
import javax.swing.*

class AICodeReviewToolWindow(private val project: Project, toolWindow: ToolWindow) {
    private val mainPanel: JPanel = JPanel(BorderLayout())
    private val markdownPanel: MarkdownHtmlPanel = MarkdownJCEFHtmlPanel(project, null)
    private val runReviewButton: JButton = JButton(AICodeReviewBundle.message("plugin.review.runReviewButton"))
    private val cancelButton: JButton = JButton(AICodeReviewBundle.message("plugin.review.cancelButton"))
    private val progressBar: JProgressBar = JProgressBar()
    private val providerLabel: JLabel = JLabel()
    private val modelLabel: JLabel = JLabel()
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val contentBuilder = StringBuilder() // 실시간으로 데이터를 축적하기 위한 StringBuilder

    init {
        updateInfoLabels()
        providerLabel.font = providerLabel.font.deriveFont(Font.PLAIN, 14f)
        modelLabel.font = modelLabel.font.deriveFont(Font.PLAIN, 14f)
        providerLabel.foreground = JBColor.foreground()
        modelLabel.foreground = JBColor.foreground()

        val infoPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(providerLabel)
            add(Box.createVerticalStrut(5))
            add(modelLabel)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border(), 0, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            )
            background = JBColor.background()
        }

        val tabbedPane = JTabbedPane().apply {
            addTab(AICodeReviewBundle.message("plugin.review.informationTab"), infoPanel)
        }

        progressBar.isIndeterminate = true
        progressBar.isVisible = false
        cancelButton.isEnabled = false

        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(runReviewButton)
            add(cancelButton)
            add(Box.createHorizontalGlue())
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        }

        val buttonAndProgressPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(buttonPanel)
            add(progressBar)
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        }

        val southPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(tabbedPane)
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        }

        mainPanel.add(markdownPanel.component, BorderLayout.CENTER)
        mainPanel.add(buttonAndProgressPanel, BorderLayout.NORTH)
        mainPanel.add(southPanel, BorderLayout.SOUTH)

        markdownPanel.component.isVisible = true

        runReviewButton.addActionListener {
            runReviewButton.isEnabled = false
            cancelButton.isEnabled = true
            progressBar.isVisible = true
            contentBuilder.clear() // 시작할 때 기존 내용을 초기화
            runCodeReview()
        }

        cancelButton.addActionListener {
            scope.coroutineContext.cancelChildren()
            appendReviewResult(AICodeReviewBundle.message("plugin.review.cancelled") + "\n")
            runReviewButton.isEnabled = true
            cancelButton.isEnabled = false
            progressBar.isVisible = false
        }

        // 설정 변경 리스너 등록
        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(AICodeReviewSettingsConfigurable.SETTINGS_CHANGED_TOPIC, object : AICodeReviewSettingsConfigurable.SettingsChangedListener {
                override fun settingsChanged(settings: AICodeReviewSettings) {
                    updateInfoLabels()
                }
            })
    }

    private fun updateInfoLabels() {
        val settings = AICodeReviewSettings.instance
        providerLabel.text = AICodeReviewBundle.message("plugin.review.serviceProvider") + ": ${settings.SERVICE_PROVIDER}"
        modelLabel.text = AICodeReviewBundle.message("plugin.review.model") + ": ${settings.MODEL}"
    }

    private fun runCodeReview() {
        scope.launch {
            try {
                val reviewService = AICodeReviewService(project)
                reviewService.reviewChangedFiles { progress ->
                    withContext(Dispatchers.Main) {
                        appendReviewResult(progress)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendReviewResult(AICodeReviewBundle.message("plugin.review.errorOccurred", e.message ?: "") + "\n")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    runReviewButton.isEnabled = true
                    cancelButton.isEnabled = false
                    progressBar.isVisible = false
                }
            }
        }
    }

    public fun appendReviewResult(text: String) {
        contentBuilder.append(text) // 새로운 청크를 기존 텍스트에 추가
        val htmlContent = markdownToHtml(contentBuilder.toString()) // 전체를 다시 HTML로 변환

        ApplicationManager.getApplication().invokeLater {
            markdownPanel.setHtml(htmlContent, 0) // UI에 적용
        }
    }

    private fun markdownToHtml(markdown: String): String {
        val options = MutableDataSet()
        val parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()
        val document = parser.parse(markdown)
        return renderer.render(document)
    }

    fun getContent() = mainPanel
}
