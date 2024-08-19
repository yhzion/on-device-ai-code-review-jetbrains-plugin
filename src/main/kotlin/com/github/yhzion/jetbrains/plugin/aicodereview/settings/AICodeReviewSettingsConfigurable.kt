package com.github.yhzion.jetbrains.plugin.aicodereview.settings

import com.github.yhzion.jetbrains.plugin.aicodereview.AICodeReviewSettings
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent
import com.github.yhzion.jetbrains.plugin.aicodereview.AICodeReviewBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic

class AICodeReviewSettingsConfigurable : Configurable {
    private var mySettingsComponent: AICodeReviewSettingsComponent? = null

    override fun getDisplayName(): String = AICodeReviewBundle.message("plugin.settings.displayName")

    override fun createComponent(): JComponent {
        mySettingsComponent = AICodeReviewSettingsComponent()
        return mySettingsComponent!!.getPanel()
    }

    override fun isModified(): Boolean {
        val settings = AICodeReviewSettings.instance
        return mySettingsComponent!!.isModified(settings)
    }

    override fun apply() {
        val settings = AICodeReviewSettings.instance
        mySettingsComponent!!.apply(settings)
        ApplicationManager.getApplication().messageBus.syncPublisher(SETTINGS_CHANGED_TOPIC).settingsChanged(settings)
    }

    override fun reset() {
        val settings = AICodeReviewSettings.instance
        mySettingsComponent!!.reset(settings)
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }

    companion object {
        val SETTINGS_CHANGED_TOPIC = Topic.create("AICodeReviewSettingsChanged", SettingsChangedListener::class.java)
    }

    interface SettingsChangedListener {
        fun settingsChanged(settings: AICodeReviewSettings)
    }
}