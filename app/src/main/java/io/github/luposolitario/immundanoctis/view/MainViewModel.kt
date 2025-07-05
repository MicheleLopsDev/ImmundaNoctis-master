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
import io.github.luposolitario.immundanoctis.data.DisciplineChoice
import io.github.luposolitario.immundanoctis.data.EngineCommand
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.GameItem
import io.github.luposolitario.immundanoctis.data.Genre
import io.github.luposolitario.immundanoctis.data.ItemType
import io.github.luposolitario.immundanoctis.data.KAI_DISCIPLINES
import io.github.luposolitario.immundanoctis.data.NarrativeChoice
import io.github.luposolitario.immundanoctis.data.Scene
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


    // Dentro MainViewModel.kt
    data class InventoryFullState(
        val newItem: GameItem,
        val existingItems: List<GameItem>,
        val itemType: ItemType
    )


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

    val currentScene: StateFlow<Scene?>
        get() = _currentScene
    private val _currentScene = MutableStateFlow<Scene?>(null)


    private lateinit var gameLogicManager: GameLogicManager

    private val _activeNarrativeChoices = MutableStateFlow<List<NarrativeChoice>>(emptyList())
    val activeNarrativeChoices: StateFlow<List<NarrativeChoice>> =
        _activeNarrativeChoices.asStateFlow()

    private val _activeDisciplineChoices = MutableStateFlow<List<DisciplineChoice>>(emptyList())
    val activeDisciplineChoices: StateFlow<List<DisciplineChoice>> =
        _activeDisciplineChoices.asStateFlow()

    private val _kaiRank = MutableStateFlow("")
    val kaiRank: StateFlow<String> = _kaiRank.asStateFlow()

    private val _isRandomNumberRollRequired = MutableStateFlow(false)
    val isRandomNumberRollRequired: StateFlow<Boolean> = _isRandomNumberRollRequired.asStateFlow()

    private val _randomNumberResult = MutableStateFlow<Int?>(null)
    val randomNumberResult: StateFlow<Int?> = _randomNumberResult.asStateFlow()

    // Dentro la classe MainViewModel
    private val _inventoryFullState = MutableStateFlow<InventoryFullState?>(null)
    val inventoryFullState: StateFlow<InventoryFullState?> = _inventoryFullState.asStateFlow()

    // All'interno della classe MainViewModel, vicino alle altre dichiarazioni di StateFlow

    // Usiamo un SharedFlow per eventi "spara e dimentica" come i Toast.
    private val _uiFeedbackEvent = MutableSharedFlow<String>()
    val uiFeedbackEvent: SharedFlow<String> = _uiFeedbackEvent.asSharedFlow()

    // All'inizio della classe MainViewModel
    private val _isHeroDead = MutableStateFlow(false)
    val isHeroDead: StateFlow<Boolean> = _isHeroDead.asStateFlow()


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

        updatePlayerStatus()

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
            _sessionName.value =
                gameLogicManager.adventureName // <--- Usa il nome dell'avventura dal file JSON
            gameLogicManager.resetUsedScenes()
            _currentScene.value = gameLogicManager.selectRandomStartScene(Genre.FANTASY)
            log("Scena iniziale NUOVA AVVENTURA impostata da GameLogicManager: ${_currentScene.value?.id ?: "Nessuna scena iniziale"}. Nome Avventura: ${_sessionName.value}")
            viewModelScope.launch {
                sendInitialDmPrompt(currentSession)
            }
        } else {
            val lastSceneId = currentSession.usedScenes.lastOrNull()
            _currentScene.value = if (lastSceneId != null) {
                gameLogicManager.getSceneById(lastSceneId)
            } else {
                gameLogicManager.selectRandomStartScene(Genre.FANTASY)
            }
            log("Scena sessione esistente impostata a: ${_currentScene.value?.id ?: "Nessuna scena valida trovata. Riprovo con casuale START."}")
            viewModelScope.launch {
                processCurrentSceneNarrative(shouldGenerateNarration = false)
            }
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
            heroCharacter?.language ?: Locale.ENGLISH.language

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
                _engineLoadingState.first { it is EngineLoadingState.Success }
                log("DEBUG: Motori pronti per sendMessage. Invio testo del giocatore.")

                engineToUse.sendMessage(parsedPlayerText)
                    .collect { token ->
                        _streamingText.update { it + token }
                    }
            } catch (e: Exception) {
                Log.e(tag, "Errore durante la raccolta del flow di messaggi", e)
                log("Errore: ${e.message}")
            } finally {
                log("Generazione completata o interrotta. Inizio parsing della risposta.")

                val rawLLMResponse = _streamingText.value
                val respondingCharacter =
                    _gameCharacters.value.find { it.id == _respondingCharacterId.value }
                val llmLanguage = respondingCharacter?.language ?: Locale.ITALIAN.language

                // --- 👇 NUOVA LOGICA DI PARSING 👇 ---

                // 1. Dividi la risposta di Gemma in narrazione e tag
                val parts = rawLLMResponse.split("--- TAGS ---", limit = 2)
                val narrativePart = parts.getOrNull(0)?.trim() ?: ""
                val tagsPart = parts.getOrNull(1)?.trim() ?: ""

                val allCommands = mutableListOf<EngineCommand>()

                // 2. Passa SOLO la parte narrativa al parser per pulire i vecchi tag (es. {STAT_MOD...})
                if (narrativePart.isNotBlank()) {
                    val (cleanedNarrative, narrativeCommands) = stringTagParser.parseAndReplaceWithCommands(
                        inputString = narrativePart,
                        currentActor = respondingCharacter?.type,
                        lang = llmLanguage
                    )
                    allCommands.addAll(narrativeCommands)

                    // 3. Aggiungi il messaggio di chat con la narrazione PULITA
                    val finalMessage = ChatMessage(
                        authorId = _respondingCharacterId.value ?: CharacterID.DM,
                        position = messageCounter.getAndIncrement(),
                        text = cleanedNarrative
                    )
                    _chatMessages.update { it + finalMessage }
                    autoSaveChatIfEnabled()
                }

                // 4. Passa SOLO la parte dei tag al parser per estrarre i comandi di aggiornamento delle scelte
                if (tagsPart.isNotBlank()) {
                    val (_, choiceCommands) = stringTagParser.parseAndReplaceWithCommands(
                        inputString = tagsPart,
                        currentActor = respondingCharacter?.type,
                        lang = llmLanguage
                    )
                    allCommands.addAll(choiceCommands)
                }

                // 5. Esegui tutti i comandi raccolti
                viewModelScope.launch {
                    processCommands(allCommands)
                }

                // 6. Resetta lo stato della generazione
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

    // --- FUNZIONE DI LOG MODIFICATA ---
    fun log(message: String) {
        // Stampa nel Logcat di Android Studio per il debug
        Log.d(tag, message)
        // Mantiene il log interno per una futura UI di debug
        _logMessages.update { it + message }
    }

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
            if (_currentScene.value?.id != null) {
                sendInitialDmPrompt(currentSession)
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
        log("Processing ${commands.size} commands: ${commands.map { it.commandName }}")
        val currentSession = gameStateManager.loadSession() ?: return
        val hero = currentSession.characters.find { it.id == CharacterID.HERO } ?: return
        var sessionModified = false

        commands.forEach { command ->
            Log.d(
                tag,
                "Executing command: ${command.commandName} with params: ${command.parameters}"
            )
            when (command.commandName) {

                "addItem" -> {
                    val itemName = command.parameters["itemName"] as? String
                    val itemTypeStr = command.parameters["itemType"] as? String
                    val quantity = command.parameters["quantity"] as? Int ?: 1

                    if (itemName != null && itemTypeStr != null) {
                        try {
                            val itemType = ItemType.valueOf(itemTypeStr)
                            val newItem =
                                GameItem(name = itemName, type = itemType, quantity = quantity)

                            val inventory = hero.details?.inventory ?: mutableListOf()

                            // Controlla i limiti
                            val weaponCount = inventory.count { it.type == ItemType.WEAPON }
                            val backpackItemCount =
                                inventory.count { it.type == ItemType.BACKPACK_ITEM }

                            var canAddDirectly = true

                            if (itemType == ItemType.WEAPON && weaponCount >= 2) {
                                _inventoryFullState.value = InventoryFullState(
                                    newItem,
                                    inventory.filter { it.type == ItemType.WEAPON },
                                    itemType
                                )
                                canAddDirectly = false
                            } else if (itemType == ItemType.BACKPACK_ITEM && backpackItemCount >= 8) {
                                _inventoryFullState.value = InventoryFullState(
                                    newItem,
                                    inventory.filter { it.type == ItemType.BACKPACK_ITEM },
                                    itemType
                                )
                                canAddDirectly = false
                            }

                            if (canAddDirectly) {
                                inventory.add(newItem)
                                log("✅ Aggiunto all'inventario: ${newItem.name} (x$quantity)")
                                _uiFeedbackEvent.emit("Hai trovato: ${newItem.name}")
                                sessionModified = true
                            } else {
                                log("⚠️ Inventario pieno per il tipo $itemType. In attesa della decisione del giocatore.")
                                _uiFeedbackEvent.emit("Hai trovato: ${newItem.name}, ma il tuo inventario è pieno!")
                            }

                        } catch (e: IllegalArgumentException) {
                            log("❌ ERRORE: Tipo di oggetto non valido '$itemTypeStr' per il comando addItem.")
                        }
                    }
                }

                // **MODIFICA**: Il comando "addGold" è stato rimosso. La sua logica andrà dentro "addItem".

                "updateChoiceText" -> {
                    val choiceId = command.parameters["id"] as? String
                    val italianText = command.parameters["italianText"] as? String
                    if (choiceId != null && italianText != null) {
                        updateNarrativeChoiceText(choiceId, italianText)
                    }
                }
                "updateDisciplineChoiceText" -> {
                    val disciplineId = command.parameters["id"] as? String
                    val italianText = command.parameters["italianText"] as? String
                    if (disciplineId != null && italianText != null) {
                        updateDisciplineChoiceText(disciplineId, italianText)
                    }
                }

                // **MODIFICA**: "requireMeal" diventa "requireAction"
                "requireAction" -> {
                    // Per ora la logica rimane la stessa, ma il nome del comando è aggiornato.
                    val inventory = hero.details?.inventory ?: mutableListOf()
                    val meal = inventory.find { it.type == ItemType.MEAL }

                    if (meal != null && meal.quantity > 0) {
                        val updatedMeal = meal.copy(quantity = meal.quantity - 1)
                        val updatedInventory =
                            inventory.map { if (it.id == meal.id) updatedMeal else it }
                                .toMutableList()

                        if (updatedMeal.quantity <= 0) {
                            updatedInventory.remove(updatedMeal)
                        }

                        val updatedDetails = hero.details?.copy(inventory = updatedInventory)
                        val updatedHero = hero.copy(details = updatedDetails)
                        _gameCharacters.update { list -> list.map { if (it.id == hero.id) updatedHero else it } }

                        log("✅ Pasto consumato. Il giocatore ha mangiato.")
                        _uiFeedbackEvent.emit("Hai consumato un Pasto.")

                    } else {
                        val currentEndurance = hero.stats?.resistenza ?: 0
                        val newEndurance =
                            (currentEndurance - 3).coerceAtLeast(0)

                        val updatedStats = hero.stats?.copy(resistenza = newEndurance)
                        val updatedHero = hero.copy(stats = updatedStats)
                        _gameCharacters.update { list -> list.map { if (it.id == hero.id) updatedHero else it } }

                        if (newEndurance <= 0) {
                            _isHeroDead.value = true
                        }

                        log("❌ Nessun pasto disponibile. Il giocatore perde 3 punti Resistenza.")
                        _uiFeedbackEvent.emit("Non hai cibo! Perdi 3 punti Resistenza.")
                    }
                    sessionModified = true
                }

                // **MODIFICA**: "removeAllWeaponsAndBackpackItems" diventa "removeAllItems"
                "removeAllItems" -> {
                    // Per ora la logica rimane la stessa, ma il nome del comando è aggiornato.
                    val inventory = hero.details?.inventory ?: mutableListOf()

                    inventory.removeAll { item ->
                        (item.type == ItemType.WEAPON || item.type == ItemType.BACKPACK_ITEM) && item.isDiscardable
                    }

                    val updatedDetails = hero.details?.copy(inventory = inventory)
                    val updatedHero = hero.copy(details = updatedDetails)
                    _gameCharacters.update { list -> list.map { if(it.id == hero.id) updatedHero else it } }

                    log("‼️ Oggetti scartabili (Armi e Zaino) rimossi.")
                    _uiFeedbackEvent.emit("Hai perso il tuo equipaggiamento!")
                    sessionModified = true
                }

                "applyStatModifier" -> {
                    val statName = command.parameters["statName"] as? String
                    val amountStr = command.parameters["amount"] as? String

                    if (statName != null && amountStr != null) {
                        val heroStats = hero.stats ?: return@forEach
                        var newCombatSkill = heroStats.combattivita
                        var newEndurance = heroStats.resistenza

                        val amount = amountStr.toIntOrNull()

                        if (amount != null) {
                            when (statName.uppercase()) {
                                "COMBATTIVITA" -> {
                                    newCombatSkill = (newCombatSkill + amount).coerceAtLeast(0)
                                    log("STAT MOD: Combattività modificata di $amount. Nuovo valore: $newCombatSkill")
                                }
                                "RESISTENZA" -> {
                                    newEndurance = (newEndurance + amount).coerceAtLeast(0)
                                    log("STAT MOD: Resistenza modificata di $amount. Nuovo valore: $newEndurance")
                                }
                            }

                            val updatedStats = heroStats.copy(combattivita = newCombatSkill, resistenza = newEndurance)
                            val updatedHero = hero.copy(stats = updatedStats)
                            _gameCharacters.update { list -> list.map { if(it.id == hero.id) updatedHero else it } }

                            _uiFeedbackEvent.emit("La tua $statName è cambiata di $amount!")
                            sessionModified = true
                        }
                    }
                }
                else -> {
                    log("⚠️ Comando sconosciuto o non ancora implementato: ${command.commandName}")
                }
            }
        }

        if (sessionModified) {
            val updatedCharacters =
                currentSession.characters.map { if (it.id == CharacterID.HERO) hero else it }
            gameStateManager.saveSession(currentSession.copy(characters = updatedCharacters))
            log("Salvataggio sessione dopo l'aggiornamento.")
        }
    }

    fun resolveInventoryExchange(itemToDiscard: GameItem, newItem: GameItem) {
        viewModelScope.launch {
            val session = gameStateManager.loadSession() ?: return@launch
            val hero = session.characters.find { it.id == CharacterID.HERO } ?: return@launch

            val inventory = hero.details?.inventory ?: return@launch

            // Rimuovi il vecchio oggetto e aggiungi il nuovo
            inventory.remove(itemToDiscard)
            inventory.add(newItem)

            gameStateManager.saveSession(session)
            log("✅ Scambiato '${itemToDiscard.name}' con '${newItem.name}'.")
            _uiFeedbackEvent.emit("'${itemToDiscard.name}' scartato, '${newItem.name}' raccolto.")

            // Resetta lo stato per nascondere il dialogo
            _inventoryFullState.value = null
        }
    }

    fun dismissInventoryFullDialog() {
        viewModelScope.launch {
            val item = _inventoryFullState.value?.newItem
            if (item != null) {
                _uiFeedbackEvent.emit("Hai deciso di lasciare '${item.name}'.")
            }
            _inventoryFullState.value = null
        }
    }

    private fun updateNarrativeChoiceText(choiceId: String, italianText: String) {
        _activeNarrativeChoices.update { currentChoices ->
            currentChoices.map { choice ->
                if (choice.id == choiceId) {
                    // Crea una nuova istanza di NarrativeChoice con il testo italiano aggiornato
                    choice.copy(choiceText = choice.choiceText.copy(italian = italianText))
                } else {
                    choice
                }
            }
        }
        log("Testo per la scelta narrativa '$choiceId' aggiornato a: '$italianText'")
    }

    private fun updateDisciplineChoiceText(disciplineId: String, italianText: String) {
        _activeDisciplineChoices.update { currentChoices ->
            currentChoices.map { choice ->
                if (choice.disciplineId == disciplineId) {
                    choice.copy(choiceText = choice.choiceText?.copy(italian = italianText))
                } else {
                    choice
                }
            }
        }
        log("Testo per la scelta di disciplina '$disciplineId' aggiornato a: '$italianText'")
    }

    private suspend fun processCurrentSceneNarrative(shouldGenerateNarration: Boolean = true) {
        val scene = _currentScene.value ?: run {
            log("ERRORE: Tentativo di processare una scena nulla.")
            return
        }

        var stringRaw : String = ""

        prepareChoicesForScene(scene)
        if (shouldGenerateNarration) {
            if (_isGenerating.value) return
            _isGenerating.value = true
            _streamingText.value = ""
            _respondingCharacterId.value = CharacterID.DM

            try {
                _engineLoadingState.first { it is EngineLoadingState.Success }

                val sceneNarrativeEnglish = scene.narrativeText.english ?: ""
                val choicesTextListEnglish =
                    scene.choices?.mapNotNull { it.choiceText.english } ?: emptyList()
                val choicesStringEnglish = choicesTextListEnglish.joinToString(", ")
                val lastMessageText =
                    _chatMessages.value.lastOrNull()?.text ?: "L'avventura ha inizio."

                // Recupera il tono narrativo dalle preferenze
                val currentTone = savePreferences.narrativeTone // <-- Recupera il tono salvato
                val toneInstruction = if (currentTone != "originale") {
                    "Adatta il tono generale della narrazione per essere $currentTone. Arricchisci la descrizione con dettagli vividi e sensoriali che rafforzino questo tono, senza però inventare nuovi eventi o stravolgere la trama originale della scena."
                } else {
                    "Mantieni lo stile del testo originale." // Quando è "originale", solo traduce senza alterazioni di tono
                }


                // 1. Estrai e formatta le scelte per il prompt (questo codice va prima della definizione del prompt)
                val choicesForPrompt = scene.choices?.joinToString("\n") {
                    "CHOICE_ID: \"${it.id}\" -> TEXT: \"${it.choiceText.english}\""
                } ?: "Nessuna scelta narrativa."

                val disciplinesForPrompt = scene.disciplineChoices?.joinToString("\n") {
                    // Gestisce il caso in cui choiceText potrebbe essere nullo
                    val text = it.choiceText?.english ?: "Usa la disciplina ${it.disciplineId}"
                    "DISCIPLINE_CHOICE_ID: \"${it.disciplineId}\" -> TEXT: \"$text\""
                } ?: "Nessuna scelta di disciplina."

                // 2. Definisci il nuovo prompt per Gemma con le correzioni applicate

                // Sostituisci il vecchio prompt con il nuovo nella chiamata all'engine:
                // dmEngine.sendMessage(newPromptForGemma)...
                log("DEBUG: Invio prompt di armonizzazione, traduzione e tonale al DM per la scena ID=${scene.id}")
                val promptForGemma = """
                Tu sei il Dungeon Master per un libro-gioco. Il tuo compito è elaborare una scena per il giocatore.
                 
                Segui queste istruzioni ESATTAMENTE:
                                 
                1.  **IDENTIFICA E SEPARA OGNI TIPO DI CONTENUTO**:
                    * **Narrazione**: Il testo puramente descrittivo escludi i TAG XML-like (es. `<addItem .../>`, `<applyStatModifier .../>`, `<addGold .../>`, `<requireMeal .../>`, `<removeAllWeaponsAndBackpackItems .../>`).
                    * **Comandi di Gioco Espliciti**: Istruzioni specifiche che alterano lo stato del gioco, presenti nel testo narrativo in formato XML-like (es. `<addItem .../>`, `<applyStatModifier .../>`, `<addGold .../>`, `<requireMeal .../>`, `<removeAllWeaponsAndBackpackItems .../>`) o in un formato abbreviato specifico come `<STAT_MOD:NOME_STATISTICA:VALORE_MODIFICATORE>`. Questi comandi saranno **fisicamente presenti** nel "TESTO NARRATIVO DA TRADURRE E PRESERVARE".
                    * **Dati Scelte**: Informazioni per le scelte del giocatore e discipline, fornite in formato "CHOICE_ID: 'id' -> TEXT: 'testo'".
                                 
                2.  **GENERA E RAGGRUPPA TUTTI I TAG RICHIESTI**: Crea una sezione separata da `--- TAGS ---`. In questa sezione, devi inserire **SOLO E SOLTANTO** i seguenti tipi di tag, basati **ESCLUSIVAMENTE** e **SENZA ALCUNA ECCEZIONE** sui dati forniti nell'input:
                    * Tutti i "Comandi di Gioco Espliciti" esattamente come erano nell'input originale. **Se un comando è nel formato abbreviato `<STAT_MOD:NOME:VALORE>`.** **NON GENERARE MAI NUOVI Comandi di Gioco basandoti sul contesto narrativo o su azioni implicite.** Devono essere copiati direttamente dall'input se presenti.
                    * Tag delle scelte in italiano, usando il formato `<choice_it id="ID_SCELTA">Testo Tradotto.</choice_it>`, generati solo dai "Dati Scelte" forniti.
                    * Tag delle discipline in italiano, usando il formato `<discipline_it id="ID_DISCIPLINA">Testo Tradotto.</discipline_it>`, generati solo dai "Dati Scelte" forniti.
                    **È FONDAMENTALE: NON INVENTARE, NON AGGIUNGERE, E NON GENERARE MAI NUOVI TAG O INFORMAZIONI CHE NON SIANO ESPLICITAMENTE PRESENTI O DERIVABILI DALL'INPUT FORNITO. TUTTI I TAG DEVONO ESSERE NEL FORMATO XML-LIKE (`<.../>` o `<...></...>` COME NEGLI ESEMPI).**
                       
                                  
                L'output DEVE avere due parti: la narrazione tradotta e pulita, seguita dal separatore, seguito da tutti i tag raggruppati.
                                 
                ESEMPIO DI OUTPUT PERFETTO:
                Sei di fronte a un altare di pietra. Sullo sfondo, senti un rumore.
                --- TAGS ---
                <addItem itemType="WEAPON" weaponType="SWORD" itemName="Spada Lunga" modifier="+2" quantity="1"/>
                <choice_it id="choice_1_1">Esamina l'altare.</choice_it>
                                 
                Non aggiungere commenti o saluti.
                                 
                ---
                                 
                **DATI DELLA SCENA:**
                                 
                [CONTESTO DELL'AZIONE PRECEDENTE]
                $lastMessageText
                
                [TESTO NARRATIVO DA TRADURRE ]
                $sceneNarrativeEnglish
                
                [SCELTE NARRATIVE DA TRADURRE E INSERIRE NEI TAG <choice_it>]
                $choicesForPrompt
                
                [SCELTE DI DISCIPLINA DA TRADURRE E INSERIRE NEI TAG <discipline_it>]
                $disciplinesForPrompt
                ---
                
                **NARRATORE (in italiano, tono $currentTone):**
                """.trimIndent()
                Log.d(tag, "DEBUG_GEMMA_PROMPT_SENT: \n---\n${promptForGemma}\n---") // Per debug

                var stopStreamingToText = false // Flag per interrompere lo streaming al testo visualizzato


                dmEngine.sendMessage(promptForGemma)
                    .collect { token ->
                        stringRaw = stringRaw+ token
                        if (!stopStreamingToText) {
                            // Se il token contiene '<', prendi solo la parte precedente e ferma lo streaming.
                            log("DEBUG_TOKEN: $token")
                            if (token.contains("<") ) {
                                val partBeforeTag = token.substringBefore("<")
                                _streamingText.update { it + partBeforeTag }
                                stopStreamingToText = true // Interrompi lo streaming a _streamingText da qui in poi
                            } else if (token.contains("---")) {
                                val partBeforeTag = token.substringBefore("---")
                                _streamingText.update { it + partBeforeTag }
                                stopStreamingToText = true // Interrompi lo streaming a _streamingText da qui in poi
                            } else
                            {
                                _streamingText.update { it + token }
                            }
                        }
                    }

            } catch (e: Exception) {
                Log.e(
                    tag,
                    "Errore durante la generazione della narrazione della scena: ${e.message}",
                    e
                )
                log("ERRORE: Impossibile generare la narrazione del DM. ${e.message}")
                // Dentro MainViewModel.kt, sostituisci il blocco finally in sendMessage E processCurrentSceneNarrative

                // Dentro MainViewModel.kt, nel blocco try della generazione...

            } finally {
                log("Generazione completata. Inizio parsing della risposta.")
                log("STREAM: " + _streamingText.value )
                log("RAW: $stringRaw")
                val rawLLMResponse = stringRaw
                val respondingCharacter =
                    _gameCharacters.value.find { it.id == _respondingCharacterId.value }
                val llmLanguage = respondingCharacter?.language ?: Locale.ITALIAN.language

                // --- LOGICA DEFINITIVA DI PARSING ---

                // 1. Dividi la risposta di Gemma in narrazione e tag
                val parts = rawLLMResponse.split("--- TAGS ---", limit = 2)
                val narrativePart = parts.getOrNull(0)?.trim()
                    ?: rawLLMResponse // Fallback all'intera risposta se il separatore non c'è
                val tagsPart = parts.getOrNull(1)?.trim() ?: ""

                val allCommands = mutableListOf<EngineCommand>()
                // Applica una regex per rimuovere tutti i tag XML-like dalla narrativePart
                val cleanedNarrativeFromRegex = narrativePart.replace(Regex("<[^>]+>"), "")
                // 2. Processa la parte narrativa per pulirla e trovare comandi legacy (es. {STAT_MOD...})
                // NOTA: La pulizia dei tag dalla narrazione è ora gestita primariamente dal prompt di Gemma.
                // Questa funzione parseAndReplaceWithCommands si aspetta che la maggior parte dei tag XML-like
                // sia già stata rimossa dal modello.
//                val (cleanedNarrative, narrativeCommands) = stringTagParser.parseAndReplaceWithCommands(
//                    inputString = cleanedNarrativeFromRegex,
//                    currentActor = respondingCharacter?.type,
//                    lang = llmLanguage
//                )
//                allCommands.addAll(narrativeCommands)

                // 3. Processa la parte dei tag per ottenere i comandi di aggiornamento delle scelte
                if (tagsPart.isNotBlank()) {
                    val (_, choiceCommands) = stringTagParser.parseAndReplaceWithCommands(
                        inputString = tagsPart,
                        currentActor = respondingCharacter?.type,
                        lang = llmLanguage
                    )
                    allCommands.addAll(choiceCommands)
                }

                // 4. Aggiungi il messaggio di chat con la narrazione PULITA (solo se non è vuota)
                if (cleanedNarrativeFromRegex.isNotBlank()) {
                    val finalMessage = ChatMessage(
                        authorId = _respondingCharacterId.value ?: CharacterID.DM,
                        position = messageCounter.getAndIncrement(),
                        text = cleanedNarrativeFromRegex
                    )
                    _chatMessages.update { it + finalMessage }
                    autoSaveChatIfEnabled()
                }
                log("_chatMessages.value: " + _chatMessages.value)

                // 5. Esegui TUTTI i comandi raccolti
                if (allCommands.isNotEmpty()) {
                    processCommands(allCommands)
                }

                // 6. Resetta lo stato della generazione
                _isGenerating.value = false
                _streamingText.value = ""
                _respondingCharacterId.value = null
            }
        }
    }

    private fun prepareChoicesForScene(scene: Scene) {
        val requiresRoll = scene.narrativeText.italian?.contains(
            "Tabella dei Numeri Casuali",
            ignoreCase = true
        ) == true
        _isRandomNumberRollRequired.value = requiresRoll

        if (requiresRoll) {
            _activeNarrativeChoices.value = emptyList()
            _activeDisciplineChoices.value = emptyList()
        } else {
            populateChoicesForCurrentScene()
        }
    }

    private fun populateChoicesForCurrentScene() {
        val scene = _currentScene.value ?: return
        val hero = _gameCharacters.value.find { it.id == CharacterID.HERO }
        if (hero != null) {
            val availableNarrativeChoices = scene.choices ?: emptyList()
            _activeNarrativeChoices.value = availableNarrativeChoices

            val availableDisciplineChoices = scene.disciplineChoices?.filter { disciplineChoice ->
                gameRules.canUseDiscipline(hero, disciplineChoice.disciplineId, scene)
            } ?: emptyList()
            _activeDisciplineChoices.value = availableDisciplineChoices

            log("DEBUG: Scelte popolate per la scena ${scene.id}. Scelte narrative: ${availableNarrativeChoices.size}, Discipline: ${availableDisciplineChoices.size}")
        }
    }

    fun onNarrativeChoiceSelected(choice: NarrativeChoice) {
        val choiceMessage = ChatMessage(
            authorId = CharacterID.HERO,
            text = "*Sceglie di: ${choice.choiceText.italian}*",
            position = messageCounter.getAndIncrement()
        )
        _chatMessages.update { it + choiceMessage }
        navigateToScene(choice.nextSceneId)
    }

    fun onDisciplineChoiceSelected(choice: DisciplineChoice) {
        val choiceText =
            choice.choiceText?.italian
                ?: KAI_DISCIPLINES.find { it.id == choice.disciplineId }?.name
                ?: choice.disciplineId

        val choiceMessage = ChatMessage(
            authorId = CharacterID.HERO,
            text = "*Usa la disciplina: $choiceText*",
            position = messageCounter.getAndIncrement()
        )
        _chatMessages.update { it + choiceMessage }
        navigateToScene(choice.nextSceneId)
    }

    private fun navigateToScene(sceneId: String) {
        viewModelScope.launch {
            val nextScene = gameLogicManager.getSceneById(sceneId)
            if (nextScene != null) {
                log("Navigazione alla scena: ${nextScene.id}")

                _activeNarrativeChoices.value = emptyList()
                _activeDisciplineChoices.value = emptyList()
                _isRandomNumberRollRequired.value = false

                _currentScene.value = nextScene
                gameStateManager.loadSession()?.let {
                    if (!it.usedScenes.contains(nextScene.id)) {
                        it.usedScenes.add(nextScene.id)
                        gameStateManager.saveSession(it)
                    }
                }
                processCurrentSceneNarrative()
            } else {
                val errorMessage =
                    "ERRORE CRITICO: Scena con ID '$sceneId' non trovata nel file scenes.json."
                Log.e(tag, errorMessage)
                log(errorMessage)
            }
        }
    }

    suspend fun sendInitialDmPrompt(sessionData: SessionData) {
        if (sessionData.isStarted) {
            log("DEBUG: La sessione è già iniziata, non invio prompt iniziale DM.")
            processCurrentSceneNarrative(shouldGenerateNarration = false)
            return
        }

        val updatedSession = sessionData.copy(isStarted = true)
        gameStateManager.saveSession(updatedSession)
        log("DEBUG: Sessione marcata come avviata.")
        processCurrentSceneNarrative(shouldGenerateNarration = true)
    }

    private fun updatePlayerStatus() {
        val hero = _gameCharacters.value.find { it.id == CharacterID.HERO }
        hero?.let {
            val rank = gameRules.getKaiRank(it.kaiDisciplines.size)
            _kaiRank.value = rank
        }
    }

    fun onRollRandomNumber() {
        if (!_isRandomNumberRollRequired.value) return
        val result = Random.nextInt(0, 10)
        _randomNumberResult.value = result
    }

    fun resolveRandomNumberChoice() {
        val scene = _currentScene.value
        val rolledNumber = _randomNumberResult.value
        if (scene == null || rolledNumber == null) return

        val targetChoice = scene.choices?.find { choice ->
            val min = choice.minRoll
            val max = choice.maxRoll
            if (min != null && max != null) {
                rolledNumber in min..max
            } else {
                false
            }
        }

        if (targetChoice != null) {
            navigateToScene(targetChoice.nextSceneId)
        } else {
            log("ERRORE: Nessuna scelta trovata per il numero $rolledNumber nella scena ${scene.id}")
        }

        _randomNumberResult.value = null
        _isRandomNumberRollRequired.value = false
    }
}