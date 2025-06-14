package io.github.luposolitario.immundanoctis.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Salva la preferenza dell'utente sulla scelta della modalità del motore AI.
 * - `false` (default): Modalità Mista (Gemma per DM, GGUF per PG).
 * - `true`: Modalità Solo Gemma (Gemma per DM e per PG).
 */
class EnginePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var useGemmaForAll: Boolean
        get() = prefs.getBoolean(KEY_USE_GEMMA_FOR_ALL, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_GEMMA_FOR_ALL, value).apply()

    companion object {
        private const val PREFS_NAME = "engine_preferences"
        private const val KEY_USE_GEMMA_FOR_ALL = "use_gemma_for_all"
    }
}
