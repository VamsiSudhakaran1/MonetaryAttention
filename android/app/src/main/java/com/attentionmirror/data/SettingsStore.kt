package com.attentionmirror.data

import android.content.Context

/**
 * Tiny preferences wrapper for user-facing settings. Currently just the opt-in
 * "hard truth" tone (see [com.attentionmirror.domain.Copy]).
 */
class SettingsStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var hardTruthMode: Boolean
        get() = prefs.getBoolean(KEY_HARD_TRUTH, false)
        set(value) = prefs.edit().putBoolean(KEY_HARD_TRUTH, value).apply()

    private companion object {
        const val KEY_HARD_TRUTH = "hard_truth_mode"
    }
}
