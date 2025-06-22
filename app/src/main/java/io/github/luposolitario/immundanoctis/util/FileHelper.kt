package io.github.luposolitario.immundanoctis.util

import android.content.Context
import java.io.File

/**
 * Ottiene una directory specifica all'interno della cartella esterna dell'app.
 * Se la cartella non esiste, viene creata.
 *
 * @param context Il contesto dell'applicazione.
 * @param subfolder La sottocartella da creare/accedere (es. "downloads", "saves").
 * @return Un oggetto File che rappresenta la directory, o null se la memoria esterna non è accessibile.
 */
fun getAppSpecificDirectory(context: Context, subfolder: String): File? {
    val appSpecificDir = context.getExternalFilesDir(null) ?: return null
    val customDir = File(appSpecificDir, subfolder)
    if (!customDir.exists()) {
        customDir.mkdirs()
    }
    return customDir
}
