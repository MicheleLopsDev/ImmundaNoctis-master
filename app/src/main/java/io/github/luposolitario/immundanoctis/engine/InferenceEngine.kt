package io.github.luposolitario.immundanoctis.engine

import kotlinx.coroutines.flow.Flow

/**
 * Un'interfaccia generica che definisce le capacità di un motore di inferenza LLM.
 * Qualsiasi motore (Llama.cpp, Gemma, ecc.) dovrà implementare questo "contratto".
 */
interface InferenceEngine {
    /**
     * Carica un modello da un percorso specifico.
     */
    suspend fun load(modelPath: String)

    /**
     * Invia un prompt al modello e restituisce la risposta come un flusso di token.
     */
    fun sendMessage(text: String): Flow<String>

    /**
     * Rilascia le risorse del modello caricato.
     */
    suspend fun unload()
}
