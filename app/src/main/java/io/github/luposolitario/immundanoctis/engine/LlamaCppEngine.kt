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
import java.util.LinkedList

/**
 * Implementazione di InferenceEngine che usa il modulo :llama (llama.cpp).
 * AGGIORNATA per usare il corretto flusso con LlmInferenceSession.
 */
class LlamaCppEngine(private val context: Context) : InferenceEngine {

    // Questa è una data class di esempio per rappresentare un messaggio di chat.
// Assicurati che corrisponda alla tua classe ChatMessage.
    data class Message(val role: String, val content: String)
    private val llama: LLamaAndroid = LLamaAndroid.instance()
    private val tag = "LlamaCppEngine"
    private var currentModelPath: String? = null
    // MaxTokens sarà ora letto dalle preferenze, non fisso
    private var maxTokens: Int = 4096 // Valore predefinito, verrà aggiornato dal caricamento
    private var totalTokensUsed = 0
    private val llamaPreferences = LlamaPreferences(context)
    private var chatHistory: LinkedList <Message> = LinkedList()

    // Campo per conservare il prompt di sistema/personalità, formattato per il template DarkIdol
    private var currentSystemPromptFormatted: String? = null

    private val _tokenInfo = MutableStateFlow(
        TokenInfo(0, maxTokens, TokenStatus.GREEN, 0)
    )
    override val tokenInfo: StateFlow<TokenInfo> = _tokenInfo.asStateFlow()

    override suspend fun load(modelPath: String, chatbotPersonality: String?) {
        currentModelPath = modelPath
        // Aggiorna maxTokens qui basandosi su LlamaPreferences
        maxTokens = llamaPreferences.nLen // Usa nLen dalle preferenze come maxTokens
        totalTokensUsed = 0
        updateTokenCount()

        if (!chatbotPersonality.isNullOrBlank()) {
            // Applica il template per il messaggio di sistema del modello DarkIdol
            currentSystemPromptFormatted =   chatbotPersonality
            Log.d(tag, "Inizializzato template per chat con personality: $chatbotPersonality")
        }

        chatHistory.add(Message("system", currentSystemPromptFormatted ?: ""))

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
        Log.i(tag, " - Repeat-P: ${llamaPreferences.repeatP}")
        Log.i(tag, " - nLen (Max Tokens): ${llamaPreferences.nLen}")
    }

    override fun sendMessage(text: String): Flow<String> {
        if (currentModelPath == null) {
            val errorMessage = "[ERRORE: Sessione di chat con Llama non inizializzata]"
            Log.e(tag, errorMessage)
            return kotlinx.coroutines.flow.flowOf(errorMessage)
        }

        // Il modello genererà la risposta e poi </s>
        var messageToSend =  buildPromptWithHistory(text)//fullPromptBuilder.toString()
        Log.d(tag, "DEBUG_PROMPT: Prompt finale inviato a LLama (Llama 2): \n---\n${messageToSend}\n---")

        // Estima i token dell'intero messaggio che verrà inviato
        val inputTokens = estimateTokens(messageToSend)
        val fullResponse = StringBuilder()

        return llama.send(message = messageToSend, formatChat = false)
            .onEach { partialResponse ->
                fullResponse.append(partialResponse)
            }
            .onCompletion {
                val outputTokens = estimateTokens(fullResponse.toString())
                chatHistory.add(Message("assistant", fullResponse.toString()))
                totalTokensUsed += (inputTokens + outputTokens)
                updateTokenCount()
            Log.d(tag, "DEBUG_FLOW: Token utilizzati in questo messaggio: input=$inputTokens, output=$outputTokens, totale sessione=$totalTokensUsed")
            }
    }

    /**
     * Resetta la sessione. Per llama.cpp, il modo più sicuro è ricaricare il modello.
     * Gestisce anche l'iniezione di un system prompt.
     */
    override suspend fun resetSession(systemPrompt: String?) {
        if (currentModelPath == null) { // Aggiunto controllo per currentModelPath
            val errorMessage = "Impossibile resettare la sessione: percorso del modello non disponibile."
            Log.w(tag, errorMessage)
            // Non rilanciare l'eccezione se il percorso non è disponibile qui, ma pulisci lo stato
            totalTokensUsed = 0
            currentSystemPromptFormatted = null
            updateTokenCount()
            return
        }

        try {
            Log.d(tag, "Reset sessione LlamaCpp in corso...")

            llama.unload()
            llama.load(
                pathToModel = currentModelPath!!, // currentModelPath non sarà nullo qui grazie al controllo iniziale
                temperature = llamaPreferences.temperature,
                repeatPenalty = llamaPreferences.repeatP,
                topK = llamaPreferences.topK,
                topP = llamaPreferences.topP
            )

            // Resetta il system prompt formattato
            currentSystemPromptFormatted = null

            // Se è fornito un prompt di sistema, formattalo e conservalo
            if (!systemPrompt.isNullOrBlank()) {
                // Applica il template per il messaggio di sistema del modello DarkIdol
                currentSystemPromptFormatted =   systemPrompt
                // Stima i token del system prompt e aggiungili al totale usato.
                totalTokensUsed = estimateTokens(currentSystemPromptFormatted!!)
                Log.d(tag, "System prompt iniettato e considerato nel conteggio token iniziale: $totalTokensUsed")
            } else {
                totalTokensUsed = 0 // Nessun system prompt, inizia da 0 token
            }

            updateTokenCount()
            Log.d(tag, "Sessione LlamaCpp resettata con successo. System Prompt: ${currentSystemPromptFormatted?.take(50)}...")
        } catch (e: Exception) {
            Log.e(tag, "Errore durante il reset della sessione LlamaCpp", e)
            totalTokensUsed = 0 // In caso di errore, resetta i token
            currentSystemPromptFormatted = null // In caso di errore, resetta la personalità
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
            currentSystemPromptFormatted = null // Resetta anche la personalità
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
     * Costruisce una stringa di prompt completa includendo la cronologia della chat.
     *
     * @param messages La lista dei messaggi precedenti (cronologia).
     * @param newUserInput Il nuovo messaggio dell'utente.
     * @return Una singola stringa formattata secondo il template del modello, pronta per l'invio.
     */
    fun buildPromptWithHistory( newUserInput: String): String {
        val promptBuilder = StringBuilder()
        // Il template gestisce il messaggio di sistema solo se è il primo
        val systemMessage = chatHistory.firstOrNull { it.role == "system" }
        if (systemMessage != null) {
            // Aggiungi il system prompt formattato
            promptBuilder.append("SYSTEM: ${systemMessage.content}\n")
        }

        // Itera attraverso i messaggi precedenti e li formatta
        for (message in chatHistory) {
            when (message.role) {
                "user" -> {
                    // Il template aggiunge "ASSISTANT:" alla fine del turno Utente
                    promptBuilder.append("USER: ${message.content} ASSISTANT:")
                }
                "assistant" -> {
                    // Il template aggiunge "</s>" alla fine del turno Assistant
                    promptBuilder.append("${message.content}</s>")
                }
            }
        }

        // Aggiungi il nuovo input dell'utente e il segnale per l'assistente
        promptBuilder.append("USER: $newUserInput ASSISTANT:")

        return promptBuilder.toString()
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