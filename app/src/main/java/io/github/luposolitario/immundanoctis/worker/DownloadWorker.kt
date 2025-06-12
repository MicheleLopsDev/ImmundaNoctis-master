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
        const val KEY_URL = "KEY_URL"
        const val KEY_DESTINATION = "KEY_DESTINATION"
        const val KEY_PROGRESS = "KEY_PROGRESS"
        const val KEY_BYTES_DOWNLOADED = "KEY_BYTES_DOWNLOADED"
        const val KEY_TOTAL_BYTES = "KEY_TOTAL_BYTES"
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
                val outputStream = FileOutputStream(destinationFile)
                val inputStream = connection.getInputStream()

                val buffer = ByteArray(4 * 1024)
                var bytesRead: Int
                var bytesDownloaded = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    bytesDownloaded += bytesRead

                    // Invia aggiornamenti sul progresso
                    val progress = (bytesDownloaded * 100 / totalBytes).toInt()
                    setProgress(workDataOf(KEY_PROGRESS to progress))
                }

                outputStream.close()
                inputStream.close()
                Result.success()
            } catch (e: Exception) {
                Result.failure()
            }
        }
    }
}
