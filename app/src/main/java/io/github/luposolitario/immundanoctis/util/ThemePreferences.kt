package io.github.luposolitario.immundanoctis.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestisce la lettura e scrittura della preferenza del tema (chiaro/scuro) e ora anche l'ultimo personaggio selezionato.
 */
class ThemePreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_IS_DARK_THEME = "is_dark_theme"
        private const val KEY_HF_TOKEN = "hugging_face_token"
        // NUOVA CHIAVE: ID dell'ultimo personaggio selezionato
        private const val KEY_LAST_SELECTED_CHARACTER_ID = "last_selected_character_id"
    }

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Salva la preferenza del tema.
     * @param isDark true per il tema scuro, false per quello chiaro.
     */
    fun saveTheme(isDark: Boolean) {
        with(sharedPrefs.edit()) {
            putBoolean(KEY_IS_DARK_THEME, isDark)
            apply()
        }
    }

    /**
     * Controlla se il tema scuro è stato esplicitamente impostato.
     * @return true/false se una preferenza è stata salvata, altrimenti null.
     */
    private fun getThemeSetting(): Boolean? {
        // Controlla se la chiave esiste prima di restituire un valore
        return if (sharedPrefs.contains(KEY_IS_DARK_THEME)) {
            sharedPrefs.getBoolean(KEY_IS_DARK_THEME, false)
        } else {
            null
        }
    }

    /**
     * Determina quale tema usare: quello salvato dall'utente, o come fallback,
     * quello del sistema operativo.
     * @param systemIsDark Il valore di isSystemInDarkTheme().
     * @return true se il tema da usare è quello scuro.
     */
    fun useDarkTheme(systemIsDark: Boolean): Boolean {
        return getThemeSetting() ?: systemIsDark
    }

    fun saveToken(token: String?) {
        with(sharedPrefs.edit()) {
            putString(KEY_HF_TOKEN, token)
            apply()
        }
    }

    fun getToken(): String? {
        return sharedPrefs.getString(KEY_HF_TOKEN, null)
    }

    // NUOVI METODI: Salva e recupera l'ID dell'ultimo personaggio selezionato
    fun saveLastSelectedCharacterId(characterId: String) {
        with(sharedPrefs.edit()) {
            putString(KEY_LAST_SELECTED_CHARACTER_ID, characterId)
            apply()
        }
    }

    fun getLastSelectedCharacterId(): String? {
        return sharedPrefs.getString(KEY_LAST_SELECTED_CHARACTER_ID, null)
    }
}