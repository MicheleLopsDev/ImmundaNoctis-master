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


    var scenesPath: String?
        get() = prefs.getString(KEY_JSON_SCNES_PATH, "./scenes/scenes.json")
        set(value) = prefs.edit().putString(KEY_JSON_SCNES_PATH, value).apply()

    // ******* NUOVA PROPRIETÀ PER IL FLAG DI COPIA DEL CONFIG.JSON *******
    var isConfigCopied: Boolean
        get() = prefs.getBoolean(KEY_CONFIG_COPIED, false)
        set(value) = prefs.edit().putBoolean(KEY_CONFIG_COPIED, value).apply()


    companion object {
        private const val PREFS_NAME = "save_preferences"
        private const val KEY_AUTOSAVE = "is_auto_save_enabled"
        private const val KEY_CHAT_ENABLED = "is_chat_enabled"
        private const val KEY_JSON_SCNES_PATH = "json_scenes_path"
        // ******* NUOVA CHIAVE PER IL FLAG *******
        private const val KEY_CONFIG_COPIED = "is_config_copied"
    }
}