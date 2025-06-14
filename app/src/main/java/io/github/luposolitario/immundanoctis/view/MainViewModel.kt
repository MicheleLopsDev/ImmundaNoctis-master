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
import kotlinx.coroutines.Job // <-- IMPORTANTE: Aggiungi questo import

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val tag: String? = this::class.simpleName

    private val dmEngine: InferenceEngine = GemmaEngine(application.applicationContext)
    private val playerEngine: InferenceEngine = LlamaCppEngine()

    // 1. AGGIUNGI UNA VARIABILE PER MEMORIZZARE IL TASK DI GENERAZIONE
    private var generationJob: Job? = null

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
            stopGeneration() // Annulla anche qui per sicurezza
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
        if (_isGenerating.value) {
            log("Generazione già in corso, richiesta ignorata.")
            return
        }

        val userMessage = ChatMessage(authorId = CharacterID.HERO, text = text)
        _chatMessages.update { it + userMessage }

        val targetId = _conversationTargetId.value

        // --- MODIFICA CHIAVE QUI ---
        // Se il bersaglio è il DM OPPURE l'EROE stesso, usa il motore del DM.
        // Altrimenti, usa il motore dei PG (per interazioni tra giocatori).
        val engineToUse = if (targetId == CharacterID.DM || targetId == CharacterID.HERO) {
            dmEngine
        } else {
            playerEngine
        }

        val logMessage = if (targetId == CharacterID.HERO) {
            "Invio prompt a 'HERO' (gestito dal DM)..."
        } else {
            "Invio prompt a '$targetId'..."
        }
        log(logMessage)

        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            _streamingText.value = ""
            // Chi risponde è sempre il personaggio selezionato
            _respondingCharacterId.value = targetId

            try {
                // Se il bersaglio è l'eroe, il DM deve sapere a chi sta rispondendo
                val prompt = if (targetId == CharacterID.HERO) {
                    // Puoi formattare il prompt per dare più contesto al DM
                    "L'eroe (hero) pensa o dice a se stesso: '$text'"
                } else {
                    text
                }
                engineToUse.sendMessage(prompt)
                    .collect { token ->
                        _streamingText.update { it + token }
                    }
            } catch (e: Exception) {
                Log.e(tag, "Errore durante la raccolta del flow di messaggi", e)
                log("Errore: ${e.message}")
            } finally {
                log("Generazione per '$targetId' completata o interrotta.")
                if (_streamingText.value.isNotBlank()) {
                    // L'autore della risposta è il bersaglio originale
                    val finalMessage = ChatMessage(authorId = targetId, text = _streamingText.value)
                    _chatMessages.update { it + finalMessage }
                }
                _isGenerating.value = false
                _streamingText.value = ""
                _respondingCharacterId.value = null
            }
        }
    }


    // 3. AGGIUNGI QUESTA NUOVA FUNZIONE PUBBLICA
    fun stopGeneration() {
        if (generationJob?.isActive == true) {
            log("Cancellazione del task di generazione in corso...")
            generationJob?.cancel()
        }
    }

    fun log(message: String) {
        _logMessages.update { it + message }
    }
}
