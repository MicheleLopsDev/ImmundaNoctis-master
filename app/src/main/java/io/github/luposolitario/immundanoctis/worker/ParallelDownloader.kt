package io.github.luposolitario.immundanoctis.worker

import android.util.Log
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

suspend fun downloadInParallel(
    urlString: String,
    outputFile: String,
    accessToken: String,
    numThreads: Int = 4
) = withContext(Dispatchers.IO) {

    val url = URL(urlString)

    val connection = (url.openConnection() as HttpURLConnection).apply {
        setRequestProperty("Authorization", "Bearer $accessToken")
        setRequestProperty("Accept-Encoding", "identity")
        requestMethod = "HEAD"
        connect()
    }

    val responseCode = connection.responseCode
    val responseMessage = connection.responseMessage
    val headers = connection.headerFields

    Log.d("Downloader", "Response code: $responseCode")
    Log.d("Downloader", "Response message: $responseMessage")
    headers.forEach { (key, value) ->
        Log.d("Downloader", "$key: $value")
    }

    if (responseCode != 200) {
        throw IllegalStateException("Errore HTTP $responseCode: $responseMessage")
    }

    val totalSize = connection.contentLengthLong
    if (connection.getHeaderField("Accept-Ranges") != "bytes") {
        throw IllegalStateException("Il server non supporta i byte range")
    }

    val partSize = totalSize / numThreads
    val raf = RandomAccessFile(outputFile, "rw")
    raf.setLength(totalSize.toLong())
    raf.close()

    val executor = Executors.newFixedThreadPool(numThreads)
    val tasks = (0 until numThreads).map { i ->
        executor.submit {
            val start = i * partSize
            val end = if (i == numThreads - 1) totalSize - 1 else (start + partSize - 1)

            val partConn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Range", "bytes=$start-$end")
                connect()
            }

            val partStream = partConn.inputStream
            val partBuffer = ByteArray(1024 * 1024)
            val rafPart = RandomAccessFile(outputFile, "rw")
            rafPart.seek(start)

            var bytes = partStream.read(partBuffer)
            while (bytes != -1) {
                rafPart.write(partBuffer, 0, bytes)
                bytes = partStream.read(partBuffer)
            }

            rafPart.close()
            partStream.close()
        }
    }

    tasks.forEach { it.get() } // aspetta tutti i thread
    executor.shutdown()
}
