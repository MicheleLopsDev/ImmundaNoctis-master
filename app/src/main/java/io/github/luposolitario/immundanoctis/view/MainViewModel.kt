package io.github.luposolitario.immundanoctis.view

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import io.github.luposolitario.immundanoctis.data.ChatMessage
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.data.ExportedMessage
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.engine.*
import io.github.luposolitario.immundanoctis.util.EnginePreferences
import io.github.luposolitario.immundanoctis.util.GameStateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val tag: String? = this::class.simpleName

    // --- 1. AGGIUNGI QUESTO STATEFLOW SOTTO GLI ALTRI ---
    private val _sessionName = MutableStateFlow("Immunda Noctis")
    val sessionName: StateFlow<String> = _sessionName.asStateFlow()

    // --- NUOVO GESTORE DI STATO ---
    private val gameStateManager = GameStateManager(application)

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // --- NUOVO STATEFLOW PER I PERSONAGGI ---
    private val _gameCharacters = MutableStateFlow<List<GameCharacter>>(emptyList())
    val gameCharacters: StateFlow<List<GameCharacter>> = _gameCharacters.asStateFlow()


    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _respondingCharacterId = MutableStateFlow<String?>(null)
    val respondingCharacterId: StateFlow<String?> = _respondingCharacterId.asStateFlow()

    private val _logMessages = MutableStateFlow<List<String>>(listOf("ViewModel Inizializzato."))
    val logMessages: StateFlow<List<String>> = _logMessages.asStateFlow()

    private val _conversationTargetId = MutableStateFlow(CharacterID.DM)
    val conversationTargetId: StateFlow<String> = _conversationTargetId.asStateFlow()

    private val _saveChatEvent = MutableSharedFlow<String>()
    val saveChatEvent: SharedFlow<String> = _saveChatEvent.asSharedFlow()

    private var generationJob: Job? = null

    private val enginePreferences = EnginePreferences(application)
    private val useGemmaForAll = enginePreferences.useGemmaForAll

    private val dmEngine: InferenceEngine
    private val playerEngine: InferenceEngine
    private val translationEngine = TranslationEngine()

    init {
        if (useGemmaForAll) {
            log("Modalità Solo Gemma ATTIVA.")
            dmEngine = GemmaEngine(application.applicationContext)
            playerEngine = dmEngine
        } else {
            log("Modalità Mista ATTIVA (Gemma per DM, GGUF per PG).")
            dmEngine = GemmaEngine(application.applicationContext)
            playerEngine = LlamaCppEngine()
        }
    }

    /**
     * NUOVA FUNZIONE: Carica la sessione di gioco (o ne crea una di default)
     * e popola lo StateFlow dei personaggi.
     */

    fun loadGameSession() {
        val session = gameStateManager.loadSession() ?: gameStateManager.createDefaultSession()
        _gameCharacters.value = session.characters
        // --- 2. AGGIORNA IL NOME DELLA SESSIONE QUI ---
        _sessionName.value = session.sessionName
        log("Sessione di gioco caricata: ${session.sessionName}")
    }



    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            log("Rilascio risorse motori...")
            stopGeneration()
            dmEngine.unload()
            playerEngine.unload()
            translationEngine.close()
            log("Motori rilasciati.")
        }
    }

    fun loadEngines(dmModelPath: String?, playerModelPath: String?) {
        viewModelScope.launch {
            dmModelPath?.let {
                log("Tentativo di caricamento modello DM (Gemma)...")
                dmEngine.load(it)
            } ?: log("Nessun percorso per il modello DM.")

            if (!useGemmaForAll) {
                playerModelPath?.let {
                    log("Tentativo di caricamento modello PG (GGUF)...")
                    playerEngine.load(it)
                } ?: log("Nessun percorso per il modello PG.")
            } else {
                log("Modalità Solo Gemma attiva, caricamento modello GGUF saltato.")
            }
            log("Processo di caricamento motori completato.")
        }
    }

    fun translateMessage(messageId: String) {
        viewModelScope.launch {
            val originalMessage = _chatMessages.value.find { it.id == messageId } ?: return@launch
            updateMessage(messageId) { it.copy(isTranslating = true) }
            try {
                val lines = originalMessage.text.split('\n')
                val translatedLines = lines.map { line ->
                    if (line.isBlank()) {
                        async { "" }
                    } else {
                        async { translationEngine.translate(line) }
                    }
                }.awaitAll()
                val finalTranslation = translatedLines.joinToString("\n")
                updateMessage(messageId) {
                    it.copy(translatedText = finalTranslation, isTranslating = false)
                }
                log("Traduzione completata per il messaggio ID: $messageId")
            } catch (e: Exception) {
                log("Errore di traduzione: ${e.message}")
                updateMessage(messageId) { it.copy(isTranslating = false) }
            }
        }
    }

    private fun updateMessage(messageId: String, transformation: (ChatMessage) -> ChatMessage) {
        _chatMessages.update { currentMessages ->
            currentMessages.map { if (it.id == messageId) transformation(it) else it }
        }
    }

    /**
     * MODIFICATA: Ora prende i personaggi direttamente dallo stato interno del ViewModel.
     */
    fun onSaveChatClicked() {
        viewModelScope.launch {
            val characters = _gameCharacters.value
            val exportedMessages = _chatMessages.value.map { chatMessage ->
                val authorName = characters.find { it.id == chatMessage.authorId }?.name ?: "Sconosciuto"
                ExportedMessage(author = authorName, message = chatMessage.text)
            }
            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonString = gson.toJson(exportedMessages)
            _saveChatEvent.emit(jsonString)
            log("Contenuto JSON della chat pronto per il salvataggio.")
        }
    }

    fun sendMessage(text: String) {
        if (_isGenerating.value) {
            log("Generazione già in corso, richiesta ignorata.")
            return
        }

        val userMessage = ChatMessage(authorId = CharacterID.HERO, text = text)
        _chatMessages.update { it + userMessage }

        var targetId = _conversationTargetId.value

        // --- INIZIO DELLA CORREZIONE ---

        // LOGICA SEMPLIFICATA:
        // Usiamo SEMPRE il motore principale (dmEngine) per mantenere uno stile narrativo coerente.
        // Il motore è abbastanza intelligente da impersonare il personaggio target.
        val engineToUse = dmEngine

        val logMessage = "Invio prompt per una risposta da '$targetId'..."
        log(logMessage)

        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            _streamingText.value = ""
            _respondingCharacterId.value = targetId
            try {
                // Il prompt non ha più bisogno di casi speciali.
                // Sarà il "System Prompt" (che miglioreremo in futuro) a dare le istruzioni.
                engineToUse.sendMessage(text)
                    .collect { token ->
                        _streamingText.update { it + token }
                    }
            } catch (e: Exception) {
                Log.e(tag, "Errore durante la raccolta del flow di messaggi", e)
                log("Errore: ${e.message}")
            } finally {
                log("Generazione per '$targetId' completata o interrotta.")
                if (_streamingText.value.isNotBlank()) {
                    val finalMessage = ChatMessage(authorId = targetId, text = _streamingText.value)
                    _chatMessages.update { it + finalMessage }
                }
                _isGenerating.value = false
                _streamingText.value = ""
                _respondingCharacterId.value = null // Resettiamo il responding character
            }
        }
        // --- FINE DELLA CORREZIONE ---
    }

    fun stopGeneration() {
        if (generationJob?.isActive == true) {
            log("Cancellazione del task di generazione in corso...")
            generationJob?.cancel()
        }
    }

    fun setConversationTarget(characterId: String) {
        _conversationTargetId.value = characterId
        log("Ora stai parlando con: $characterId")
    }

    fun log(message: String) {
        _logMessages.update { it + message }
    }
}
