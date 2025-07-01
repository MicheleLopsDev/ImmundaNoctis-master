package io.github.luposolitario.immundanoctis.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestisce le preferenze relative al salvataggio e allo stile di gioco.
 */
class SavePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Controlla se il salvataggio automatico è abilitato.
     * Il valore predefinito è `true`.
     */
    var isAutoSaveEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTOSAVE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTOSAVE, value).apply()

    /**
     * Controlla se la chat e l'interazione con i PNG sono abilitate.
     * Il valore predefinito è ora `false`.
     */
    var isChatEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHAT_ENABLED, false) // <-- MODIFICA QUI
        set(value) = prefs.edit().putBoolean(KEY_CHAT_ENABLED, value).apply()

    companion object {
        private const val PREFS_NAME = "save_preferences"
        private const val KEY_AUTOSAVE = "is_auto_save_enabled"
        private const val KEY_CHAT_ENABLED = "is_chat_enabled"
    }
}