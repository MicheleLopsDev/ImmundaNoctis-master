package io.github.luposolitario.immundanoctis.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import java.io.File

/**
 * Gestisce la lettura e scrittura delle informazioni per i modelli del DM e dei PG,
 * utilizzando le SharedPreferences.
 */
class ModelPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "immunda_noctis_model_prefs"

        // Chiavi per il modello del DM
        private const val KEY_DM_MODEL_NAME = "dm_model_name"
        private const val KEY_DM_MODEL_URL = "dm_model_url"
        private const val KEY_DM_MODEL_PATH = "dm_model_file_path"

        // Chiavi per il modello dei PG
        private const val KEY_PLAYER_MODEL_NAME = "player_model_name"
        private const val KEY_PLAYER_MODEL_URL = "player_model_url"
        private const val KEY_PLAYER_MODEL_PATH = "player_model_file_path"
    }

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- Funzioni per il Modello del DM ---

    /**
     * Salva le informazioni del modello del DM.
     */
    fun saveDmModel(model: Downloadable) {
        with(sharedPrefs.edit()) {
            putString(KEY_DM_MODEL_NAME, model.name)
            putString(KEY_DM_MODEL_URL, model.source.toString())
            putString(KEY_DM_MODEL_PATH, model.destination.path)
            apply()
        }
    }

    /**
     * Recupera il modello del DM salvato.
     * @return Un oggetto Downloadable se presente, altrimenti null.
     */
    fun getDmModel(): Downloadable? {
        val name = sharedPrefs.getString(KEY_DM_MODEL_NAME, null)
        val urlString = sharedPrefs.getString(KEY_DM_MODEL_URL, null)
        val filePath = sharedPrefs.getString(KEY_DM_MODEL_PATH, null)

        return if (name != null && urlString != null && filePath != null) {
            Downloadable(name, Uri.parse(urlString), File(filePath))
        } else {
            null
        }
    }

    /**
     * Rimuove le informazioni del modello del DM.
     */
    fun clearDmModel() {
        with(sharedPrefs.edit()) {
            remove(KEY_DM_MODEL_NAME)
            remove(KEY_DM_MODEL_URL)
            remove(KEY_DM_MODEL_PATH)
            apply()
        }
    }

    // --- Funzioni per il Modello dei PG ---

    /**
     * Salva le informazioni del modello dei PG.
     */
    fun savePlayerModel(model: Downloadable) {
        with(sharedPrefs.edit()) {
            putString(KEY_PLAYER_MODEL_NAME, model.name)
            putString(KEY_PLAYER_MODEL_URL, model.source.toString())
            putString(KEY_PLAYER_MODEL_PATH, model.destination.path)
            apply()
        }
    }

    /**
     * Recupera il modello dei PG salvato.
     * @return Un oggetto Downloadable se presente, altrimenti null.
     */
    fun getPlayerModel(): Downloadable? {
        val name = sharedPrefs.getString(KEY_PLAYER_MODEL_NAME, null)
        val urlString = sharedPrefs.getString(KEY_PLAYER_MODEL_URL, null)
        val filePath = sharedPrefs.getString(KEY_PLAYER_MODEL_PATH, null)

        return if (name != null && urlString != null && filePath != null) {
            Downloadable(name, Uri.parse(urlString), File(filePath))
        } else {
            null
        }
    }

    /**
     * Rimuove le informazioni del modello dei PG.
     */
    fun clearPlayerModel() {
        with(sharedPrefs.edit()) {
            remove(KEY_PLAYER_MODEL_NAME)
            remove(KEY_PLAYER_MODEL_URL)
            remove(KEY_PLAYER_MODEL_PATH)
            apply()
        }
    }
}
