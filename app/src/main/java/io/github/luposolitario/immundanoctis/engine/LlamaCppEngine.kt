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
    private val maxTokens = 4096
    private var totalTokensUsed = 0
    private val llamaPreferences = LlamaPreferences(context)

    private val _tokenInfo = MutableStateFlow(
        TokenInfo(0, maxTokens, TokenStatus.GREEN, 0)
    )
    override val tokenInfo: StateFlow<TokenInfo> = _tokenInfo.asStateFlow()

    override suspend fun load(modelPath: String) {
        currentModelPath = modelPath
        totalTokensUsed = 0
        updateTokenCount()

        try {
            if (!File(modelPath).exists()) {
                val errorMessage = "Modello Llama GGUF non trovato: $modelPath"
                Log.e(tag, errorMessage)
                throw IllegalStateException(errorMessage) // RILANCIA L'ECCEZIONE
            }

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
    }

    override fun sendMessage(text: String): Flow<String> {
        if (currentModelPath == null) {
            val errorMessage = "[ERRORE: Sessione di chat con Llama non inizializzata]"
            Log.e(tag, errorMessage)
            return kotlinx.coroutines.flow.flowOf(errorMessage)
        }
        val inputTokens = estimateTokens(text)
        val fullResponse = StringBuilder()

        return llama.send(message = text, formatChat = false)
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

                totalTokensUsed = 0

                if (!systemPrompt.isNullOrBlank()) {
                    totalTokensUsed += estimateTokens(systemPrompt)
                    Log.d(tag, "System prompt considerato nel nuovo conteggio token.")
                }

                updateTokenCount()
                Log.d(tag, "Sessione LlamaCpp resettata con successo")
            } ?: run {
                val errorMessage = "Impossibile resettare la sessione: percorso del modello non disponibile"
                Log.w(tag, errorMessage)
                totalTokensUsed = 0
                updateTokenCount()
                throw IllegalStateException(errorMessage) // RILANCIA L'ECCEZIONE
            }
        } catch (e: Exception) {
            Log.e(tag, "Errore durante il reset della sessione LlamaCpp", e)
            totalTokensUsed = 0
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