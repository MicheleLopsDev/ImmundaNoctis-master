package io.github.luposolitario.immundanoctis.stdf.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestisce la verifica dei file scaricati per i modelli STDF usando SharedPreferences.
 * Salva e recupera la dimensione attesa di ogni file per verificare l'integrità.
 */
class StdfFileVerification(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // La chiave per ogni file è una combinazione dell'ID del modello e del nome del file.
    private fun getFileSizeKey(modelId: String, fileName: String) = "${modelId}_${fileName}_size"

    /**
     * Salva la dimensione di un file scaricato.
     */
    fun saveFileSize(modelId: String, fileName: String, size: Long) {
        prefs.edit().putLong(getFileSizeKey(modelId, fileName), size).apply()
    }

    /**
     * Recupera la dimensione salvata di un file.
     * @return La dimensione in bytes o null se non trovata.
     */
    fun getFileSize(modelId: String, fileName: String): Long {
        // Restituisce -1 se non trovata, così da non essere mai uguale alla dimensione reale.
        return prefs.getLong(getFileSizeKey(modelId, fileName), -1L)
    }

    /**
     * Rimuove tutte le informazioni di verifica per un modello specifico.
     * Utile quando un modello viene cancellato.
     */
    fun clearVerification(modelId: String) {
        val editor = prefs.edit()
        val keysToRemove = prefs.all.keys.filter { it.startsWith("${modelId}_") }
        for (key in keysToRemove) {
            editor.remove(key)
        }
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "stdf_file_verification"
    }
}