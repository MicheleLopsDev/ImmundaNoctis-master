package io.github.luposolitario.immundanoctis.engine

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

/**
 * Implementazione di InferenceEngine che usa la libreria MediaPipe per i modelli Gemma.
 * AGGIORNATA per usare il corretto flusso con LlmInferenceSession.
 */
class GemmaEngine(private val context: Context) : InferenceEngine {
    private val tag = "GemmaEngine"
    private var llmInference: LlmInference? = null
    private var session: LlmInferenceSession? = null

    override suspend fun load(modelPath: String) {
        try {
            if (!File(modelPath).exists()) {
                Log.e(tag, "Modello Gemma non trovato: $modelPath")
                return
            }

            // 1. Crea le opzioni per il motore principale
            val inferenceOptions = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .build()
            llmInference = LlmInference.createFromOptions(context, inferenceOptions)

            // 2. Crea le opzioni per la sessione di chat
            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(40)
                .setTemperature(1.0f)
                .setTopP(0.9f)
                .build()

            // 3. Crea la sessione usando sia il motore che le sue opzioni
            session = LlmInferenceSession.createFromOptions(llmInference, sessionOptions)

            Log.d(tag, "Motore e sessione Gemma caricati con successo.")
        } catch (e: Exception) {
            Log.e(tag, "Errore durante il caricamento del modello o della sessione Gemma.", e)
        }
    }

    override fun sendMessage(text: String): Flow<String> = callbackFlow {
        if (session == null) {
            val errorMessage = "[ERRORE: Sessione di chat con Gemma non inizializzata]"
            Log.e(tag, errorMessage)
            trySend(errorMessage)
            close()
            return@callbackFlow
        }

        try {
            // MODIFICA CHIAVE: La chiamata ora avviene in due passaggi
            // 1. Aggiungi il prompt alla conversazione attuale della sessione
            session!!.addQueryChunk(text)

            // 2. Chiama generateResponseAsync solo con il listener per ricevere la risposta
            session!!.generateResponseAsync { partialResponse, done ->
                trySend(partialResponse)
                if (done) {
                    close()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Errore durante la chiamata a generateResponseAsync", e)
            close(e)
        }

        awaitClose { }
    }

    override suspend fun unload() {
        session?.close()
        llmInference?.close()
        session = null
        llmInference = null
        Log.d(tag, "Motore e sessione Gemma rilasciati.")
    }
}
