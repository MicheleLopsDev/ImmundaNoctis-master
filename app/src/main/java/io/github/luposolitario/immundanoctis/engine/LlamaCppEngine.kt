// io/github/luposolitario/immundanoctis/engine/LlamaCppEngine.kt
package io.github.luposolitario.immundanoctis.engine

import android.content.Context
import android.llama.cpp.LLamaAndroid
import android.util.Log
import io.github.luposolitario.immundanoctis.util.LlamaPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlin.math.roundToInt
import java.io.File

/**
 * Implementazione di InferenceEngine che usa il modulo :llama (llama.cpp).
 * QUESTA VERSIONE INTEGRA LA NUOVA INTERFACCIA E IL SEMAFORO DEI TOKEN.
 */
class LlamaCppEngine(private val context: Context) : InferenceEngine {
    private val llama: LLamaAndroid = LLamaAndroid.instance()
    private val tag = "LlamaCppEngine"
    private var currentModelPath: String? = null
    // MaxTokens sarà ora letto dalle preferenze, non fisso
    private var maxTokens: Int = 4096 // Valore predefinito, verrà aggiornato dal caricamento
    private var totalTokensUsed = 0
    private val llamaPreferences = LlamaPreferences(context)

    // NUOVO: Campo per conservare il prompt di sistema/personalità
    private var currentSystemPrompt: String? = null

    private val _tokenInfo = MutableStateFlow(
        TokenInfo(0, maxTokens, TokenStatus.GREEN, 0)
    )
    override val tokenInfo: StateFlow<TokenInfo> = _tokenInfo.asStateFlow()

    override suspend fun load(modelPath: String) {
        currentModelPath = modelPath
        // Aggiorna maxTokens qui basandosi su LlamaPreferences
        maxTokens = llamaPreferences.nLen // Usa nLen dalle preferenze come maxTokens
        totalTokensUsed = 0
        updateTokenCount()

        try {
            if (!File(modelPath).exists()) {
                val errorMessage = "Modello Llama GGUF non trovato: $modelPath"
                Log.e(tag, errorMessage)
                throw IllegalStateException(errorMessage) // RILANCIA L'ECCEZIONE
            }

            // Imposta nlen qui, non in sendMessage
            llama.nlen = llamaPreferences.nLen
            Log.i(tag, "Llama GGUF configurato con nLen (max tokens): ${llama.nlen}")

            llama.load(
                pathToModel = modelPath,
                temperature = llamaPreferences.temperature,
                repeatPenalty = llamaPreferences.repeatP,
                topK = llamaPreferences.topK,
                topP = llamaPreferences.topP
            )
            Log.d(tag, "Modello LlamaCpp caricato: $modelPath")

            // Dopo aver caricato il modello, imposta il prompt di sistema iniziale
            // Lo faremo in resetSession per coerenza con l'interfaccia
            // ma se si carica un modello senza reset della sessione, si può fare qui.
            // Per ora, lo lasciamo a resetSession.

        } catch (e: Exception) {
            Log.e(tag, "Errore durante il caricamento del modello LlamaCpp.", e)
            currentModelPath = null
            totalTokensUsed = 0
            updateTokenCount()
            throw e // RILANCIA L'ECCEZIONE
        }
    }

    private fun logParameters() {
        Log.i(tag, "LlamaEngine configurato con i seguenti parametri:")
        Log.i(tag, " - Temperatura: ${llamaPreferences.temperature}")
        Log.i(tag, " - Top-K: ${llamaPreferences.topK}")
        Log.i(tag, " - Top-P: ${llamaPreferences.topP}")
        Log.i(tag, " - Repeat-P: ${llamaPreferences.repeatP}") // Aggiunto log per Repeat-P
        Log.i(tag, " - nLen (Max Tokens): ${llamaPreferences.nLen}") // Aggiunto log per nLen
    }

    override fun sendMessage(text: String): Flow<String> {
        if (currentModelPath == null) {
            val errorMessage = "[ERRORE: Sessione di chat con Llama non inizializzata]"
            Log.e(tag, errorMessage)
            return kotlinx.coroutines.flow.flowOf(errorMessage)
        }

        // Aggiungi il prompt di sistema all'inizio del messaggio
        val messageWithSystemPrompt = if (!currentSystemPrompt.isNullOrBlank()) {
            // Se currentSystemPrompt è già formattato, usalo così com'è
            currentSystemPrompt + text
        } else {
            text
        }

        // Estima i token dell'intero messaggio che verrà inviato, inclusa la personalità
        val inputTokens = estimateTokens(messageWithSystemPrompt)
        val fullResponse = StringBuilder()

        return llama.send(message = messageWithSystemPrompt, formatChat = false)
            .onEach { partialResponse ->
                fullResponse.append(partialResponse)
            }
            .onCompletion {
                val outputTokens = estimateTokens(fullResponse.toString())
                totalTokensUsed += (inputTokens + outputTokens)
                updateTokenCount()
                Log.d(tag, "Token utilizzati in questo messaggio: input=$inputTokens, output=$outputTokens, totale sessione=$totalTokensUsed")
            }
    }

    /**
     * Resetta la sessione. Per llama.cpp, il modo più sicuro è ricaricare il modello.
     * Gestisce anche l'iniezione di un system prompt.
     */
    override suspend fun resetSession(systemPrompt: String?) {
        try {
            currentModelPath?.let { modelPath ->
                Log.d(tag, "Reset sessione LlamaCpp in corso...")

                llama.unload()
                llama.load(
                    pathToModel = modelPath,
                    temperature = llamaPreferences.temperature,
                    repeatPenalty = llamaPreferences.repeatP,
                    topK = llamaPreferences.topK,
                    topP = llamaPreferences.topP
                )

                // Rimuovi il vecchio system prompt se presente
                currentSystemPrompt = null

                // Se è fornito un prompt di sistema, formattalo e conservalo
                if (!systemPrompt.isNullOrBlank()) {
                    // Applica il template Jinja2 per il messaggio di sistema
                    currentSystemPrompt = "<|im_start|>system" + systemPrompt + "<|im_end|>"
                    // Stima i token del system prompt e aggiungili al totale usato
                    // Questi token saranno presenti in ogni richiesta, quindi li conteggiamo qui all'inizio
                    totalTokensUsed = estimateTokens(currentSystemPrompt!!)
                    Log.d(tag, "System prompt iniettato e considerato nel conteggio token iniziale: $totalTokensUsed")
                } else {
                    totalTokensUsed = 0 // Nessun system prompt, inizia da 0 token
                }

                updateTokenCount()
                Log.d(tag, "Sessione LlamaCpp resettata con successo. System Prompt: ${currentSystemPrompt?.take(50)}...")
            } ?: run {
                val errorMessage = "Impossibile resettare la sessione: percorso del modello non disponibile"
                Log.w(tag, errorMessage)
                totalTokensUsed = 0 // In caso di errore, resetta i token
                updateTokenCount()
                throw IllegalStateException(errorMessage) // RILANCIA L'ECCEZIONE
            }
        } catch (e: Exception) {
            Log.e(tag, "Errore durante il reset della sessione LlamaCpp", e)
            totalTokensUsed = 0 // In caso di errore, resetta i token
            updateTokenCount()
            throw e // RILANCIA L'ECCEZIONE
        }
    }

    override suspend fun unload() {
        try {
            llama.unload()
        } catch (e: Exception) {
            Log.e(tag, "Errore durante il rilascio delle risorse LlamaCpp.", e)
        } finally {
            currentModelPath = null
            totalTokensUsed = 0
            currentSystemPrompt = null // Resetta anche la personalità
            updateTokenCount()
            Log.d(tag, "Modello LlamaCpp scaricato")
        }
    }

    override fun getTokensUsed(): Int {
        return totalTokensUsed
    }

    /**
     * Stima il numero di token in base al testo fornito.
     * Questa è una stima approssimativa che cerca di essere coerente
     * con quella utilizzata in GemmaEngine.
     */
    private fun estimateTokens(text: String): Int {
        if (text.isEmpty()) return 0
        // Una stima più robusta per i token. Llama.cpp spesso conta più come 0.75 parole per token.
        // E un minimo di 1 token per stringhe non vuote.
        val charBasedTokens = (text.length / 3.5).roundToInt() // Media di caratteri per token
        val wordCount = text.split("\\s+".toRegex()).size
        val roughEstimate = maxOf(1, (charBasedTokens + wordCount * 0.2).roundToInt())
        return roughEstimate
    }

    /**
     * Calcola la stima dei token usati e aggiorna lo StateFlow.
     */
    private fun updateTokenCount() {
        val currentTokens = totalTokensUsed
        val percentage = if (maxTokens > 0) (currentTokens * 100) / maxTokens else 0

        val newStatus = when {
            percentage >= 95 -> TokenStatus.CRITICAL
            percentage > 60 -> TokenStatus.RED
            percentage > 33 -> TokenStatus.YELLOW
            else -> TokenStatus.GREEN
        }

        _tokenInfo.value = TokenInfo(currentTokens, maxTokens, newStatus, percentage)
        Log.d(tag, "Token count (stimato): $currentTokens/$maxTokens ($percentage%) - Status: $newStatus")
    }
}