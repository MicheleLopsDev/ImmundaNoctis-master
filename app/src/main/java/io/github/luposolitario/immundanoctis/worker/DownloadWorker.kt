package io.github.luposolitario.immundanoctis.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_URL = "key_url"
        const val KEY_DESTINATION = "key_destination"

        // MODIFICA: Useremo queste chiavi per passare dati più dettagliati
        const val KEY_PROGRESS = "key_progress_percent"
        const val KEY_BYTES_DOWNLOADED = "key_bytes_downloaded"
        const val KEY_TOTAL_BYTES = "key_total_bytes"
    }

    override suspend fun doWork(): Result {
        val urlString = inputData.getString(KEY_URL) ?: return Result.failure()
        val destinationPath = inputData.getString(KEY_DESTINATION) ?: return Result.failure()

        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection()
                connection.connect()

                val totalBytes = connection.contentLengthLong
                val destinationFile = File(destinationPath)
                destinationFile.parentFile?.mkdirs()
                val outputStream = FileOutputStream(destinationFile)
                val inputStream = connection.getInputStream()

                val buffer = ByteArray(4 * 1024)
                var bytesRead: Int
                var bytesDownloaded = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    bytesDownloaded += bytesRead

                    // MODIFICA: Inviamo dati di progresso più ricchi
                    val progressData = workDataOf(
                        KEY_BYTES_DOWNLOADED to bytesDownloaded,
                        KEY_TOTAL_BYTES to totalBytes
                    )
                    setProgress(progressData)
                }

                outputStream.close()
                inputStream.close()
                Result.success()
            } catch (e: Exception) {
                File(destinationPath).delete()
                Result.failure()
            }
        }
    }
}
