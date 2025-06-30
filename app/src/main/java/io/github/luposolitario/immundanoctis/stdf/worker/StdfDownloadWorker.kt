package io.github.luposolitario.immundanoctis.stdf.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.luposolitario.immundanoctis.stdf.data.ModelFile
import io.github.luposolitario.immundanoctis.stdf.data.StdfFileVerification
import io.github.luposolitario.immundanoctis.stdf.data.StdfModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Worker dedicato che gestisce il download di TUTTI i file per un singolo modello STDF.
 */
class StdfDownloadWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val fileVerification = StdfFileVerification(context)
    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return@withContext Result.failure()
        val modelFilesJson = inputData.getString(KEY_MODEL_FILES_JSON) ?: return@withContext Result.failure()
        val baseUrl = inputData.getString(KEY_BASE_URL) ?: return@withContext Result.failure()
        val hfToken = inputData.getString(KEY_HF_TOKEN)

        // Deserializza la lista di file da scaricare
        val fileListType = object : TypeToken<List<ModelFile>>() {}.type
        val filesToDownload: List<ModelFile> = gson.fromJson(modelFilesJson, fileListType)

        val totalFiles = filesToDownload.size
        var filesCompleted = 0

        Log.d(TAG, "✅ Worker AVVIATO per il modello $modelId. File totali: $totalFiles")

        for (modelFile in filesToDownload) {
            val destinationDir = File(StdfModel.getModelsDir(context), modelId)
            destinationDir.mkdirs()
            val destinationFile = File(destinationDir, modelFile.name)
            val fileUrl = "${baseUrl.removeSuffix("/")}/${modelFile.uri}"

            Log.d(TAG, "Inizio download file ${filesCompleted + 1}/$totalFiles: ${modelFile.name}")

            try {
                // Se il file è già scaricato e verificato, saltalo
                val savedSize = fileVerification.getFileSize(modelId, modelFile.name)
                if (destinationFile.exists() && destinationFile.length() == savedSize && savedSize != -1L) {
                    Log.d(TAG, "File ${modelFile.name} già presente e verificato. Salto.")
                    filesCompleted++
                    continue
                }

                // Esegui il download del singolo file
                downloadFile(fileUrl, destinationFile, hfToken)

                // Verifica e salva la dimensione
                fileVerification.saveFileSize(modelId, modelFile.name, destinationFile.length())
                Log.d(TAG, "Completato: ${modelFile.name}, Dimensione: ${destinationFile.length()}")
                filesCompleted++

                // Aggiorna il progresso complessivo
                val overallProgress = (filesCompleted * 100 / totalFiles)
                setProgress(workDataOf(KEY_PROGRESS to overallProgress))

            } catch (e: Exception) {
                Log.e(TAG, "❌ Download FALLITO per il file ${modelFile.name}", e)
                return@withContext Result.failure()
            }
        }

        Log.d(TAG, "✅ TUTTI i file per il modello $modelId sono stati scaricati con successo.")
        return@withContext Result.success()
    }

    /**
     * Logica di download per un singolo file.
     */
    private suspend fun downloadFile(fileUrl: String, destinationFile: File, hfToken: String?) {
        val url = URL(fileUrl)
        val connection = url.openConnection() as HttpURLConnection
        if (!hfToken.isNullOrEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer $hfToken")
        }
        connection.connect()

        if (connection.responseCode !in 200..299) throw Exception("Errore server: ${connection.responseCode}")

        connection.inputStream.use { input ->
            FileOutputStream(destinationFile).use { output ->
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    if (isStopped) {
                        destinationFile.delete()
                        throw Exception("Worker interrotto.")
                    }
                    output.write(buffer, 0, bytesRead)
                }
            }
        }
    }

    companion object {
        const val TAG = "StdfDownloadWorker"
        const val KEY_MODEL_ID = "model_id"
        const val KEY_MODEL_FILES_JSON = "model_files_json" // Ora passiamo una stringa JSON
        const val KEY_BASE_URL = "base_url"
        const val KEY_HF_TOKEN = "hf_token"
        const val KEY_PROGRESS = "progress"
    }
}