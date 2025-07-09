// in file: java/io/github/luposolitario/immundanoctis/view/MainViewModel.kt

package io.github.luposolitario.immundanoctis.view

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.github.luposolitario.immundanoctis.data.*
import io.github.luposolitario.immundanoctis.engine.*
import io.github.luposolitario.immundanoctis.engine.rules.LoneWolfRules
import io.github.luposolitario.immundanoctis.util.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random


class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val tag: String? = this::class.simpleName

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

    private val gameStateManager = GameStateManager.getInstance(application)
    private val enginePreferences = EnginePreferences(application)
    private val themePreferences = ThemePreferences(application)
    private val llamaPreferences = LlamaPreferences(application)
    private val savePreferences = SavePreferences(application)
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _gameCharacters = MutableStateFlow<List<GameCharacter>>(emptyList())
    val gameCharacters: StateFlow<List<GameCharacter>> = _gameCharacters.asStateFlow()
    private val _gameHero = MutableStateFlow<GameCharacter?>(null)
    val gameHero: StateFlow<GameCharacter?> = _gameHero.asStateFlow()

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

    private val _currentScene = MutableStateFlow<Scene?>(null)
    val currentScene: StateFlow<Scene?> get() = _currentScene

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

    private val _inventoryFullState = MutableStateFlow<InventoryFullState?>(null)
    val inventoryFullState: StateFlow<InventoryFullState?> = _inventoryFullState.asStateFlow()

    private val _uiFeedbackEvent = MutableSharedFlow<String>()
    val uiFeedbackEvent: SharedFlow<String> = _uiFeedbackEvent.asSharedFlow()

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
        viewModelScope.launch(Dispatchers.IO) {
            GameLogicManager.loadAllScenes(application.applicationContext)
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

    fun loadGameSession(startFresh: Boolean = false) {
        val session = gameStateManager.loadSession()
        val actualIsNewAdventure = (startFresh)

        val currentSession = session
        _gameHero.value = currentSession.hero
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
                GameLogicManager.adventureName
            GameLogicManager.resetUsedScenes()
            _currentScene.value = GameLogicManager.selectRandomStartScene(Genre.FANTASY)
            log("Scena iniziale NUOVA AVVENTURA impostata da GameLogicManager: ${_currentScene.value?.id ?: "Nessuna scena iniziale"}. Nome Avventura: ${_sessionName.value}")
            viewModelScope.launch {
                sendInitialDmPrompt(currentSession)
            }
        } else {
            val lastSceneId = currentSession.usedScenes.lastOrNull()
            _currentScene.value = if (lastSceneId != null) {
                GameLogicManager.getSceneById(lastSceneId)
            } else {
                GameLogicManager.selectRandomStartScene(Genre.FANTASY)
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

        val heroCharacter = _gameHero.value
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

                val parts = rawLLMResponse.split("--- TAGS ---", limit = 2)
                val narrativePart = parts.getOrNull(0)?.trim() ?: ""
                val tagsPart = parts.getOrNull(1)?.trim() ?: ""

                val allCommands = mutableListOf<EngineCommand>()

                if (narrativePart.isNotBlank()) {
                    val (cleanedNarrative, narrativeCommands) = stringTagParser.parseAndReplaceWithCommands(
                        inputString = narrativePart,
                        currentActor = respondingCharacter?.type,
                        lang = llmLanguage
                    )
                    allCommands.addAll(narrativeCommands)

                    val finalMessage = ChatMessage(
                        authorId = _respondingCharacterId.value ?: CharacterID.DM,
                        position = messageCounter.getAndIncrement(),
                        text = cleanedNarrative
                    )
                    _chatMessages.update { it + finalMessage }
                    autoSaveChatIfEnabled()
                }

                if (tagsPart.isNotBlank()) {
                    val (_, choiceCommands) = stringTagParser.parseAndReplaceWithCommands(
                        inputString = tagsPart,
                        currentActor = respondingCharacter?.type,
                        lang = llmLanguage
                    )
                    allCommands.addAll(choiceCommands)
                }

                viewModelScope.launch {
                    processCommands(allCommands)
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
        Log.d(tag, message)
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
            _currentScene.value = GameLogicManager.selectRandomStartScene(Genre.FANTASY)
            log("Scena reimpostata a una scena START casuale di genere FANTASY.")
            GameLogicManager.resetUsedScenes()
            val currentSession =
                gameStateManager.loadSession()
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
        var hero = currentSession.hero
        var sessionModified = false
        val newCommandsToProcess = mutableListOf<EngineCommand>()

        commands.forEach { command ->
            Log.d(
                tag,
                "Executing command: ${command.commandName} with params: ${command.parameters}"
            )
            when (command.commandName) {
                "addItem" -> {
                    val itemName = command.parameters["itemName"] as? String
                    val itemTypeStr = command.parameters["itemType"] as? String
                    val quantity = (command.parameters["quantity"] as? String)?.toIntOrNull() ?: 1

                    if (itemName != null && itemTypeStr != null) {
                        try {
                            val itemType = ItemType.valueOf(itemTypeStr.uppercase())
                            val inventory = hero.details?.inventory ?: mutableListOf()

                            if (itemType == ItemType.GOLD) {
                                val goldItem = inventory.find { it.type == ItemType.GOLD }
                                if (goldItem != null) {
                                    goldItem.quantity += quantity
                                } else {
                                    inventory.add(GameItem(name = itemName, type = ItemType.GOLD, quantity = quantity))
                                }
                                log("💰 Aggiunte ${quantity} Corone d'Oro.")
                                viewModelScope.launch { _uiFeedbackEvent.emit("Hai trovato ${quantity} Corone d'Oro!") }
                                sessionModified = true
                            } else {
                                val newItem = GameItem(name = itemName, type = itemType, quantity = quantity, notes = command.parameters["notes"] as? String)
                                val weaponCount = inventory.count { it.type == ItemType.WEAPON }
                                val backpackItemCount = inventory.count { it.type == ItemType.BACKPACK_ITEM }
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
                                    viewModelScope.launch { _uiFeedbackEvent.emit("Hai trovato: ${newItem.name}") }
                                    sessionModified = true
                                } else {
                                    log("⚠️ Inventario pieno per il tipo $itemType. In attesa della decisione del giocatore.")
                                    viewModelScope.launch { _uiFeedbackEvent.emit("Hai trovato: ${newItem.name}, ma il tuo inventario è pieno!") }
                                }
                            }
                        } catch (e: IllegalArgumentException) {
                            log("❌ ERRORE: Tipo di oggetto non valido '$itemTypeStr' per il comando addItem.")
                        }
                    }
                }

                "setFlag" -> {
                    val flagName = command.parameters["flagName"] as? String
                    val flagValue = command.parameters["flagValue"] as? String
                    if (flagName != null && flagValue != null) {
                        hero.details?.gameFlags?.set(flagName, flagValue)
                        log("🚩 Flag impostato: '$flagName' a '$flagValue'")
                        sessionModified = true
                    } else {
                        log("❌ ERRORE: Parametri mancanti per il comando setFlag.")
                    }
                }

                "rollForQuantity" -> {
                    val itemName = command.parameters["item"] as? String
                    val baseValueStr = command.parameters["baseValue"] as? String
                    if (itemName != null && baseValueStr != null) {
                        val baseValue = baseValueStr.toIntOrNull() ?: 0
                        val roll = rollDice(1, 10) -1 // Tira un dado da 10 facce (0-9)
                        val finalQuantity = baseValue + roll

                        log("🎲 Comando rollForQuantity: Base=$baseValue, Tiro=$roll, Quantità Finale=$finalQuantity")

                        // Creiamo un nuovo comando addItem e lo aggiungiamo alla lista da processare
                        val addItemCommand = EngineCommand(
                            commandName = "addItem",
                            parameters = mapOf(
                                "itemName" to itemName,
                                "itemType" to "GOLD", // Assumiamo sia sempre oro per ora
                                "quantity" to finalQuantity.toString()
                            )
                        )
                        newCommandsToProcess.add(addItemCommand)
                    }
                }

                "checkStatAndJump" -> {
                    val statName = command.parameters["statName"] as? String
                    val operator = command.parameters["operator"] as? String
                    val valueStr = command.parameters["value"] as? String
                    val targetScene = command.parameters["targetScene"] as? String

                    if (statName != null && operator != null && valueStr != null && targetScene != null) {
                        val value = valueStr.toIntOrNull()
                        val heroStats = hero.stats
                        if (value != null && heroStats != null) {
                            val statToCompare = when (statName.uppercase()) {
                                "ENDURANCE", "RESISTENZA" -> heroStats.resistenza
                                "COMBATSKILL", "COMBATTIVITA" -> heroStats.combattivita
                                else -> null
                            }

                            if (statToCompare != null) {
                                val conditionMet = when (operator) {
                                    "LESS_THAN_OR_EQUAL" -> statToCompare <= value
                                    "GREATER_THAN_OR_EQUAL" -> statToCompare >= value
                                    "EQUALS" -> statToCompare == value
                                    else -> false
                                }

                                if (conditionMet) {
                                    log("✅ Condizione IF_STAT verificata ($statName $operator $value). Navigazione a '$targetScene'.")
                                    if (targetScene.equals("DEATH", ignoreCase = true)) {
                                        _isHeroDead.value = true
                                    } else {
                                        navigateToScene(targetScene)
                                    }
                                } else {
                                    log("ℹ️ Condizione IF_STAT non verificata ($statName $operator $value). Nessuna azione.")
                                }
                            }
                        }
                    }
                }

                "updateChoiceText" -> {

                    val sceneId = command.parameters["sceneId"] as? String
                    val progressiveId = command.parameters["progressiveId"] as? String
                    val italianText = command.parameters["italianText"] as? String
                    if (sceneId != null && progressiveId !=null && italianText != null) {
                        updateNarrativeChoiceText(sceneId , progressiveId, italianText)
                    }
                }
                "updateDisciplineChoiceText" -> {
                    val disciplineId = command.parameters["id"] as? String
                    val italianText = command.parameters["italianText"] as? String
                    if (disciplineId != null && italianText != null) {
                        updateDisciplineChoiceText(disciplineId, italianText)
                    }
                }

                "requireAction" -> {
                    val inventory = hero.details?.inventory ?: mutableListOf()
                    val meal = inventory.find { it.name == "Pasto" }

                    if (meal != null && meal.quantity > 0) {
                        meal.quantity--
                        if (meal.quantity == 0) {
                            inventory.remove(meal)
                        }
                        log("✅ Pasto consumato. Il giocatore ha mangiato.")
                        viewModelScope.launch { _uiFeedbackEvent.emit("Hai consumato un Pasto.") }
                    } else {
                        val currentEndurance = hero.stats?.resistenza ?: 0
                        val newEndurance = (currentEndurance - 3).coerceAtLeast(0)
                        val updatedStats = hero.stats?.copy(resistenza = newEndurance)
                        hero = hero.copy(stats = updatedStats)
                        if (newEndurance <= 0) {
                            _isHeroDead.value = true
                        }
                        log("❌ Nessun pasto disponibile. Il giocatore perde 3 punti Resistenza.")
                        viewModelScope.launch { _uiFeedbackEvent.emit("Non hai cibo! Perdi 3 punti Resistenza.") }
                    }
                    sessionModified = true
                }

                "removeAllItems" -> {
                    val itemTypeToRemoveStr = command.parameters["type"] as? String
                    if (itemTypeToRemoveStr != null) {
                        try {
                            val itemTypeToRemove = ItemType.valueOf(itemTypeToRemoveStr.uppercase())
                            val inventory = hero.details?.inventory ?: mutableListOf()
                            val itemsRemoved = inventory.removeAll { item ->
                                item.type == itemTypeToRemove && item.isDiscardable
                            }

                            if (itemsRemoved) {
                                log("‼️ Rimosso/i ${itemTypeToRemove.name} dall'inventario.")
                                viewModelScope.launch { _uiFeedbackEvent.emit("Hai perso i tuoi oggetti di tipo ${itemTypeToRemove.name}!") }
                                sessionModified = true
                            }
                        } catch (e: IllegalArgumentException) {
                            log("❌ ERRORE: Tipo di oggetto non valido '$itemTypeToRemoveStr' per il comando removeAllItems.")
                        }
                    }
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
                                "COMBATSKILL", "COMBATTIVITA" -> {
                                    newCombatSkill = (newCombatSkill + amount).coerceAtLeast(0)
                                    log("STAT MOD: Combattività modificata di $amount. Nuovo valore: $newCombatSkill")
                                }
                                "ENDURANCE", "RESISTENZA" -> {
                                    newEndurance = (newEndurance + amount).coerceAtLeast(0)
                                    log("STAT MOD: Resistenza modificata di $amount. Nuovo valore: $newEndurance")
                                }
                            }
                            if (newEndurance <= 0) {
                                _isHeroDead.value = true
                            }
                            // --- 👇 MODIFICA CHIAVE QUI 👇 ---
                            // 1. Crea una NUOVA istanza di LoneWolfStats
                            val updatedStats = heroStats.copy(combattivita = newCombatSkill, resistenza = newEndurance)
                            // 2. Crea una NUOVA istanza di GameCharacter con le statistiche aggiornate
                            hero = hero.copy(stats = updatedStats)

                            viewModelScope.launch { _uiFeedbackEvent.emit("La tua $statName è cambiata di $amount!") }
                            sessionModified = true
                        } else if (amountStr.equals("MAX_RESISTANCE_RESTORE", ignoreCase = true)) {
                        // Logica speciale per ripristinare la resistenza al massimo (se mai servirà)
                        // Questa è una previsione basata sui libri game, dove a volte si riposa completamente.
                        // Per ora, questa logica non è usata, ma è pronta.
                        }
                    }
                }
                else -> {
                    log("⚠️ Comando sconosciuto o non ancora implementato: ${command.commandName}")
                }
            }
        }

        // Se sono stati generati nuovi comandi (es. da rollForQuantity), li processiamo
        if (newCommandsToProcess.isNotEmpty()) {
            processCommands(newCommandsToProcess)
        }

        if (sessionModified) {
            gameStateManager.saveSession(currentSession.copy(hero = hero))
            _gameCharacters.value = gameStateManager.loadSession()?.characters!!
            _gameHero.value =  gameStateManager.loadSession()?.hero!!
            log("Salvataggio sessione dopo l'aggiornamento.")
        }
    }

    fun resolveInventoryExchange(itemToDiscard: GameItem, newItem: GameItem) {
        viewModelScope.launch {
            val session = gameStateManager.loadSession() ?: return@launch
            val hero = session.hero
            val inventory = hero.details?.inventory ?: return@launch
            inventory.remove(itemToDiscard)
            inventory.add(newItem)
            gameStateManager.saveSession(session)
            _gameCharacters.value = gameStateManager.loadSession()?.characters!!
            _gameHero.value =  gameStateManager.loadSession()?.hero!!
            log("✅ Scambiato '${itemToDiscard.name}' con '${newItem.name}'.")
            _uiFeedbackEvent.emit("'${itemToDiscard.name}' scartato, '${newItem.name}' raccolto.")
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

    private fun updateNarrativeChoiceText(sceneId: String, progressiveId: String,italianText: String) {
        _activeNarrativeChoices.update { currentChoices ->
            currentChoices.map { choice ->
                if (choice.scene == sceneId && choice.progressive == progressiveId) {
                    choice.copy(choiceText = choice.choiceText.copy(italian = italianText))
                } else {
                    choice
                }
            }
        }
        log("Testo per la scelta narrativa '$sceneId'_'$progressiveId' aggiornato a: '$italianText'")
    }

    private fun updateDisciplineChoiceText(disciplineId: String, italianText: String) {
        _activeDisciplineChoices.update { currentChoices ->
            currentChoices.map { choice ->
                if (choice.discipline == disciplineId) {
                    choice.copy(choiceText = choice.choiceText?.copy(italian = italianText))
                } else {
                    choice
                }
            }
        }
        log("Testo per la scelta di disciplina '$disciplineId' aggiornato a: '$italianText'")
    }


    private fun buildGemmaPromptForScene(scene: Scene, lastMessageText: String): String {
        val sceneNarrativeEnglish = scene.narrativeText.english ?: ""

        // Modificato per corrispondere al formato richiesto dal prompt migliorato
        val choicesForPrompt = activeNarrativeChoices.value.joinToString("\n") {
            "* SCELTA: ID_SCENA:\"${it.scene}\", ID_PROGRESSIVO:\"${it.progressive}\", TESTO: \"${it.choiceText.english}\""
        }.ifEmpty { "Nessuna scelta narrativa." }

        // Modificato per corrispondere al formato richiesto dal prompt migliorato
        val disciplinesForPrompt = activeDisciplineChoices.value.joinToString("\n") {
            val text = it.choiceText?.english ?: "Usa la disciplina ${it.discipline}"
            "* DISCIPLINA: ID: \"${it.discipline}\", TESTO: \"$text\""
        }.ifEmpty { "Nessuna scelta di disciplina." }

        val currentTone = savePreferences.narrativeTone
        val sceneTypeInfo = "INFO: Il tipo di scena è '${scene.sceneType}' e il livello di sfida è '${scene.challengeLevel}'."

        // --- PROMPT AGGIORNATO CON ISTRUZIONI PIÙ STRINGENTI E FORMATO SCELTE CORRETTO ---
        return """
        Tu sei il Dungeon Master per un libro-gioco. Il tuo compito è elaborare una scena per il giocatore.
        
        Segui queste istruzioni ESATTAMENTE:
        
        1.  **Traduci e Armonizza**: Leggi il [TESTO NARRATIVO DA TRADURRE] e le [SCELTE] fornite. Traduci tutto in italiano, mantenendo uno stile coerente e un tono narrativo '$currentTone'.
        2.  **REGOLA FONDAMENTALE**: La tua risposta deve iniziare DIRETTAMENTE con la traduzione del [TESTO NARRATIVO DA TRADURRE]. NON includere o ripetere il testo da [CONTESTO DELL'AZIONE PRECEDENTE].
        3.  **Formatta l'Output**:
            * Scrivi prima la narrazione tradotta e pulita.
            * Arricchisci la narrativa aggiungendo dettagli per tutti i testi secondo il tono scelto
            * Poi, aggiungi il separatore `--- TAGS ---`.
            * Sotto il separatore, inserisci **TUTTE** le traduzioni delle scelte e delle discipline che ti sono state fornite in [SCELTE DA TRADURRE E INSERIRE NEI TAG], usando i seguenti formati:
                * Per ogni SCELTA, usa il tag `<choice_it scene="ID_SCENA" progressivo="ID_PROGRESSIVO">Testo Tradotto.</choice_it>`.
                * Per ogni DISCIPLINA, usa il tag `<discipline_it id="ID_DELLA_DISCIPLINA">Testo Tradotto.</discipline_it>`.
        
        **NON GENERARE MAI TAG di meccaniche di gioco come `<ADD_ITEM...>` o `<STAT_MOD...>` nella tua risposta.**
        
        ---
        
        **DATI DELLA SCENA:**
        
        [METADATI SCENA]
        $sceneTypeInfo
    
        [CONTESTO DELL'AZIONE PRECEDENTE]
        $lastMessageText
        
        [TESTO NARRATIVO DA TRADURRE]
        $sceneNarrativeEnglish
        
        [SCELTE DA TRADURRE E INSERIRE NEI TAG]
        $choicesForPrompt
        $disciplinesForPrompt
        ---
        
        **NARRATORE (in italiano, tono $currentTone):**
        """.trimIndent()
    }


    private suspend fun processCurrentSceneNarrative(shouldGenerateNarration: Boolean = true) {
        val scene = _currentScene.value ?: run {
            log("ERRORE: Tentativo di processare una scena nulla.")
            return
        }

        // --- FASE 1: ESECUZIONE IMMEDIATA DELLE MECCANICHE DI GIOCO DAL JSON ---
        val gameMechanics = scene.gameMechanics
        if (!gameMechanics.isNullOrEmpty()) {
            log("Trovate ${gameMechanics.size} meccaniche di gioco predefinite nella scena: $gameMechanics")
            val commandsToExecute = mutableListOf<EngineCommand>()

            gameMechanics.forEach { mechanicString ->
                // Usiamo il parser sulla singola stringa di meccanica
                val (_, commands) = stringTagParser.parseAndReplaceWithCommands(mechanicString, CharacterType.DM)
                commandsToExecute.addAll(commands)
            }

            if (commandsToExecute.isNotEmpty()) {
                // -->> MODIFICA CRUCIALE: ESEGUIAMO SUBITO I COMANDI <<--
                processCommands(commandsToExecute)
                log("LOG SPECIALIZZATO: Eseguiti ${commandsToExecute.size} comandi da gameMechanics.")
            }
        }
        // --- FINE FASE 1 ---

        if (_isHeroDead.value) {
            log("Eroe morto dopo l'esecuzione delle meccaniche. Interrompo l'elaborazione della scena.")
            return
        }

        prepareChoicesForScene(scene)

        if (!shouldGenerateNarration) {
            log("Sessione caricata. La narrazione non viene rigenerata.")
            return
        }

        // --- FASE 2: GENERAZIONE NARRATIVA CON GEMMA ---
        if (_isGenerating.value) return
        _isGenerating.value = true
        _streamingText.value = ""
        _respondingCharacterId.value = CharacterID.DM
        var stringRaw = ""

        try {
            _engineLoadingState.first { it is EngineLoadingState.Success }

            val lastMessageText = _chatMessages.value.lastOrNull()?.text ?: "L'avventura ha inizio."
            val promptForGemma = buildGemmaPromptForScene(scene, lastMessageText)
            Log.d(tag, "DEBUG_GEMMA_PROMPT_SENT (Refactored): \n---\n$promptForGemma\n---")

            var stopStreamingToText = false

            dmEngine.sendMessage(promptForGemma)
                .collect { token ->
                    stringRaw += token
                    if (!stopStreamingToText) {
                        if (token.contains("---")) {
                            val partBeforeTag = token.substringBefore("---")
                            _streamingText.update { it + partBeforeTag }
                            stopStreamingToText = true
                        } else {
                            _streamingText.update { it + token }
                        }
                    }
                }

        } catch (e: Exception) {
            Log.e(tag, "Errore durante la generazione della narrazione della scena: ${e.message}", e)
            log("ERRORE: Impossibile generare la narrazione del DM. ${e.message}")
        } finally {
            log("Generazione completata. Inizio parsing della risposta.")
            log("RAW: $stringRaw")

            val parts = stringRaw.split("--- TAGS ---", limit = 2)
            val narrativePart = _streamingText.value.trim()
            val tagsPart = parts.getOrNull(1)?.trim() ?: ""

            if (narrativePart.isNotBlank()) {
                val finalMessage = ChatMessage(
                    authorId = _respondingCharacterId.value ?: CharacterID.DM,
                    position = messageCounter.getAndIncrement(),
                    text = narrativePart
                )
                _chatMessages.update { it + finalMessage }
                autoSaveChatIfEnabled()
            }

            if (tagsPart.isNotBlank()) {
                val (_, choiceCommands) = stringTagParser.parseAndReplaceWithCommands(tagsPart, CharacterType.DM)
                if (choiceCommands.isNotEmpty()) {
                    processCommands(choiceCommands)
                }
            }

            _isGenerating.value = false
            _streamingText.value = ""
            _respondingCharacterId.value = null
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
        val hero = _gameHero.value
        if (hero != null) {
            val narrativeChoices = scene.choices ?: emptyList()
            val playerFlags = hero.details?.gameFlags ?: emptyMap()
            val playerInventory = hero.details?.inventory ?: emptyList()

            val availableNarrativeChoices = narrativeChoices.filter { choice ->
                var conditionMet = true

                choice.requiredFlag?.let { required ->
                    val playerFlagValue = playerFlags[required.name]
                    conditionMet = playerFlagValue == required.value
                }
                if (conditionMet && choice.requiredItem != null) {
                    conditionMet = playerInventory.any { it.name == choice.requiredItem }
                }
                conditionMet
            }

            _activeNarrativeChoices.value = availableNarrativeChoices

            val availableDisciplineChoices = scene.disciplineChoices?.filter { disciplineChoice ->
                gameRules.canUseDiscipline(hero, disciplineChoice.discipline, scene)
            } ?: emptyList()
            _activeDisciplineChoices.value = availableDisciplineChoices

            log("DEBUG: Scelte popolate per la scena ${scene.id}. Scelte totali: ${narrativeChoices.size}, Scelte valide: ${availableNarrativeChoices.size}, Discipline: ${availableDisciplineChoices.size}")
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
                ?: KAI_DISCIPLINES.find { it.id == choice.discipline }?.name
                ?: choice.discipline

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
            val nextScene = GameLogicManager.getSceneById(sceneId)
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
                        _gameCharacters.value = gameStateManager.loadSession()?.characters!!
                        _gameHero.value =  gameStateManager.loadSession()?.hero!!
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
        _gameCharacters.value = gameStateManager.loadSession()?.characters!!
        _gameHero.value =  gameStateManager.loadSession()?.hero!!
        log("DEBUG: Sessione marcata come avviata.")
        processCurrentSceneNarrative(shouldGenerateNarration = true)
    }

    private fun updatePlayerStatus() {
        val hero = _gameHero.value
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