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
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val tag: String? = this::class.simpleName

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
        // Non facciamo più nessun controllo qui, perché ora ogni motore
        // è responsabile di non ricaricare se stesso se è già attivo.
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

    // ... (tutte le altre funzioni del ViewModel rimangono invariate) ...

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

    fun onSaveChatClicked(characters: List<GameCharacter>) {
        viewModelScope.launch {
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

        val targetId = _conversationTargetId.value
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
            _respondingCharacterId.value = targetId
            try {
                val prompt = if (targetId == CharacterID.HERO) {
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
                    val finalMessage = ChatMessage(authorId = targetId, text = _streamingText.value)
                    _chatMessages.update { it + finalMessage }
                }
                _isGenerating.value = false
                _streamingText.value = ""
                _respondingCharacterId.value = null
            }
        }
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
