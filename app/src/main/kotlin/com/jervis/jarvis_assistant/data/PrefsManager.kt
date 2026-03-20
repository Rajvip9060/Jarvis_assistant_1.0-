package com.jervis.jarvis_assistant.data

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "openrouter_api_key"
    }

    // API Key ko phone memory mein save karne ke liye
    fun saveApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    // Saved API Key ko wapas nikalne ke liye
    fun getApiKey(): String {
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }
}