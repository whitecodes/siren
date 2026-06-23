package com.siren.player.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

enum class LanguageMode {
    CHINESE, ENGLISH, SYSTEM
}

object LanguageManager {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE_MODE = "language_mode"

    private lateinit var prefs: SharedPreferences
    private val _languageMode = MutableStateFlow(LanguageMode.SYSTEM)
    val languageMode: StateFlow<LanguageMode> = _languageMode

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedMode = prefs.getString(KEY_LANGUAGE_MODE, null)
        _languageMode.value = when (savedMode) {
            "CHINESE" -> LanguageMode.CHINESE
            "ENGLISH" -> LanguageMode.ENGLISH
            else -> LanguageMode.SYSTEM
        }
    }

    fun setLanguageMode(mode: LanguageMode) {
        _languageMode.value = mode
        prefs.edit().putString(KEY_LANGUAGE_MODE, mode.name).apply()
    }

    fun getLocale(): Locale? {
        return when (_languageMode.value) {
            LanguageMode.CHINESE -> Locale("zh", "CN")
            LanguageMode.ENGLISH -> Locale("en")
            LanguageMode.SYSTEM -> null
        }
    }
}
