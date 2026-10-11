package dev.qssn70.deeptutor.config

import android.content.Context

/**
 * Client-only preferences. Never writes to DeepTutor's site localStorage.
 * The web application remains responsible for account/theme/session settings.
 */
class MobilePreferences(context: Context) {
    private val store = context.getSharedPreferences("mobile_preferences", Context.MODE_PRIVATE)

    var textZoomPercent: Int
        get() = store.getInt("text_zoom_percent", 100).takeIf { it in ZOOM_LEVELS } ?: 100
        set(value) {
            require(value in ZOOM_LEVELS)
            store.edit().putInt("text_zoom_percent", value).apply()
        }

    var enableReadingFixes: Boolean
        get() = store.getBoolean("reading_fixes", true)
        set(value) { store.edit().putBoolean("reading_fixes", value).apply() }

    var menuOnLeft: Boolean
        get() = store.getBoolean("menu_on_left", false)
        set(value) { store.edit().putBoolean("menu_on_left", value).apply() }

    fun nextTextZoom(): Int {
        val currentIndex = ZOOM_LEVELS.indexOf(textZoomPercent)
        val next = ZOOM_LEVELS[(currentIndex + 1) % ZOOM_LEVELS.size]
        textZoomPercent = next
        return next
    }

    companion object {
        val ZOOM_LEVELS = listOf(100, 110, 120, 130)
    }
}
