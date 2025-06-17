package io.github.luposolitario.immundanoctis.engine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Contiene le informazioni aggiornate sul consumo dei token.
 * Spostato qui per essere accessibile da tutta l'app.
 */
data class TokenInfo(
    val count: Int,
    val maxTokens: Int,
    val status: TokenStatus,
    val percentage: Int
)

/**
 * Stato del consumo dei token, usato per il "semaforo" nella UI.
 * Spostato qui per essere accessibile da tutta l'app.
 */
enum class TokenStatus {
    GREEN, // Fino al 33%
    YELLOW, // Dal 34% al 60%
    RED, // Oltre il 60%
    CRITICAL // Oltre il 95% (per l'avviso popup)
}


/**
 * Un'interfaccia generica che definisce le capacità di un motore di inferenza LLM.
 * VERSIONE FINALE CORRETTA.
 */
interface InferenceEngine {
    /**
     * Un flusso reattivo che espone lo stato attuale del consumo dei token.
     * La UI può osservare questo StateFlow per aggiornare il semaforo in tempo reale.
     */
    val tokenInfo: StateFlow<TokenInfo>

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

    /**
     * Resetta la sessione di chat corrente e, opzionalmente, la prepara
     * con un prompt di sistema per darle un contesto iniziale.
     */
    suspend fun resetSession(systemPrompt: String? = null)

    /**
     * Restituisce il numero di token utilizzati nella sessione corrente.
     * Utile per logica non reattiva.
     */
    fun getTokensUsed(): Int
}
