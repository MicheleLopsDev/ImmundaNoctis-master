package io.github.luposolitario.immundanoctis.view

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.data.ChatMessage
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.engine.GemmaEngine
import io.github.luposolitario.immundanoctis.engine.InferenceEngine
import io.github.luposolitario.immundanoctis.engine.LlamaCppEngine
import io.github.luposolitario.immundanoctis.engine.TokenInfo
import io.github.luposolitario.immundanoctis.engine.TranslationEngine
import io.github.luposolitario.immundanoctis.util.EnginePreferences
import io.github.luposolitario.immundanoctis.util.GameStateManager
import io.github.luposolitario.immundanoctis.util.SavePreferences
import io.github.luposolitario.immundanoctis.util.getAppSpecificDirectory
import kotlinx.coroutines.Deferred // AGGIUNGI QUESTA LINEA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong


class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val tag: String? = this::class.simpleName

    sealed interface EngineLoadingState {
        data object Loading : EngineLoadingState
        data object Success : EngineLoadingState
        data class Error(val message: String?) : EngineLoadingState
    }

    private val _engineLoadingState =
        MutableStateFlow<EngineLoadingState>(EngineLoadingState.Loading)
    val engineLoadingState: StateFlow<EngineLoadingState> = _engineLoadingState.asStateFlow()

    var isPickingForDm: Boolean = false

    private val _sessionName = MutableStateFlow("Immunda Noctis")
    val sessionName: StateFlow<String> = _sessionName.asStateFlow()

    private val gameStateManager = GameStateManager(application)
    private val enginePreferences = EnginePreferences(application)

    private val savePreferences = SavePreferences(application)
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

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

    private val messageCounter = AtomicLong(0)

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
            playerEngine = LlamaCppEngine(application.applicationContext)
        }
    }

    val activeTokenInfo: StateFlow<TokenInfo> = conversationTargetId.flatMapLatest { targetId ->
        val engineToUse = if (!useGemmaForAll && targetId.startsWith("Companion", true)) {
            playerEngine
        } else {
            dmEngine
        }
        engineToUse.tokenInfo
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = dmEngine.tokenInfo.value
    )

    fun loadGameSession() {
        val session = gameStateManager.loadSession() ?: gameStateManager.createDefaultSession()
        _gameCharacters.value = session.characters
        _sessionName.value = session.sessionName
        log("Sessione di gioco caricata: ${session.sessionName}")

        if (savePreferences.isAutoSaveEnabled) {
            loadChatFromAutoSave()
        }
    }

    private fun loadChatFromAutoSave() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savesDir = getAppSpecificDirectory(getApplication(), "saves")
                val autoSaveFile = File(savesDir, "autosave_chat.json")

                if (autoSaveFile.exists() && autoSaveFile.length() > 0) {
                    val gson = GsonBuilder().create()
                    val type = object : TypeToken<List<ChatMessage>>() {}.type

                    FileReader(autoSaveFile).use { reader ->
                        val loadedMessages: List<ChatMessage> = gson.fromJson(reader, type)
                        if (loadedMessages.isNotEmpty()) {
                            _chatMessages.value = loadedMessages
                            val maxPosition = loadedMessages.maxOfOrNull { it.position } ?: -1L
                            messageCounter.set(maxPosition + 1)
                            log("Chat caricata con successo (${loadedMessages.size} messaggi).")
                        }
                    }
                } else {
                    log("Nessun file di auto-salvataggio valido trovato.")
                }
            } catch (e: Exception) {
                log("Errore durante il caricamento della chat: ${e.message}")
                Log.e(tag, "Errore caricamento chat", e)
            }
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
        _engineLoadingState.value = EngineLoadingState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (dmModelPath == null && playerModelPath == null) {
                    throw IllegalStateException("Nessun modello configurato.")
                }

                val loadTasks = mutableListOf<Deferred<Unit>>()

                dmModelPath?.let {
                    loadTasks.add(async {
                        log("Tentativo di caricamento modello DM (Gemma) su thread IO...")
                        dmEngine.load(it)
                    })
                }

                if (!useGemmaForAll) {
                    playerModelPath?.let {
                        loadTasks.add(async {
                            log("Tentativo di caricamento modello PG (GGUF) su thread IO...")
                            playerEngine.load(it)
                        })
                    }
                }

                loadTasks.add(async {
                    log("Tentativo di caricamento modello di traduzione...")
                    translationEngine.loadModel()
                })

                loadTasks.awaitAll()

                log("Processo di caricamento motori in background completato.")
                _engineLoadingState.value = EngineLoadingState.Success

            } catch (e: Exception) {
                Log.e(tag, "Errore critico durante il caricamento degli engine", e)
                log("ERRORE CARICAMENTO: ${e.message}")
                _engineLoadingState.value =
                    EngineLoadingState.Error(e.message)
            }
        }
    }

    fun translateMessage(messageId: String) {
        viewModelScope.launch {
            val originalMessage = _chatMessages.value.find { it.id == messageId } ?: return@launch
            updateMessage(messageId) { it.copy(isTranslating = true) }
            try {
                // Non controlliamo più isModelReady qui, translate() gestirà l'eccezione se non è pronto.
                val lines = originalMessage.text.split('\n')
                // Passiamo la lingua di destinazione, che per ora è sempre l'italiano per la UI
                val translatedLines = lines.map { line ->
                    if (line.isBlank()) {
                        async { "" }
                    } else {
                        async { translationEngine.translate(line, targetLang = Locale.ITALIAN.language) } // Chiamata aggiornata
                    }
                }.awaitAll()
                val finalTranslation = translatedLines.joinToString("\n")
                updateMessage(messageId) {
                    it.copy(translatedText = finalTranslation, isTranslating = false)
                }
                log("Traduzione completata per il messaggio ID: $messageId")
            } catch (e: Exception) {
                log("Errore di traduzione: ${e.message}. Assicurati che il modello sia scaricato e caricato.")
                updateMessage(messageId) { it.copy(isTranslating = false) }
                Log.e(tag, "Errore di traduzione", e)
            }
        }
    }

    private fun updateMessage(messageId: String, transformation: (ChatMessage) -> ChatMessage) {
        _chatMessages.update { currentMessages ->
            currentMessages.map { if (it.id == messageId) transformation(it) else it }
        }
    }

    fun onSaveChatClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val chatToSave = _chatMessages.value
                if (chatToSave.isEmpty()) {
                    log("Chat vuota, nessun salvataggio manuale eseguito.")
                    return@launch
                }
                val gson = GsonBuilder().setPrettyPrinting().create()
                val jsonString = gson.toJson(chatToSave)

                val savesDir = getAppSpecificDirectory(getApplication(), "saves")
                if (savesDir == null) {
                    log("Errore: impossibile accedere alla cartella di salvataggio.")
                    return@launch
                }

                val timeStamp =
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "manual_save_${timeStamp}.json"
                val file = File(savesDir, fileName)

                FileWriter(file).use { writer -> writer.write(jsonString) }
                log("Chat salvata manualmente su $fileName")
            } catch (e: Exception) {
                log("Errore durante il salvataggio manuale: ${e.message}")
                Log.e(tag, "Errore salvataggio manuale", e)
            }
        }
    }

    fun sendMessage(text: String, conversationTargetId: String) {
        if (_isGenerating.value) {
            log("Generazione già in corso, richiesta ignorata.")
            return
        }

        val userMessage = ChatMessage(authorId = CharacterID.HERO, position = messageCounter.getAndIncrement(), text = text)
        _chatMessages.update { it + userMessage }
        autoSaveChatIfEnabled()

        val targetId = _conversationTargetId.value
        var engineToUse = dmEngine

        if (!useGemmaForAll && conversationTargetId.startsWith("Companion", true)) {
            engineToUse = playerEngine
        }

        val logMessage = "Invio prompt per una risposta da '$targetId'..."
        log(logMessage)

        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            _streamingText.value = ""
            _respondingCharacterId.value = targetId
            try {
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
                    val finalMessage = ChatMessage(position = messageCounter.getAndIncrement(), authorId = targetId, text = _streamingText.value)
                    _chatMessages.update { it + finalMessage }
                    autoSaveChatIfEnabled()
                }
                _isGenerating.value = false
                _streamingText.value = ""
                _respondingCharacterId.value = null
            }
        }
    }

    private fun autoSaveChatIfEnabled() {
        if (!savePreferences.isAutoSaveEnabled) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val chatToSave = _chatMessages.value
                if (chatToSave.isEmpty()) return@launch

                val gson = GsonBuilder().setPrettyPrinting().create()
                val jsonString = gson.toJson(chatToSave)

                val savesDir = getAppSpecificDirectory(getApplication(), "saves")
                if (savesDir == null) {
                    log("Errore: impossibile accedere alla cartella di salvataggio.")
                    return@launch
                }
                val file = File(savesDir, "autosave_chat.json")
                FileWriter(file).use { writer -> writer.write(jsonString) }
                log("Chat salvata automaticamente.")
            } catch (e: Exception) {
                log("Errore durante il salvataggio automatico della chat: ${e.message}")
                Log.e(tag, "Errore salvataggio automatico", e)
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

    fun resetSession() {
        viewModelScope.launch {
            log("Avvio reset sessione per il motore attivo...")
            val targetId = _conversationTargetId.value
            val engineToUse = if (!useGemmaForAll && targetId.startsWith("Companion", true)) {
                playerEngine
            } else {
                dmEngine
            }
            engineToUse.resetSession(null)
            log("Reset della sessione completato per ${engineToUse::class.simpleName}.")
        }
    }
}