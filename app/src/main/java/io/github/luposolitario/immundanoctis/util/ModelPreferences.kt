package io.github.luposolitario.immundanoctis.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import java.io.File

/**
 * Gestisce la lettura e scrittura delle informazioni dell'ultimo modello
 * scaricato, utilizzando le SharedPreferences.
 */
class ModelPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "immunda_noctis_model_prefs"
        // Definiamo una chiave per ogni informazione che vogliamo salvare
        private const val KEY_MODEL_NAME = "key_model_name"
        private const val KEY_MODEL_URL = "key_model_url"
        private const val KEY_MODEL_FILE_PATH = "key_model_file_path"
    }

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Salva le informazioni di un oggetto Downloadable.
     *
     * @param model L'oggetto Downloadable da cui estrarre i dati.
     */
    fun saveLastModel(model: Downloadable) {
        with(sharedPrefs.edit()) {
            putString(KEY_MODEL_NAME, model.name)
            putString(KEY_MODEL_URL, model.source.toString())
            putString(KEY_MODEL_FILE_PATH, model.destination.path)
            apply()
        }
    }

    /**
     * Recupera le informazioni salvate e le usa per ricostruire un oggetto Downloadable.
     *
     * @return Un oggetto Downloadable se tutte le informazioni sono presenti, altrimenti null.
     */
    fun getLastModel(): Downloadable? {
        val name = sharedPrefs.getString(KEY_MODEL_NAME, null)
        val urlString = sharedPrefs.getString(KEY_MODEL_URL, null)
        val filePath = sharedPrefs.getString(KEY_MODEL_FILE_PATH, null)

        // Se abbiamo tutti e tre i pezzi, possiamo ricostruire l'oggetto
        return if (name != null && urlString != null && filePath != null) {
            Downloadable(
                name = name,
                source = Uri.parse(urlString),
                destination = File(filePath)
            )
        } else {
            null // Altrimenti, non abbiamo un modello valido salvato
        }
    }

    /**
     * Rimuove tutte le informazioni del modello salvato dalle preferenze.
     * Utile quando il file viene cancellato.
     */
    fun clearLastModel() {
        with(sharedPrefs.edit()) {
            remove(KEY_MODEL_NAME)
            remove(KEY_MODEL_URL)
            remove(KEY_MODEL_FILE_PATH)
            apply()
        }
    }
}
