package io.github.luposolitario.immundanoctis.view

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.data.CharacterType
import io.github.luposolitario.immundanoctis.data.ChatMessage
import io.github.luposolitario.immundanoctis.data.EngineCommand
import io.github.luposolitario.immundanoctis.data.GameChallenge
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.Genre
import io.github.luposolitario.immundanoctis.data.NarrativeChoice
import io.github.luposolitario.immundanoctis.data.Scene
import io.github.luposolitario.immundanoctis.data.SceneType
import io.github.luposolitario.immundanoctis.data.SessionData
import io.github.luposolitario.immundanoctis.engine.GameLogicManager
import io.github.luposolitario.immundanoctis.engine.GameRulesEngine
import io.github.luposolitario.immundanoctis.engine.GemmaEngine
import io.github.luposolitario.immundanoctis.engine.InferenceEngine
import io.github.luposolitario.immundanoctis.engine.LlamaCppEngine
import io.github.luposolitario.immundanoctis.engine.TokenInfo
import io.github.luposolitario.immundanoctis.engine.TranslationEngine
import io.github.luposolitario.immundanoctis.engine.rules.LoneWolfRules
import io.github.luposolitario.immundanoctis.util.EnginePreferences
import io.github.luposolitario.immundanoctis.util.GameStateManager
import io.github.luposolitario.immundanoctis.util.LlamaPreferences
import io.github.luposolitario.immundanoctis.util.SavePreferences
import io.github.luposolitario.immundanoctis.util.StringTagParser
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import io.github.luposolitario.immundanoctis.util.getAppSpecificDirectory
import kotlinx.coroutines.Deferred
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
import kotlinx.coroutines.flow.first
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
import kotlin.random.Random

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
    private val themePreferences = ThemePreferences(application)
    private val llamaPreferences = LlamaPreferences(application)

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

    // All'interno della classe MainViewModel, dopo le altre dichiarazioni di variabili
    private val gameRules: GameRulesEngine = LoneWolfRules()

    private val _conversationTargetId = MutableStateFlow(
        themePreferences.getLastSelectedCharacterId() ?: CharacterID.DM
    )
    val conversationTargetId: StateFlow<String> = _conversationTargetId.asStateFlow()

    private val _saveChatEvent = MutableSharedFlow<String>()
    val saveChatEvent: SharedFlow<String> = _saveChatEvent.asSharedFlow()

    private var generationJob: Job? = null

    private val messageCounter = AtomicLong(0)

    private val useGemmaForAll = enginePreferences.useGemmaForAll

    private val dmEngine: InferenceEngine
    private val playerEngine: InferenceEngine
    private val translationEngine = TranslationEngine()

    private lateinit var stringTagParser: StringTagParser

    private val _currentScene = MutableStateFlow<Scene?>(null)
    val currentScene: StateFlow<Scene?> = _currentScene.asStateFlow()

    private lateinit var gameLogicManager: GameLogicManager

    private val _activeChallenges = MutableStateFlow<List<GameChallenge>>(emptyList())
    val activeChallenges: StateFlow<List<GameChallenge>> = _activeChallenges.asStateFlow()

    private val _activeNarrativeChoices = MutableStateFlow<List<NarrativeChoice>>(emptyList())
    val activeNarrativeChoices: StateFlow<List<NarrativeChoice>> =
        _activeNarrativeChoices.asStateFlow()

    private val _activeDirectionalChoices = MutableStateFlow<List<EngineCommand>>(emptyList())
    val activeDirectionalChoices: StateFlow<List<EngineCommand>> =
        _activeDirectionalChoices.asStateFlow()

//    private val _combatState = MutableStateFlow<CombatState?>(null)
//    val combatState = _combatState.asStateFlow()

    private val _usableDisciplines = MutableStateFlow<Set<String>>(emptySet())
    val usableDisciplines = _usableDisciplines.asStateFlow()

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
        stringTagParser = StringTagParser(application.applicationContext)
        gameLogicManager = GameLogicManager(application.applicationContext)
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

    fun loadGameSession(startFresh: Boolean = false) {
        val session = gameStateManager.loadSession()
        val actualIsNewAdventure = (session == null || startFresh)

        val currentSession = session ?: gameStateManager.createDefaultSession()
        _gameCharacters.value = currentSession.characters
        _sessionName.value = currentSession.sessionName
        log("Sessione di gioco caricata: ${currentSession.sessionName}")

        if (savePreferences.isAutoSaveEnabled) {
            loadChatFromAutoSave()
        }
        val lastSavedId = themePreferences.getLastSelectedCharacterId()
        if (lastSavedId != null && currentSession.characters.any { it.id == lastSavedId }) {
            _conversationTargetId.value = lastSavedId
            log("Ripristinato target di conversazione: $lastSavedId")
        } else {
            _conversationTargetId.value = CharacterID.DM
            log("Nessun target di conversazione salvato valido. Impostato su DM.")
        }

        if (actualIsNewAdventure) {
            _currentScene.value = gameLogicManager.selectRandomStartScene(Genre.FANTASY)
            log("Scena iniziale NUOVA AVVENTURA impostata da GameLogicManager: ${_currentScene.value?.id ?: "Nessuna scena iniziale"}")
            viewModelScope.launch {
                sendInitialDmPrompt(currentSession, _currentScene.value)
            }
            _currentScene.value?.let { currentSession.usedScenes.add(it.id) }
            gameStateManager.saveSession(currentSession)
        } else {
            val lastSceneId = currentSession.usedScenes.lastOrNull()
            _currentScene.value = if (lastSceneId != null) {
                gameLogicManager.getSceneById(lastSceneId)
            } else {
                gameLogicManager.selectRandomStartScene(Genre.FANTASY)
            }
            log("Scena sessione esistente impostata a: ${_currentScene.value?.id ?: "Nessuna scena valida trovata. Riprovo con casuale START."}")
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
                        dmEngine.load(it, llamaPreferences.chatbotPersonality)
                    })
                }

                if (!useGemmaForAll) {
                    playerModelPath?.let {
                        loadTasks.add(async {
                            log("Tentativo di caricamento modello PG (GGUF) su thread IO...")
                            playerEngine.load(it, llamaPreferences.chatbotPersonality)
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
                val lines = originalMessage.text.split('\n')
                val translatedLines = lines.map { line ->
                    if (line.isBlank()) {
                        async { "" }
                    } else {
                        async {
                            translationEngine.translate(
                                line,
                                targetLang = Locale.ITALIAN.language
                            )
                        }
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

    // Funzione updateMessage spostata qui dal tuo AdventureActivity.kt
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

        val heroCharacter = _gameCharacters.value.find { it.id == CharacterID.HERO }
        val playerLanguage =
            heroCharacter?.language ?: Locale.ENGLISH.language // Lingua del giocatore

        val (parsedPlayerText, playerCommands) = stringTagParser.parseAndReplaceWithCommands(
            inputString = text,
            currentActor = CharacterType.PLAYER,
            lang = playerLanguage
        )

        val userMessage = ChatMessage(
            authorId = CharacterID.HERO,
            position = messageCounter.getAndIncrement(),
            text = parsedPlayerText
        )
        _chatMessages.update { it + userMessage }
        autoSaveChatIfEnabled()

        viewModelScope.launch {
            processCommands(playerCommands)
        }

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
                // AGGIUNTO: Attesa che i motori siano pronti prima di inviare messaggi regolari
                _engineLoadingState.first { it is EngineLoadingState.Success }
                log("DEBUG: Motori pronti per sendMessage. Invio testo del giocatore.")

                // Il prompt per la scena iniziale è gestito ora da sendInitialDmPrompt()
                engineToUse.sendMessage(parsedPlayerText)
                    .collect { token ->
                        _streamingText.update { it + token }
                    }
            } catch (e: Exception) {
                Log.e(tag, "Errore durante la raccolta del flow di messaggi", e)
                log("Errore: ${e.message}")
            } finally {
                log("Generazione per '$targetId' completata o interrotta.")

                val rawLLMResponse = _streamingText.value
                val respondingCharacter = _gameCharacters.value.find { it.id == targetId }
                val llmLanguage = respondingCharacter?.language ?: Locale.ITALIAN.language

                val (parsedLLMText, llmCommands) = stringTagParser.parseAndReplaceWithCommands(
                    inputString = rawLLMResponse,
                    currentActor = respondingCharacter?.type,
                    lang = llmLanguage
                )

                if (parsedLLMText.isNotBlank()) {
                    val finalMessage = ChatMessage(
                        authorId = targetId,
                        position = messageCounter.getAndIncrement(),
                        text = parsedLLMText
                    )
                    _chatMessages.update { it + finalMessage }
                    autoSaveChatIfEnabled()
                }

                viewModelScope.launch {
                    processCommands(llmCommands)
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
        themePreferences.saveLastSelectedCharacterId(characterId)
        log("Ora stai parlando con: $characterId")
    }

    fun log(message: String) {
        _logMessages.update { it + message }
    }

    // NUOVA FUNZIONE: per scaricare esplicitamente il motore DM (necessaria per la pulizia)
    suspend fun unloadDmEngine() {
        Log.d(tag, "Richiesta di unload del motore DM (Gemma).")
        dmEngine.unload()
        Log.d(tag, "Motore DM (Gemma) scaricato.")
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

            // Se il motore è il playerEngine (LlamaCppEngine) e c'è una personalità definita, passala.
            val systemPromptForReset = if (engineToUse is LlamaCppEngine) {
                llamaPreferences.chatbotPersonality
            } else {
                null
            }

            engineToUse.resetSession(systemPromptForReset)
            log("Reset della sessione completato per ${engineToUse::class.simpleName}.")
            _currentScene.value = gameLogicManager.selectRandomStartScene(Genre.FANTASY)
            log("Scena reimpostata a una scena START casuale di genere FANTASY.")
            gameLogicManager.resetUsedScenes()
            val currentSession =
                gameStateManager.loadSession() ?: gameStateManager.createDefaultSession()
            if (_currentScene.value?.sceneType == SceneType.START) {
                sendInitialDmPrompt(currentSession, _currentScene.value)
            }
        }
    }

    private fun rollDice(numDice: Int, sides: Int): Int {
        var totalRoll = 0
        repeat(numDice) {
            totalRoll += Random.nextInt(1, sides + 1)
        }
        return totalRoll
    }

    private suspend fun processCommands(commands: List<EngineCommand>) {
        if (commands.isEmpty()) {
            return
        }
        log("Processing commands: ${commands.map { it.commandName }}")
        commands.forEach { command ->
            Log.d(
                tag,
                "Comando ricevuto: ${command.commandName} con parametri: ${command.parameters}"
            )
            when (command.commandName) {
                "playAudio" -> {
                    val audioFile = command.parameters["audioFile"] as? String
                    log("DEBUG COMMAND: Riproduci audio: $audioFile")
                    // Qui andrebbe la logica per riprodurre l'audio
                }

                "generateImage" -> {
                    val prompt = command.parameters["prompt"] as? String
                    log("DEBUG COMMAND: Genera immagine con prompt: $prompt")
                    // Qui andrebbe la logica per generare un'immagine con Stable Diffusion
                }

                "triggerGraphicEffect" -> {
                    val effectName = command.parameters["effectName"] as? String
                    log("DEBUG COMMAND: Attiva effetto grafico: $effectName")
                    // Qui andrebbe la logica per attivare un effetto grafico
                }

                "narrativeChoice" -> {
                    val choiceId = command.parameters["nextSceneId"] as? String
                    val choiceText = command.parameters["choiceText"] as? String
                    log("DEBUG COMMAND: Avvia scelta narrativa: ID=${choiceId}, Testo='${choiceText}'")
                    // Qui andrebbe la logica per presentare una scelta narrativa
                }

                "displayDirectionalButton" -> {
                    val direction = command.parameters["direction"] as? String
                    val colorHex = command.parameters["colorHex"] as? String
                    val choiceText = command.parameters["choiceText"] as? String
                    val nextSceneId = command.parameters["nextSceneId"] as? String
                    log("DEBUG COMMAND: Mostra pulsante direzionale: Dir=${direction}, Colore=${colorHex}, Testo='${choiceText}', ProssimaScena=${nextSceneId}")
                    // Qui andrebbe la logica per esporre questi dati alla UI per mostrare il pulsante
                }

                else -> {
                    log("DEBUG COMMAND: Comando sconosciuto: ${command.commandName}")
                }
            }
        }
    }

    // NUOVA FUNZIONE: Invia il prompt iniziale del DM all'avvio di una nuova avventura
    suspend fun sendInitialDmPrompt(
        sessionData: SessionData,
        currentScene1: Scene?
    ) {
        // Impedisce l'invio multiplo se la sessione è già stata avviata con questo prompt
        if (sessionData.isStarted) {
            log("DEBUG: La sessione è già iniziata, non invio prompt iniziale DM.")
            return
        }

        _isGenerating.value = true
        _streamingText.value = ""
        _respondingCharacterId.value = CharacterID.DM // Sempre DM per il prompt iniziale

        try {
            // AGGIUNTO: Attendiamo che i motori siano caricati prima di inviare il prompt
            log("DEBUG: Attendendo caricamento motori prima di inviare prompt iniziale DM...")
            _engineLoadingState.first { it is EngineLoadingState.Success }
            log("DEBUG: Motori pronti, invio prompt iniziale DM.")

            val scene = currentScene1
            if (scene == null || scene.sceneType != SceneType.START) {
                log("ATTENZIONE: Impossibile inviare prompt iniziale DM. Scena non START o non caricata.")
                _isGenerating.value = false // Assicurati di resettare lo stato di generazione
                return
            }

            // --- INIZIO NUOVA LOGICA: Costruzione del prompt dal tag config.json ---
            val startPromptTag = stringTagParser.getTagConfigById("start_adventure_prompt")
            if (startPromptTag == null || startPromptTag.parameters == null) {
                log("ERRORE: Tag 'start_adventure_prompt' non trovato o incompleto in config.json!")
                _isGenerating.value = false
                return
            }

            // Estrai i parametri dal tag
            val baseText =
                startPromptTag.parameters.firstOrNull { it.name == "baseText" }?.value as? String
                    ?: "Sei il DM per un gioco di ruolo."
            val genreTextTemplate =
                startPromptTag.parameters.firstOrNull { it.name == "genreText" }?.value as? String
                    ?: "Il genere della storia è: {genre}."
            val sceneTextTemplate =
                startPromptTag.parameters.firstOrNull { it.name == "sceneText" }?.value as? String
                    ?: "Inizia la narrazione dalla seguente scena: {scene_narrative_text}."
            val continuationText =
                startPromptTag.parameters.firstOrNull { it.name == "continuationText" }?.value as? String
                    ?: "Ti prego di iniziare a narrare la storia basandoti su questa scena."

            val genre = Genre.FANTASY.name // Genere hardcoded per ora, poi dalla campagna

            // Selezioniamo prima la stringa della lingua corretta (es. italiano)
            val sceneNarrativeText = scene.narrativeText.italian  // o .en a seconda della lingua di gioco

            val secretPrompt = """
    $baseText
    ${genreTextTemplate.replace("{genre}", genre, ignoreCase = true)}
    ${
                sceneTextTemplate.replace(
                    "{scene_narrative_text}",
                    sceneNarrativeText.toString(),
                    ignoreCase = true
                )
            }  
    $continuationText
    """.trimIndent()

            log("DEBUG: Invio prompt iniziale DM: Genere=${genre}, Scena ID=${scene.id}")

            dmEngine.sendMessage(secretPrompt)
                .collect { token ->
                    _streamingText.update { it + token }
                }
            val updatedSession = sessionData.copy(isStarted = true)
            gameStateManager.saveSession(updatedSession)
            log("DEBUG: Sessione marcata come avviata.")

        } catch (e: Exception) {
            Log.e(tag, "Errore durante l'invio del prompt iniziale al DM: ${e.message}", e)
            log("ERRORE: Impossibile avviare la narrazione del DM. ${e.message}")
        } finally {
            if (_streamingText.value.isNotBlank()) {
                val finalMessage = ChatMessage(
                    authorId = CharacterID.DM,
                    position = messageCounter.getAndIncrement(),
                    text = _streamingText.value
                )
                _chatMessages.update { it + finalMessage }
                autoSaveChatIfEnabled()
            }
            _isGenerating.value = false
            _streamingText.value = ""
            _respondingCharacterId.value = null
        }
    }

    fun onDisciplineClicked(disciplineId: String) {}
}