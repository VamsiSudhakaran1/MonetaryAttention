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

    /**
     * Packages the user has marked ad-free (e.g. YouTube Premium, X Premium).
     * Their time is still tracked, but no ad value is attributed.
     */
    var adFreePackages: Set<String>
        // Copy out: the Set returned by getStringSet must not be mutated.
        get() = prefs.getStringSet(KEY_AD_FREE, emptySet())?.toSet() ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_AD_FREE, value).apply()

    fun setAdFree(packageName: String, adFree: Boolean) {
        val updated = adFreePackages.toMutableSet()
        if (adFree) updated.add(packageName) else updated.remove(packageName)
        adFreePackages = updated
    }

    private companion object {
        const val KEY_HARD_TRUTH = "hard_truth_mode"
        const val KEY_AD_FREE = "ad_free_packages"
    }
}
