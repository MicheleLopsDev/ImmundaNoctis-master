package io.github.luposolitario.immundanoctis.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.*
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong


class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_URL = "key_url"
        const val KEY_DESTINATION = "key_destination"
        const val KEY_MODEL_DOWNLOAD_ACCESS_TOKEN = "KEY_MODEL_DOWNLOAD_ACCESS_TOKEN"
        const val KEY_MODEL_NAME = "KEY_MODEL_NAME"
        const val KEY_BYTES_DOWNLOADED = "key_bytes_downloaded"
        const val KEY_TOTAL_BYTES = "key_total_bytes"
    }



    private fun createForegroundInfo(): ForegroundInfo {
        val channelId = "download_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Download in corso",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Download modello in corso")
            .setContentText("Scaricamento in background...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

        // 🔥 Qui sta il punto: aggiungi il tipo
        return ForegroundInfo(
            1001,
            notification,
            FOREGROUND_SERVICE_TYPE_DATA_SYNC // <--- OBBLIGATORIO su Android 14+
        )
    }


    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val urlString = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val destinationPath = inputData.getString(KEY_DESTINATION) ?: return@withContext Result.failure()
        val accessToken = inputData.getString(KEY_MODEL_DOWNLOAD_ACCESS_TOKEN) ?: return@withContext Result.failure()
        val totalDownloaded = AtomicLong(0L)
        val url = URL(urlString)


        try {
            setForeground(createForegroundInfo()) // <-- fondamentale
            val headConnection = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Accept-Encoding", "identity")
                requestMethod = "HEAD"
                connect()
            }

            val responseCode = headConnection.responseCode
            val totalSize = headConnection.contentLengthLong

            if (responseCode != 200) {
                Log.e("Downloader", "Errore HTTP $responseCode")
                return@withContext Result.failure()
            }

            if (headConnection.getHeaderField("Accept-Ranges") != "bytes") {
                Log.e("Downloader", "Il server non supporta i byte-range")
                return@withContext Result.failure()
            }

            Log.d("Downloader", "Total size: $totalSize")

            // Prepara il file
            val file = File(destinationPath)
            file.parentFile?.mkdirs()
            RandomAccessFile(file, "rw").setLength(totalSize)

            val numParts = 8
            val partSize = totalSize / numParts

            coroutineScope {
                (0 until numParts).map { index ->
                    async {
                        val start = index * partSize
                        val end = if (index == numParts - 1) totalSize - 1 else (start + partSize - 1)

                        val partConn = (url.openConnection() as HttpURLConnection).apply {
                            setRequestProperty("Authorization", "Bearer $accessToken")
                            setRequestProperty("Range", "bytes=$start-$end")
                            connect()
                        }

                        val buffer = ByteArray(1024 * 1024)
                        val input = partConn.inputStream
                        val raf = RandomAccessFile(file, "rw").apply { seek(start) }

                        var bytes = input.read(buffer)
                        while (bytes != -1) {
                            raf.write(buffer, 0, bytes)

                            val downloaded = totalDownloaded.addAndGet(bytes.toLong())

                            setProgress(
                                workDataOf(
                                    KEY_TOTAL_BYTES to totalSize,
                                    KEY_BYTES_DOWNLOADED to downloaded
                                )
                            )

                            bytes = input.read(buffer)
                        }

                        input.close()
                        raf.close()
                    }
                }.awaitAll()
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Errore: ${e.message}",e)
            File(destinationPath).delete()
            Result.failure()
        }
    }
}
