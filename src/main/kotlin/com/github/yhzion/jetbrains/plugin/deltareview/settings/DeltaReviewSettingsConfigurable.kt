package com.github.yhzion.jetbrains.plugin.deltareview.settings

import com.github.yhzion.jetbrains.plugin.deltareview.DeltaReviewSettings
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class DeltaReviewSettingsConfigurable : Configurable {
    private var mySettingsComponent: CodeReviewSettingsComponent? = null

    override fun getDisplayName(): String = "Review Code Changes"

    override fun createComponent(): JComponent {
        mySettingsComponent = CodeReviewSettingsComponent()
        return mySettingsComponent!!.getPanel()
    }

    override fun isModified(): Boolean {
        val settings = DeltaReviewSettings.instance
        return mySettingsComponent!!.isModified(settings)
    }

    override fun apply() {
        val settings = DeltaReviewSettings.instance
        mySettingsComponent!!.apply(settings)
    }

    override fun reset() {
        val settings = DeltaReviewSettings.instance
        mySettingsComponent!!.reset(settings)
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}