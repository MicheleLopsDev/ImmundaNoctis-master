package io.github.luposolitario.immundanoctis.engine

import android.llama.cpp.LLamaAndroid
import kotlinx.coroutines.flow.Flow

/**
 * Implementazione di InferenceEngine che usa il modulo :llama (llama.cpp).
 */
class LlamaCppEngine(private val llama: LLamaAndroid = LLamaAndroid.instance()) : InferenceEngine {

    override suspend fun load(modelPath: String) {
        llama.load(modelPath)
    }

    override fun sendMessage(text: String): Flow<String> {
        // Il tuo LLamaAndroid.send formatta già come chat? Se no, dovremmo passare true.
        // Per ora lo lascio a false come nel codice originale.
        return llama.send(message = text, formatChat = false)
    }

    override suspend fun unload() {
        llama.unload()
    }
}
