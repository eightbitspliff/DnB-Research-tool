package com.dnbresearch.app.data

import android.content.Context
import com.dnbresearch.app.BuildConfig

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_YOUTUBE_API_KEY
        set(value) = prefs.edit().putString(KEY_API, value.trim()).apply()

    var days: Long
        get() = prefs.getLong(KEY_DAYS, 14L)
        set(value) = prefs.edit().putLong(KEY_DAYS, value).apply()

    var hideMixes: Boolean
        get() = prefs.getBoolean(KEY_HIDE_MIXES, true)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_MIXES, value).apply()

    var officialOnly: Boolean
        get() = prefs.getBoolean(KEY_OFFICIAL_ONLY, false)
        set(value) = prefs.edit().putBoolean(KEY_OFFICIAL_ONLY, value).apply()

    private companion object {
        const val KEY_API = "youtube_api_key"
        const val KEY_DAYS = "days"
        const val KEY_HIDE_MIXES = "hide_mixes"
        const val KEY_OFFICIAL_ONLY = "official_only"
    }
}
