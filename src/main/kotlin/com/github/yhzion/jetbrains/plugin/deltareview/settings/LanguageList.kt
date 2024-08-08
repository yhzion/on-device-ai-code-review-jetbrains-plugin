package com.github.yhzion.jetbrains.plugin.deltareview.settings

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId

data class Language(val code: String, val name: String)

data class LanguageList(val languages: List<Language>)

fun loadLanguagesFromFile(filePath: String): List<Language> {
    val pluginId = PluginId.getId("com.github.yhzion.jetbrains.plugin.deltareview")
    val pluginClassLoader = PluginManager.getPlugin(pluginId)?.pluginClassLoader
        ?: throw IllegalStateException("Plugin classloader not found")

    val inputStream = pluginClassLoader.getResourceAsStream(filePath)
        ?: throw IllegalArgumentException("Language file not found: $filePath")

    val jsonContent = inputStream.bufferedReader().use { it.readText() }
    val languageList = Gson().fromJson<LanguageList>(jsonContent, object : TypeToken<LanguageList>() {}.type)
    return languageList.languages
}