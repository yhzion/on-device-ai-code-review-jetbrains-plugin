package com.github.yhzion.jetbrains.plugin.deltareview.settings

import com.github.yhzion.jetbrains.plugin.deltareview.DeltaReviewSettings
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent
import com.github.yhzion.jetbrains.plugin.deltareview.DeltaReviewBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic

class DeltaReviewSettingsConfigurable : Configurable {
    private var mySettingsComponent: DeltaReviewSettingsComponent? = null

    override fun getDisplayName(): String = DeltaReviewBundle.message("plugin.settings.displayName")

    override fun createComponent(): JComponent {
        mySettingsComponent = DeltaReviewSettingsComponent()
        return mySettingsComponent!!.getPanel()
    }

    override fun isModified(): Boolean {
        val settings = DeltaReviewSettings.instance
        return mySettingsComponent!!.isModified(settings)
    }

    override fun apply() {
        val settings = DeltaReviewSettings.instance
        mySettingsComponent!!.apply(settings)

        // 설정 변경 이벤트 발생
        ApplicationManager.getApplication().messageBus.syncPublisher(SETTINGS_CHANGED_TOPIC).settingsChanged(settings)
    }

    override fun reset() {
        val settings = DeltaReviewSettings.instance
        mySettingsComponent!!.reset(settings)
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }

    companion object {
        val SETTINGS_CHANGED_TOPIC = Topic.create("DeltaReviewSettingsChanged", SettingsChangedListener::class.java)
    }

    interface SettingsChangedListener {
        fun settingsChanged(settings: DeltaReviewSettings)
    }
}