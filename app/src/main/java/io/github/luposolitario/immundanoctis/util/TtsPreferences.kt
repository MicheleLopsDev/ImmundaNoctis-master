package io.github.luposolitario.immundanoctis.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestisce le preferenze relative al sistema Text-to-Speech (TTS).
 *
 * @param context Il contesto dell'applicazione, necessario per accedere alle SharedPreferences.
 */
class TtsPreferences(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(TTS_PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Salva l'impostazione per la lettura automatica dei messaggi.
     *
     * @param enabled Se `true`, la lettura automatica è abilitata.
     */
    fun saveAutoRead(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_READ, enabled).apply()
    }

    /**
     * Recupera l'impostazione per la lettura automatica dei messaggi.
     *
     * @return `true` se la lettura automatica è abilitata, altrimenti `false`. Il valore predefinito è `false`.
     */
    fun isAutoReadEnabled(): Boolean {
        return preferences.getBoolean(KEY_AUTO_READ, false)
    }

    companion object {
        private const val TTS_PREFS_NAME = "tts_preferences"
        private const val KEY_AUTO_READ = "auto_read_enabled"
    }
}
