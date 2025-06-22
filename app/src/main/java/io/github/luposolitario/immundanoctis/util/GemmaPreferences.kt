package io.github.luposolitario.immundanoctis.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestisce le preferenze specifiche per i parametri del GemmaEngine.
 */
class GemmaPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var nLen: Int
        get() = prefs.getInt(KEY_N_LEN, 4096)
        set(value) = prefs.edit().putInt(KEY_N_LEN, value).apply()

    var temperature: Float
        get() = prefs.getFloat(KEY_TEMPERATURE, 0.7f)
        set(value) = prefs.edit().putFloat(KEY_TEMPERATURE, value).apply()

    var topK: Int
        get() = prefs.getInt(KEY_TOP_K, 40)
        set(value) = prefs.edit().putInt(KEY_TOP_K, value).apply()

    var topP: Float
        get() = prefs.getFloat(KEY_TOP_P, 0.9f)
        set(value) = prefs.edit().putFloat(KEY_TOP_P, value).apply()

    companion object {
        private const val PREFS_NAME = "gemma_preferences"
        private const val KEY_N_LEN = "gemma_n_len"
        private const val KEY_TEMPERATURE = "gemma_temperature"
        private const val KEY_TOP_K = "gemma_top_k"
        private const val KEY_TOP_P = "gemma_top_p"
    }
}
