package io.github.luposolitario.immundanoctis.view

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.luposolitario.immundanoctis.data.ChatMessage
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.engine.GemmaEngine
import io.github.luposolitario.immundanoctis.engine.InferenceEngine
import io.github.luposolitario.immundanoctis.engine.LlamaCppEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val tag: String? = this::class.simpleName

    private val dmEngine: InferenceEngine = GemmaEngine(application.applicationContext)
    private val playerEngine: InferenceEngine = LlamaCppEngine()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _respondingCharacterId = MutableStateFlow<String?>(null)
    val respondingCharacterId: StateFlow<String?> = _respondingCharacterId.asStateFlow()

    private val _logMessages = MutableStateFlow<List<String>>(listOf("ViewModel Inizializzato."))
    val logMessages: StateFlow<List<String>> = _logMessages.asStateFlow()

    // Stato per il personaggio BERSAGLIO della conversazione
    private val _conversationTargetId = MutableStateFlow(CharacterID.DM)
    val conversationTargetId: StateFlow<String> = _conversationTargetId.asStateFlow()

    // NOTA: 'currentPlayerId' è stato rinominato in 'conversationTargetId' per chiarezza.

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            log("Rilascio risorse motori...")
            dmEngine.unload()
            playerEngine.unload()
            log("Motori rilasciati.")
        }
    }

    /**
     * Imposta il personaggio a cui l'utente sta parlando.
     */
    fun setConversationTarget(characterId: String) {
        _conversationTargetId.value = characterId
        log("Ora stai parlando con: $characterId")
    }

    fun loadEngines(dmModelPath: String?, playerModelPath: String?) {
        viewModelScope.launch {
            dmModelPath?.let {
                log("Caricamento modello DM (Gemma)...")
                dmEngine.load(it)
            } ?: log("Nessun percorso per il modello DM.")

            playerModelPath?.let {
                log("Caricamento modello PG (GGUF)...")
                playerEngine.load(it)
            } ?: log("Nessun percorso per il modello PG.")
        }
    }

    /**
     * Invia un messaggio DALL'EROE al personaggio BERSAGLIO e genera la sua risposta.
     */
    fun sendMessage(text: String) {
        if (_isGenerating.value) return

        val userMessage = ChatMessage(authorId = CharacterID.HERO, text = text)
        _chatMessages.update { it + userMessage }

        val targetId = _conversationTargetId.value
        val engineToUse = if (targetId == CharacterID.DM) dmEngine else playerEngine

        log("Invio prompt al motore di '$targetId'...")

        viewModelScope.launch {
            _isGenerating.value = true
            _streamingText.value = ""
            _respondingCharacterId.value = targetId

            engineToUse.sendMessage(text)
                .catch { error ->
                    Log.e(tag, "sendMessage() failed", error)
                    log(error.message ?: "Errore sconosciuto.")
                }
                .onCompletion {
                    if (_streamingText.value.isNotBlank()) {
                        val finalMessage = ChatMessage(authorId = targetId, text = _streamingText.value)
                        _chatMessages.update { it + finalMessage }
                    }
                    _streamingText.value = ""
                    _isGenerating.value = false
                    _respondingCharacterId.value = null
                    log("Risposta generata da '$targetId'.")
                }
                .collect { token ->
                    _streamingText.update { it + token }
                }
        }
    }

    fun log(message: String) {
        _logMessages.update { it + message }
    }
}
