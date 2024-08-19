package com.github.yhzion.jetbrains.plugin.aicodereview.utils

import org.json.JSONObject

object JsonUtils {

    fun extractContentFromJson(json: JSONObject, path: String): String {
        var current: Any = json
        val keys = path.split('.').filter { it.isNotEmpty() }

        for (key in keys) {
            if (current is JSONObject) {
                current = if (key.endsWith("]")) {
                    val arrayKey = key.substringBefore('[')
                    val index = key.substringAfter('[').substringBefore(']').toInt()
                    current.getJSONArray(arrayKey).get(index)
                } else {
                    current.get(key)
                }
            } else {
                throw IllegalArgumentException("Unexpected JSON structure for path: $path")
            }
        }

        return current.toString()
    }
}
