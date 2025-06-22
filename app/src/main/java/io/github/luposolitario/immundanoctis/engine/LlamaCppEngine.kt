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

/**
 * Implementazione di InferenceEngine che usa il modulo :llama (llama.cpp).
 */
class LlamaCppEngine(private val context: Context) : InferenceEngine {
    private val llama: LLamaAndroid = LLamaAndroid.instance()
    private val tag = "LlamaCppEngine"
    private var currentModelPath: String? = null
    // Definiamo un valore massimo di token per coerenza con GemmaEngine.
    private val maxTokens = 4096
    private var totalTokensUsed = 0
    // Aggiungi questa riga
    private val llamaPreferences = LlamaPreferences(context)

    // --- StateFlow per esporre le informazioni sui token alla UI (richiesto dall'interfaccia) ---
    private val _tokenInfo = MutableStateFlow(
        TokenInfo(0, maxTokens, TokenStatus.GREEN, 0)
    )
    override val tokenInfo: StateFlow<TokenInfo> = _tokenInfo.asStateFlow()

    override suspend fun load(modelPath: String) {
        currentModelPath = modelPath
        totalTokensUsed = 0 // Reset del contatore quando si carica un nuovo modello
        updateTokenCount() // Aggiorna la UI allo stato iniziale

        // Imposta il parametro nLen usando le preferenze
        llama.nlen = llamaPreferences.nLen
        Log.i(tag, "Llama GGUF configurato con nLen (max tokens): ${llama.nlen}")

        llama.load(modelPath)
        Log.d(tag, "Modello LlamaCpp caricato: $modelPath")
    }

    override fun sendMessage(text: String): Flow<String> {
        // Il tuo LLamaAndroid.send formatta già come chat? Se no, dovremmo passare true.
        // Per ora lo lascio a false come nel codice originale.
        return llama.send(message = text, formatChat = false)
    }

    /**
     * Resetta la sessione. Per llama.cpp, il modo più sicuro è ricaricare il modello.
     * Gestisce anche l'iniezione di un system prompt.
     */
    override suspend fun resetSession(systemPrompt: String?) {
        try {
            currentModelPath?.let { modelPath ->
                Log.d(tag, "Reset sessione LlamaCpp in corso...")

                // Unload e reload per un reset completo
                llama.unload()
                llama.load(modelPath)

                // Reset del contatore token
                totalTokensUsed = 0

                // Iniezione del System Prompt (se presente)
                if (!systemPrompt.isNullOrBlank()) {
                    // Per llama.cpp, il system prompt viene gestito formattando la chat,
                    // quindi lo aggiungiamo al conteggio dei token iniziali.
                    totalTokensUsed += estimateTokens(systemPrompt)
                    Log.d(tag, "System prompt considerato nel nuovo conteggio token.")
                }

                updateTokenCount() // Aggiorna la UI
                Log.d(tag, "Sessione LlamaCpp resettata con successo")
            } ?: run {
                Log.w(tag, "Impossibile resettare la sessione: percorso del modello non disponibile")
                totalTokensUsed = 0
                updateTokenCount()
            }
        } catch (e: Exception) {
            Log.e(tag, "Errore durante il reset della sessione LlamaCpp", e)
            totalTokensUsed = 0
            updateTokenCount()
        }
    }

    override suspend fun unload() {
        llama.unload()
        currentModelPath = null
        totalTokensUsed = 0
        updateTokenCount() // Resetta la UI
        Log.d(tag, "Modello LlamaCpp scaricato")
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

        // Stima basata su caratteristiche del testo:
        // - ~4 caratteri per token per testo inglese/latino
        // - Aggiustamento per spazi e punteggiatura
        val baseTokens = (text.length / 4.0).roundToInt()
        val wordCount = text.split("\\s+".toRegex()).size

        return maxOf(1, (baseTokens + wordCount * 0.1).roundToInt())
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
