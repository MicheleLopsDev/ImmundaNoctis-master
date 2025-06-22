package io.github.luposolitario.immundanoctis.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestisce le preferenze specifiche per i parametri del LlamaCppEngine.
 */
class LlamaPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var nLen: Int
        get() = prefs.getInt(KEY_N_LEN, 2048)
        set(value) = prefs.edit().putInt(KEY_N_LEN, value).apply()

    companion object {
        private const val PREFS_NAME = "llama_preferences"
        private const val KEY_N_LEN = "llama_n_len"
    }
}
