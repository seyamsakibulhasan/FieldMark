package com.fieldmark.app.i18n

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object LocaleManager {
    enum class Lang(val tag: String) { ENGLISH("en"), BANGLA("bn");
        override fun toString(): String = tag
    }

    private const val PREFS = "fieldmark_prefs"
    private const val KEY_LANG = "app_language"

    private val _current = MutableStateFlow(Lang.ENGLISH.tag)
    val current: StateFlow<String> = _current

    fun applyStored(context: Context) {
        val tag = prefs(context).getString(KEY_LANG, Lang.ENGLISH.tag) ?: Lang.ENGLISH.tag
        set(tag)
    }

    fun set(tag: String) {
        _current.value = tag
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(tag)
        )
    }

    fun set(context: Context, lang: Lang) {
        prefs(context).edit().putString(KEY_LANG, lang.tag).apply()
        set(lang.tag)
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
