package io.github.luposolitario.immundanoctis.stdf.service

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.luposolitario.immundanoctis.R
import io.github.luposolitario.immundanoctis.stdf.data.StdfModel
import io.github.luposolitario.immundanoctis.stdf.data.StdfModelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.IOException

class BackendService : Service() {
    private var process: Process? = null
    private val binder = LocalBinder()
    private val _backendState = MutableStateFlow<BackendState>(BackendState.Idle)
    val backendState: StateFlow<BackendState> = _backendState

    private fun updateState(state: BackendState) { _backendState.value = state }

    companion object {
        private const val TAG = "StdfBackendService"
        private const val EXECUTABLE_NAME = "libstable_diffusion_core.so"
        private const val NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "stdf_backend_service_channel"
    }

    sealed class BackendState {
        object Idle: BackendState()
        object Starting: BackendState()
        object Running: BackendState()
        data class Error(val message: String): BackendState()
    }

    inner class LocalBinder : Binder() { fun getService(): BackendService = this@BackendService }
    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val modelId = intent?.getStringExtra("modelId")
        if (modelId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification("Avvio del backend..."))
        val model = StdfModelRepository(this).models.find { it.id == modelId }

        if (model != null) {
            startBackend(model)
        } else {
            updateState(BackendState.Error("Modello non trovato"))
            stopSelf()
        }

        return START_STICKY
    }

    private fun startBackend(model: StdfModel) {
        if (_backendState.value is BackendState.Running || _backendState.value is BackendState.Starting) return
        updateState(BackendState.Starting)

        Thread {
            try {
                val nativeDir = applicationInfo.nativeLibraryDir
                val executableFile = File(nativeDir, EXECUTABLE_NAME)

                if (!executableFile.exists() || !executableFile.canExecute()) {
                    throw IOException("File eseguibile non trovato o non eseguibile! Controlla la configurazione del modulo :stdf.")
                }

                val modelDir = File(StdfModel.getModelsDir(this), model.id)
                val command = buildCommand(executableFile, modelDir, model, nativeDir)

                // *** MODIFICA PRINCIPALE: Imposta LD_LIBRARY_PATH come nell'app funzionante ***
                val currentLdPath = System.getenv("LD_LIBRARY_PATH") ?: ""
                val newLdPath = if (currentLdPath.isNotEmpty()) {
                    "$nativeDir:$currentLdPath"
                } else {
                    "$nativeDir:/system/lib64:/vendor/lib64"
                }

                val env = mapOf(
                    "LD_LIBRARY_PATH" to newLdPath,
                    "DSP_LIBRARY_PATH" to nativeDir
                )

                Log.d(TAG, "Comando Finale: ${command.joinToString(" ")}")
                Log.d(TAG, "DIR: $nativeDir")
                Log.d(TAG, "LD_LIBRARY_PATH=$newLdPath")
                Log.d(TAG, "DSP_LIBRARY_PATH=$nativeDir")

                // *** NON usare .directory() - esegui dalla directory corrente ***
                val processBuilder = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .apply {
                        environment().putAll(env)
                    }

                process = processBuilder.start()

                updateState(BackendState.Running)
                Log.i(TAG, "✅ Backend C++ avviato con successo.")

                process?.inputStream?.bufferedReader()?.useLines { lines ->
                    lines.forEach { line ->
                        Log.i(TAG, "C++ Backend: $line")
                    }
                }

                val exitCode = process?.waitFor()
                Log.w(TAG, "Processo backend terminato con codice: $exitCode")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Fallimento avvio backend", e)
                updateState(BackendState.Error("Fallimento avvio backend: ${e.message}"))
            } finally {
                stopSelf()
            }
        }.start()
    }

    private fun buildCommand(executable: File, modelDir: File, model: StdfModel, nativeDir: String): List<String> {
        val vaeEncoderPath = if (model.runOnCpu) {
            File(modelDir, "vae_encoder.mnn").absolutePath
        } else {
            File(StdfModel.getModelsDir(this), "anythingv5/vae_encoder.bin").absolutePath
        }

        // *** ORDINE DEI PARAMETRI COME NELL'APP FUNZIONANTE ***
        return mutableListOf(
            executable.absolutePath,
            "--clip", File(modelDir, if (model.useCpuClip) "clip.mnn" else "clip.bin").absolutePath,
            "--unet", File(modelDir, if (model.runOnCpu) "unet.mnn" else "unet.bin").absolutePath,
            "--vae_decoder", File(modelDir, if (model.runOnCpu) "vae_decoder.mnn" else "vae_decoder.bin").absolutePath,
            "--tokenizer", File(modelDir, "tokenizer.json").absolutePath,
            "--backend", File(nativeDir, "libQnnHtp.so").absolutePath,
            "--system_library", File(nativeDir, "libQnnSystem.so").absolutePath,
            "--port", "8081",
            "--text_embedding_size", model.textEmbeddingSize.toString(),
            "--vae_encoder", vaeEncoderPath
        ).apply {
            if (model.runOnCpu) add("--cpu")
            if (model.useCpuClip) add("--use_cpu_clip")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        process?.destroyForcibly()
        updateState(BackendState.Idle)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "STDF Backend Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Immunda Noctis STDF")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }
}