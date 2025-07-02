package io.github.luposolitario.immundanoctis.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.util.Log // Importa Log

/**
 * Copia un file dagli assets dell'applicazione a una directory specifica.
 *
 * @param context Il contesto dell'applicazione.
 * @param assetFileName Il nome del file nell'asset folder (es. "config.json").
 * @param destinationDirectory La directory di destinazione (es. la cartella "scenes").
 * @param destinationFileName Il nome del file di destinazione (se diverso da assetFileName).
 * @return true se la copia ha avuto successo, false altrimenti.
 */
fun copyAssetToFile(
    context: Context,
    assetFileName: String,
    destinationDirectory: File,
    destinationFileName: String = assetFileName // Nome di destinazione predefinito
): Boolean {
    // Assicurati che la directory di destinazione esista
    if (!destinationDirectory.exists()) {
        destinationDirectory.mkdirs()
    }

    val destinationFile = File(destinationDirectory, destinationFileName)

    try {
        context.assets.open(assetFileName).use { inputStream ->
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        Log.d("FileCopier", "File '$assetFileName' copiato con successo in '${destinationFile.absolutePath}'")
        return true
    } catch (e: IOException) {
        Log.e("FileCopier", "Errore durante la copia del file '$assetFileName': ${e.message}", e)
        return false
    }
}

// Assicurati che questa funzione sia corretta per ottenere la directory desiderata
// La tua richiesta specifica "/sdcard/Android/data/io.github.luposolitario.immundanoctis/files/scenes"
// significa che dovresti usare context.getExternalFilesDir(null) per la base.
fun getAppSpecificDirectory(context: Context, dirName: String): File? {
    // getExternalFilesDir(null) punta a /sdcard/Android/data/your.package.name/files
    val baseDir = context.getExternalFilesDir(null)
    return if (baseDir != null) {
        val appDir = File(baseDir, dirName)
        if (!appDir.exists()) {
            appDir.mkdirs() // Crea la directory se non esiste
        }
        appDir
    } else {
        null
    }
}