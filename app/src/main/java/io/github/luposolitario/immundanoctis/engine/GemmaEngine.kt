package io.github.luposolitario.immundanoctis.engine

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import io.github.luposolitario.immundanoctis.util.GemmaPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import java.io.File

/**
 * Implementazione di InferenceEngine che usa la libreria MediaPipe per i modelli Gemma.
 * AGGIORNATA per usare il corretto flusso con LlmInferenceSession.
 */
class GemmaEngine(private val context: Context) : InferenceEngine {
    private val tag = "GemmaEngine"
    private var llmInference: LlmInference? = null
    private var session: LlmInferenceSession? = null
    private var sessionOptions: LlmInferenceSession.LlmInferenceSessionOptions? = null
    private val maxTokens = 4096
    private var totalTokensUsed: Int = 0
    private var currentModelPath: String? = null

    private val gemmaPreferences = GemmaPreferences(context)

    private val _tokenInfo = MutableStateFlow(
        TokenInfo(0, maxTokens, TokenStatus.GREEN, 0)
    )
    override val tokenInfo: StateFlow<TokenInfo> = _tokenInfo.asStateFlow()

    companion object {
        const val TOKEN_LIMIT_REACHED_SIGNAL = "{TOKEN_LIMIT_REACHED}"
    }

    override suspend fun load(modelPath: String, chatbotPersonality: String?) {
        currentModelPath = modelPath
        try {
            if (!File(modelPath).exists()) {
                val errorMessage = "Modello Gemma non trovato: $modelPath"
                Log.e(tag, errorMessage)
                throw IllegalStateException(errorMessage) // RILANCIA L'ECCEZIONE
            }

            val inferenceOptions = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(gemmaPreferences.nLen)
                .build()
            llmInference = LlmInference.createFromOptions(context, inferenceOptions)

            sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(gemmaPreferences.topK)
                .setTemperature(gemmaPreferences.temperature)
                .setTopP(gemmaPreferences.topP)
                .build()

            logParameters()

            session = LlmInferenceSession.createFromOptions(llmInference, sessionOptions)
            Log.d(tag, "Motore e sessione Gemma caricati con successo.")
        } catch (e: Exception) {
            Log.e(tag, "Errore durante il caricamento del modello o della sessione Gemma.", e)
            session = null
            llmInference = null
            currentModelPath = null
            throw e // RILANCIA L'ECCEZIONE
        }
    }

    private fun logParameters() {
        Log.i(tag, "GemmaEngine configurato con i seguenti parametri:")
        Log.i(tag, " - Temperatura: ${gemmaPreferences.temperature}")
        Log.i(tag, " - Top-K: ${gemmaPreferences.topK}")
        Log.i(tag, " - Top-P: ${gemmaPreferences.topP}")
    }

    override suspend fun resetSession(systemPrompt: String?) {
        if (llmInference == null ) {
            Log.e(tag, "Il motore non è inizializzato, impossibile resettare la sessione.")
            return
        }

        try {
            session?.close()
            session = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions!!)
            totalTokensUsed = 0
            if (!systemPrompt.isNullOrBlank()) {
                session?.addQueryChunk(systemPrompt)
                totalTokensUsed += session?.sizeInTokens(systemPrompt) ?: 0
                Log.d(tag, "System prompt iniettato.")
            }
            updateTokenCount()
            Log.d(tag, "Sessione resettata con successo.")
        } catch (e: Exception) {
            Log.e(tag, "Errore durante il reset della sessione.", e)
            throw e // RILANCIA L'ECCEZIONE
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

        val inputTokens = session!!.sizeInTokens(text)
        val fullResponse = StringBuilder()

        try {
            session!!.addQueryChunk(text)

            session!!.generateResponseAsync { partialResponse, done ->
                if (isActive) {
                    partialResponse?.let {
                        fullResponse.append(it)
                        trySend(it)
                    }
                }
                if (done) {
                    val outputTokens = session!!.sizeInTokens(fullResponse.toString())
                    totalTokensUsed += inputTokens + outputTokens
                    updateTokenCount()

                    if (fullResponse.isEmpty() || totalTokensUsed >= maxTokens) {
                        Log.w(tag, "Limite token raggiunto o risposta vuota. Invio segnale di reset.")
                        trySend(TOKEN_LIMIT_REACHED_SIGNAL)
                    }
                    close()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Errore durante la chiamata a generateResponseAsync", e)
            close(e)
        }

        awaitClose {
            Log.d(tag, "Flow per sendMessage chiuso.")
        }
    }

    private fun updateTokenCount() {
        val currentTokens = totalTokensUsed
        val percentage = if (maxTokens > 0) (currentTokens * 100) / maxTokens else 0

        val newStatus = when {
            percentage >= 95 -> TokenStatus.CRITICAL
            percentage > 60 -> TokenStatus.RED
            percentage >= 33 -> TokenStatus.YELLOW
            else -> TokenStatus.GREEN
        }

        _tokenInfo.value = TokenInfo(currentTokens, maxTokens, newStatus, percentage)
        Log.d(tag, "Token count: $currentTokens/$maxTokens ($percentage%) - Status: $newStatus")
    }

    override suspend fun unload() {
        try {
            session?.close()
            llmInference?.close()
        } catch (e: Exception) {
            Log.e(tag, "Errore durante il rilascio delle risorse.", e)
        } finally {
            session = null
            llmInference = null
            currentModelPath = null
            totalTokensUsed = 0
            updateTokenCount()
            Log.d(tag, "Motore e sessione Gemma rilasciati.")
        }
    }

    override fun getTokensUsed(): Int {
        return totalTokensUsed
    }
}