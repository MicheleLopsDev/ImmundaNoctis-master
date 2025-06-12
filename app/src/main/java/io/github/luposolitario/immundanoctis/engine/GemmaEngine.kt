package io.github.luposolitario.immundanoctis.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Implementazione (placeholder) di InferenceEngine che userà MediaPipe LlmInference.
 * Per ora, i suoi metodi sono vuoti o restituiscono valori di default.
 */
class GemmaEngine(private val context: Context) : InferenceEngine {
    private val tag = "GemmaEngine"

    override suspend fun load(modelPath: String) {
        Log.d(tag, "Placeholder: Caricamento modello Gemma da: $modelPath")
        // Qui andrà la logica con LlmInference.createFromFile(context, modelPath)
    }

    override fun sendMessage(text: String): Flow<String> {
        Log.d(tag, "Placeholder: Invio messaggio a Gemma: $text")
        // Qui andrà la chiamata a llmInference.generateResponseAsFlow(text)
        return flowOf(" (Risposta di Gemma non ancora implementata) ")
    }

    override suspend fun unload() {
        Log.d(tag, "Placeholder: Scaricamento modello Gemma.")
        // Qui andrà la chiamata a llmInference.close()
    }
}
