package io.github.luposolitario.immundanoctis.stdf.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Base64
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import io.github.luposolitario.immundanoctis.R

class BackgroundGenerationService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    companion object {
        private const val TAG = "StdfGenerationService"
        private const val CHANNEL_ID = "stdf_image_generation_channel"
        private const val NOTIFICATION_ID = 3
        const val ACTION_STOP = "io.github.luposolitario.immundanoctis.STOP_STDF_GENERATION"

        private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
        val generationState: StateFlow<GenerationState> = _generationState
    }

    sealed class GenerationState {
        object Idle : GenerationState()
        data class Progress(val progress: Float) : GenerationState()
        data class Complete(val bitmap: Bitmap, val seed: Long?) : GenerationState()
        data class Error(val message: String) : GenerationState()
    }

    private fun updateState(newState: GenerationState) {
        _generationState.value = newState
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val prompt = intent?.getStringExtra("prompt")
        if (prompt == null) {
            Log.e(TAG, "Prompt nullo, il servizio non si avvia.")
            return START_NOT_STICKY
        }

        val negativePrompt = intent.getStringExtra("negative_prompt") ?: ""
        val steps = intent.getIntExtra("steps", 28)
        val cfg = intent.getFloatExtra("cfg", 7f)
        val seed = if (intent.hasExtra("seed")) intent.getLongExtra("seed", 0) else null
        val size = intent.getIntExtra("size", 512)

        Log.d(TAG, "Avvio generazione per prompt: $prompt")
        startForeground(NOTIFICATION_ID, createNotification(0f))

        if (_generationState.value is GenerationState.Complete) {
            updateState(GenerationState.Idle)
        }

        serviceScope.launch {
            runGeneration(prompt, negativePrompt, steps, cfg, seed, size)
        }

        return START_NOT_STICKY
    }

    private suspend fun runGeneration(
        prompt: String,
        negativePrompt: String,
        steps: Int,
        cfg: Float,
        seed: Long?,
        size: Int
    ) = withContext(Dispatchers.IO) {
        try {
            updateState(GenerationState.Progress(0f))

            val jsonObject = JSONObject().apply {
                put("prompt", prompt)
                put("negative_prompt", negativePrompt)
                put("steps", steps)
                put("cfg", cfg)
                put("use_cfg", true)
                put("size", size)
                seed?.let { put("seed", it) }
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url("http://localhost:8081/generate")
                .post(jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Errore di sistema durante la generazione: ${response.code}")
                }

                response.body?.let { responseBody ->
                    val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
                    var buffer = StringBuilder()

                    while (isActive) {
                        val char = reader.read()
                        if (char == -1) break

                        if (char.toChar() == '\n') {
                            val line = buffer.toString()
                            if (line.startsWith("data: ")) {
                                val data = line.substring(6).trim()
                                if (data == "[DONE]") break

                                val message = JSONObject(data)
                                when (message.optString("type")) {
                                    "progress" -> {
                                        val progress = message.optInt("step").toFloat() / message.optInt("total_steps")
                                        updateState(GenerationState.Progress(progress))
                                        updateNotification(progress)
                                    }
                                    "complete" -> {
                                        val base64Image = message.optString("image")
                                        val returnedSeed = message.optLong("seed", -1).takeIf { it != -1L }

                                        // --- 👇 LOGICA DI DECODIFICA CORRETTA E ROBUSTA 👇 ---
                                        try {
                                            val imageBytes = Base64.getDecoder().decode(base64Image)
                                            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                                            val pixels = IntArray(size * size)
                                            for (i in 0 until size * size) {
                                                val index = i * 3
                                                val r = imageBytes[index].toInt() and 0xFF
                                                val g = imageBytes[index + 1].toInt() and 0xFF
                                                val b = imageBytes[index + 2].toInt() and 0xFF
                                                pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                                            }
                                            bitmap.setPixels(pixels, 0, size, 0, 0, size, size)

                                            if (bitmap != null) {
                                                updateState(GenerationState.Complete(bitmap, returnedSeed))
                                            } else {
                                                throw IOException("BitmapFactory.decodeByteArray ha restituito null.")
                                            }
                                        } catch (e: IllegalArgumentException) {
                                            throw IOException("Stringa Base64 non valida.", e)
                                        }

                                        delay(500)
                                        stopSelf()
                                    }
                                    "error" -> throw IOException(message.optString("message", "Errore sconosciuto dal backend"))
                                }
                            }
                            buffer = StringBuilder()
                        } else {
                            buffer.append(char.toChar())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante la generazione", e)
            updateState(GenerationState.Error(e.message ?: getString(R.string.stdf_unknown_error)))
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "STDF Image Generation"
            val descriptionText = "Generazione immagini in background"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(progress: Float): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.stdf_generating_notification_title))
            .setContentText("Progresso: ${(progress * 100).toInt()}%")
            .setProgress(100, (progress * 100).toInt(), false)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(progress: Float) {
        notificationManager.notify(NOTIFICATION_ID, createNotification(progress))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (_generationState.value is GenerationState.Error || _generationState.value is GenerationState.Progress) {
            updateState(GenerationState.Idle)
        }
    }
}