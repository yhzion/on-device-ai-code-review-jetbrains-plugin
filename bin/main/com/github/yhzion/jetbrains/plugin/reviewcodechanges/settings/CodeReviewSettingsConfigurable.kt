package com.github.yhzion.jetbrains.plugin.reviewcodechanges.settings

import com.github.yhzion.jetbrains.plugin.reviewcodechanges.CodeReviewSettings
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class CodeReviewSettingsConfigurable : Configurable {
    private var mySettingsComponent: CodeReviewSettingsComponent? = null

    override fun getDisplayName(): String = "Review Code Changes"

    override fun createComponent(): JComponent {
        mySettingsComponent = CodeReviewSettingsComponent()
        return mySettingsComponent!!.getPanel()
    }

    override fun isModified(): Boolean {
        val settings = CodeReviewSettings.instance
        return mySettingsComponent!!.isModified(settings)
    }

    override fun apply() {
        val settings = CodeReviewSettings.instance
        mySettingsComponent!!.apply(settings)
    }

    override fun reset() {
        val settings = CodeReviewSettings.instance
        mySettingsComponent!!.reset(settings)
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}