import com.github.yhzion.jetbrains.plugin.deltareview.DeltaReviewBundle
import com.github.yhzion.jetbrains.plugin.deltareview.DeltaReviewSettings
import com.github.yhzion.jetbrains.plugin.deltareview.services.DeltaReviewService
import com.github.yhzion.jetbrains.plugin.deltareview.services.FileReviewResult
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import org.intellij.plugins.markdown.ui.preview.jcef.MarkdownJCEFHtmlPanel
import kotlinx.coroutines.*
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel
import com.intellij.ui.JBColor
import java.awt.*
import javax.swing.*

class DeltaReviewToolWindow(private val project: Project, toolWindow: ToolWindow) {
    private val mainPanel: JPanel = JPanel(BorderLayout())
    private val markdownPanel: MarkdownHtmlPanel = MarkdownJCEFHtmlPanel(project, null)
    private val runReviewButton: JButton = JButton(DeltaReviewBundle.message("plugin.review.runReviewButton"))
    private val cancelButton: JButton = JButton(DeltaReviewBundle.message("plugin.review.cancelButton"))
    private val progressBar: JProgressBar = JProgressBar()
    private val providerLabel: JLabel = JLabel()
    private val modelLabel: JLabel = JLabel()
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    init {
        providerLabel.text = DeltaReviewBundle.message("plugin.review.serviceProvider") + ": ${DeltaReviewSettings.instance.SERVICE_PROVIDER}"
        modelLabel.text = DeltaReviewBundle.message("plugin.review.model") + ": ${DeltaReviewSettings.instance.MODEL}"
        providerLabel.font = providerLabel.font.deriveFont(Font.PLAIN, 14f)
        modelLabel.font = modelLabel.font.deriveFont(Font.PLAIN, 14f)
        providerLabel.foreground = JBColor.foreground()  // 글씨 색상을 테마에 맞게 변경
        modelLabel.foreground = JBColor.foreground()  // 글씨 색상을 테마에 맞게 변경

        // 패널 설정
        val infoPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(providerLabel)
            add(Box.createVerticalStrut(5))  // 간격 추가
            add(modelLabel)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border(), 0, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            )
            background = JBColor.background() // 배경색을 리뷰 결과 영역과 동일하게 설정
        }

        // 탭 패널로 정보 패널 구성
        val tabbedPane = JTabbedPane().apply {
            addTab(DeltaReviewBundle.message("plugin.review.informationTab"), infoPanel)
        }

        progressBar.isIndeterminate = true
        progressBar.isVisible = false
        cancelButton.isEnabled = false

        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(runReviewButton)
            add(cancelButton)
            add(Box.createHorizontalGlue())
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0) // 패딩 추가
        }

        val buttonAndProgressPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(buttonPanel)
            add(progressBar)
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0) // 패딩 추가
        }

        val southPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(tabbedPane)
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0) // 패딩 추가
        }

        mainPanel.add(markdownPanel.component, BorderLayout.CENTER)
        mainPanel.add(buttonAndProgressPanel, BorderLayout.NORTH)
        mainPanel.add(southPanel, BorderLayout.SOUTH)

        markdownPanel.component.isVisible = true

        runReviewButton.addActionListener {
            runReviewButton.isEnabled = false
            cancelButton.isEnabled = true
            progressBar.isVisible = true
            runCodeReview()
        }

        cancelButton.addActionListener {
            scope.coroutineContext.cancelChildren()
            appendReviewResult(DeltaReviewBundle.message("plugin.review.cancelled") + "\n")
            runReviewButton.isEnabled = true
            cancelButton.isEnabled = false
            progressBar.isVisible = false
        }
    }

    private fun runCodeReview() {
        scope.launch {
            try {
                val reviewService = DeltaReviewService(project)
                val results = reviewService.reviewChangedFiles { progress ->
                    withContext(Dispatchers.Main) {
                        appendReviewResult(DeltaReviewBundle.message("plugin.review.reviewingFile", progress))
                    }
                }
                withContext(Dispatchers.Main) {
                    appendReviewResult(DeltaReviewBundle.message("plugin.review.results") + ":\n")
                    appendReviewResult(formatResults(results))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendReviewResult(DeltaReviewBundle.message("plugin.review.errorOccurred", e.message ?: "") + "\n")
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

    private fun formatResults(results: List<FileReviewResult>): String {
        return results.joinToString("\n\n") { result ->
            DeltaReviewBundle.message("plugin.review.file") + ": ${result.fileName}\n${result.review}"
        }
    }

    private fun appendReviewResult(text: String) {
        val currentContent = getCurrentContent()
        val markdownToHtml = markdownToHtml(text)

        ApplicationManager.getApplication().invokeLater {
            val newContent = currentContent + "\n" + applyTextDirection(markdownToHtml)
            markdownPanel.setHtml(newContent, 0)
        }
    }

    private fun applyTextDirection(html: String): String {
        val languageCode = when (DeltaReviewSettings.instance.PREFERRED_LANGUAGE) {
            "Arabic" -> "ar"
            "Hebrew" -> "he"
            "Persian" -> "fa"
            "Urdu" -> "ur"
            else -> "en"
        }

        val isRtl = listOf("ar", "he", "fa", "ur").contains(languageCode)
        return if (isRtl) {
            "<div dir=\"rtl\" style=\"text-align: right;\">$html</div>"
        } else {
            "<div dir=\"ltr\" style=\"text-align: left;\">$html</div>"
        }
    }

    private fun markdownToHtml(markdown: String): String {
        val options = MutableDataSet()
        val parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()
        val document = parser.parse(markdown)
        return renderer.render(document)
    }

    fun setReviewResult(result: String) {
        ApplicationManager.getApplication().invokeLater {
            markdownPanel.setHtml(applyTextDirection(result), 0)
        }
    }

    fun getCurrentContent(): String {
        return markdownPanel.component.toolTipText ?: ""
    }

    fun getContent() = mainPanel
}