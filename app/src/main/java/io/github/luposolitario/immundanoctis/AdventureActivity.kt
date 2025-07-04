package io.github.luposolitario.immundanoctis

import androidx.compose.ui.text.font.FontWeight
import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.TextButton
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.data.CharacterType
import io.github.luposolitario.immundanoctis.data.ChatMessage
import io.github.luposolitario.immundanoctis.data.DisciplineChoice
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.GameItem
import io.github.luposolitario.immundanoctis.data.*
import io.github.luposolitario.immundanoctis.data.NarrativeChoice
import io.github.luposolitario.immundanoctis.data.Scene
import io.github.luposolitario.immundanoctis.data.SessionData
import io.github.luposolitario.immundanoctis.engine.GameLogicManager
import io.github.luposolitario.immundanoctis.engine.TokenInfo
import io.github.luposolitario.immundanoctis.service.TtsService
import io.github.luposolitario.immundanoctis.ui.adventure.AdventureHeader
import io.github.luposolitario.immundanoctis.ui.adventure.ChoicesContainer
import io.github.luposolitario.immundanoctis.ui.adventure.GeneratingIndicator
import io.github.luposolitario.immundanoctis.ui.adventure.MessageBubble
import io.github.luposolitario.immundanoctis.ui.adventure.MessageInput
import io.github.luposolitario.immundanoctis.ui.adventure.PlayerActionsBar
import io.github.luposolitario.immundanoctis.ui.adventure.TokenSemaphoreIndicator
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.GameStateManager
import io.github.luposolitario.immundanoctis.util.ModelPreferences
import io.github.luposolitario.immundanoctis.util.SavePreferences
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import io.github.luposolitario.immundanoctis.util.TtsPreferences
import io.github.luposolitario.immundanoctis.view.MainViewModel
import io.github.luposolitario.immundanoctis.view.MainViewModel.EngineLoadingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AdventureActivity : ComponentActivity() {
    private val tag: String? = this::class.simpleName
    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }
    private val modelPreferences by lazy { ModelPreferences(applicationContext) }
    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    private val ttsPreferences by lazy { TtsPreferences(applicationContext) }
    private var ttsService: TtsService? = null
    private val savePreferences by lazy { SavePreferences(applicationContext) }
    private lateinit var gameStateManager: GameStateManager
    private lateinit var gameLogicManager: GameLogicManager
    private val currentSceneFlow = MutableStateFlow<Scene?>(null)


    override fun onCreate(savedInstanceState: Bundle?) {
        gameStateManager = GameStateManager(applicationContext)
        gameLogicManager = GameLogicManager(applicationContext)

        val session = gameStateManager.loadSession() ?: gameStateManager.createDefaultSession()
        super.onCreate(savedInstanceState)

        viewModel.loadGameSession()

        ttsService = TtsService(this) { /* TTS Ready */ }

        val dmModel = modelPreferences.getDmModel()
        val playerModel = modelPreferences.getPlayerModel()

        if (dmModel == null && playerModel == null) {
            viewModel.log("Nessun modello configurato. Vai in ConfigurationActivity per scaricarli.")
        } else {
            viewModel.log("Caricamento motori...")
            viewModel.loadEngines(
                dmModelPath = dmModel?.destination?.path,
                playerModelPath = playerModel?.destination?.path
            )
        }

        setContent {
            val useDarkTheme = themePreferences.useDarkTheme(isSystemInDarkTheme())
            ImmundaNoctisTheme(darkTheme = useDarkTheme) {

                val loadingState by viewModel.engineLoadingState.collectAsState()

                when (loadingState) {
                    is EngineLoadingState.Loading -> {
                        LoadingScreen()
                    }

                    is EngineLoadingState.Success -> {
                        val view = LocalView.current
                        if (!view.isInEditMode) {
                            LaunchedEffect(key1 = view) {
                                val window = (view.context as? Activity)?.window
                                if (window != null) {
                                    window.statusBarColor = Color.Black.toArgb()
                                    WindowCompat.getInsetsController(
                                        window, view
                                    ).isAppearanceLightStatusBars = false
                                }
                            }
                        }

                        val sessionName by viewModel.sessionName.collectAsState()
                        val characters by viewModel.gameCharacters.collectAsState()
                        val chatMessages by viewModel.chatMessages.collectAsState()
                        val streamingText by viewModel.streamingText.collectAsState()
                        val isGenerating by viewModel.isGenerating.collectAsState()
                        val conversationTargetId by viewModel.conversationTargetId.collectAsState()
                        val respondingCharacterId by viewModel.respondingCharacterId.collectAsState()
                        val isAutoReadEnabled = ttsPreferences.isAutoReadEnabled()
                        val isAutoSaveEnabled = savePreferences.isAutoSaveEnabled
                        val tokenInfo by viewModel.activeTokenInfo.collectAsState()
                        val kaiRank by viewModel.kaiRank.collectAsState()
                        val narrativeChoices by viewModel.activeNarrativeChoices.collectAsState()
                        val disciplineChoices by viewModel.activeDisciplineChoices.collectAsState()
                        val isChatEnabled = savePreferences.isChatEnabled
                        val currentScene by viewModel.currentScene.collectAsState()
                        val isDiceRollRequired by viewModel.isRandomNumberRollRequired.collectAsState()
                        val randomNumberResult by viewModel.randomNumberResult.collectAsState()


                        if (randomNumberResult != null) {
                            AlertDialog(
                                onDismissRequest = { viewModel.resolveRandomNumberChoice() },
                                icon = { Icon(Icons.Default.Casino, contentDescription = "Lancio del Dado") },
                                title = { Text("Risultato del Fato") },
                                text = {
                                    Text(
                                        "Hai ottenuto: $randomNumberResult",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                },
                                confirmButton = {
                                    Button(onClick = { viewModel.resolveRandomNumberChoice() }) {
                                        Text("Continua")
                                    }
                                }
                            )
                        }


                        LaunchedEffect(chatMessages) {
                            if (isAutoReadEnabled) {
                                chatMessages.lastOrNull()?.let { lastMessage ->
                                    val author = characters.find { it.id == lastMessage.authorId }
                                    if (author != null && author.id != CharacterID.HERO && lastMessage.text.isNotBlank()) {
                                        ttsService?.speak(lastMessage.text, author)
                                    }
                                }
                            }
                        }
                        DisposableEffect(Unit) {
                            onDispose {
                                ttsService?.shutdown()
                            }
                        }

                        if (characters.isEmpty()) {
                            LoadingScreen()
                        } else {
                            AdventureChatScreen(
                                viewModel=viewModel,
                                isAutoSaveEnabled = isAutoSaveEnabled,
                                sessionName = sessionName,
                                characters = characters,
                                messages = chatMessages,
                                streamingText = streamingText,
                                isGenerating = isGenerating,
                                selectedCharacterId = conversationTargetId,
                                respondingCharacterId = respondingCharacterId,
                                tokenInfo = tokenInfo,
                                kaiRank = kaiRank,
                                narrativeChoices = narrativeChoices,
                                disciplineChoices = disciplineChoices,
                                isChatEnabled = isChatEnabled,
                                currentSceneId = currentScene?.id,
                                isDiceRollEnabled = isDiceRollRequired,
                                onDiceRollClicked = { viewModel.onRollRandomNumber() },
                                onMessageSent = { messageText ->
                                    viewModel.sendMessage(messageText, conversationTargetId)
                                },
                                onCharacterSelected = { characterId ->
                                    viewModel.setConversationTarget(characterId)
                                },
                                onStopGeneration = {
                                    viewModel.stopGeneration()
                                },
                                onSaveChat = {
                                    viewModel.onSaveChatClicked()
                                },
                                onTranslateMessage = { messageId ->
                                    viewModel.translateMessage(messageId)
                                },
                                onPlayMessage = { message ->
                                    val author = characters.find { it.id == message.authorId }
                                    if (author != null) {
                                        ttsService?.speak(message.text, author)
                                    }
                                },
                                onResetSession = { viewModel.resetSession() },
                                onNarrativeChoice = { viewModel.onNarrativeChoiceSelected(it) },
                                onDisciplineChoice = { viewModel.onDisciplineChoiceSelected(it) }
                            )
                        }
                    }

                    is EngineLoadingState.Error -> {
                        val errorMessage = (loadingState as EngineLoadingState.Error).message
                        ErrorScreen(
                            errorMessage = errorMessage ?: "Errore sconosciuto", onRetry = {
                                val dmModel = modelPreferences.getDmModel()
                                val playerModel = modelPreferences.getPlayerModel()
                                viewModel.loadEngines(
                                    dmModelPath = dmModel?.destination?.path,
                                    playerModelPath = playerModel?.destination?.path
                                )
                            })
                    }
                }
            }
        }

        if (!session.isStarted) {
            currentSceneFlow.value =
                gameLogicManager.selectRandomStartScene(Genre.FANTASY)
            Log.d(
                tag,
                "Scena iniziale NUOVA AVVENTURA impostata da GameLogicManager: ${currentSceneFlow.value?.id ?: "Nessuna scena iniziale"}"
            )

            lifecycleScope.launch {
                viewModel.sendInitialDmPrompt(session)
            }
        } else {
            val lastSceneId = session.usedScenes.lastOrNull()
            currentSceneFlow.value = if (lastSceneId != null) {
                gameLogicManager.getSceneById(lastSceneId)
            } else {
                gameLogicManager.selectRandomStartScene(Genre.FANTASY)
            }
            Log.d(
                tag,
                "Scena sessione esistente impostata a: ${currentSceneFlow.value?.id ?: "Nessuna scena valida trovata. Riprovo con casuale START."}"
            )
            lifecycleScope.launch {
                viewModel.sendInitialDmPrompt(session)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsService?.shutdown()
    }
}

@Composable
fun LoadingScreen(text: String = "Caricamento motori AI...") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ErrorScreen(errorMessage: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Errore Critico",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                errorMessage,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Riprova")
            }
        }
    }
}

// In fondo al file AdventureActivity.kt

@Composable
fun InventoryFullDialog(
    state: MainViewModel.InventoryFullState,
    onConfirm: (GameItem, GameItem) -> Unit,
    onDismiss: () -> Unit
) {
    val title = if (state.itemType == ItemType.WEAPON) "Slot Armi Pieno" else "Zaino Pieno"
    val message = "Hai trovato: ${state.newItem.name}. Per prenderlo, devi scartare uno degli oggetti che già possiedi."

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Scegli cosa scartare:", fontWeight = FontWeight.Bold)
                // Lista di oggetti da scartare
                state.existingItems.forEach { itemToDiscard ->
                    Button(
                        onClick = { onConfirm(itemToDiscard, state.newItem) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text("Scarta ${itemToDiscard.name}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla (lascia l'oggetto)")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdventureChatScreen(
    viewModel: MainViewModel, // <-- PARAMETRO AGGIUNTO QUI
    isAutoSaveEnabled: Boolean,
    sessionName: String,
    characters: List<GameCharacter>,
    messages: List<ChatMessage>,
    streamingText: String,
    isGenerating: Boolean,
    selectedCharacterId: String,
    respondingCharacterId: String?,
    tokenInfo: TokenInfo,
    kaiRank: String,
    narrativeChoices: List<NarrativeChoice>,
    disciplineChoices: List<DisciplineChoice>,
    isChatEnabled: Boolean,
    currentSceneId: String?,
    isDiceRollEnabled: Boolean,
    onDiceRollClicked: () -> Unit,
    onMessageSent: (String) -> Unit,
    onCharacterSelected: (String) -> Unit,
    onStopGeneration: () -> Unit,
    onSaveChat: () -> Unit,
    onTranslateMessage: (String) -> Unit,
    onPlayMessage: (ChatMessage) -> Unit,
    onResetSession: () -> Unit,
    onNarrativeChoice: (NarrativeChoice) -> Unit,
    onDisciplineChoice: (DisciplineChoice) -> Unit
) {
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }
    val hero = characters.find { it.type == CharacterType.PLAYER }
    val inventoryFullState by viewModel.inventoryFullState.collectAsState()

    // Questo codice va all'interno del corpo della Composable,
// al di fuori dello Scaffold.
    inventoryFullState?.let { state ->
        InventoryFullDialog(
            state = state,
            onConfirm = { itemToDiscard, newItem ->
                viewModel.resolveInventoryExchange(itemToDiscard, newItem)
            },
            onDismiss = {
                viewModel.dismissInventoryFullDialog()
            }
        )
    }


    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty() || streamingText.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TokenSemaphoreIndicator(
                        tokenInfo = tokenInfo, onResetSession = onResetSession
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(sessionName)
                }
            }, actions = {
                if (currentSceneId != null) {
                    Text(
                        text = "Paragrafo: $currentSceneId",
                        modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opzioni")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (!isAutoSaveEnabled) {
                            DropdownMenuItem(text = { Text("Salva Chat Manualmente") }, onClick = {
                                onSaveChat()
                                showMenu = false
                            }, leadingIcon = {
                                Icon(
                                    Icons.Outlined.Save, contentDescription = "Salva"
                                )
                            })
                        }
                    }
                }
            })
        }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AdventureHeader(
                characters = characters,
                selectedCharacterId = selectedCharacterId,
                onCharacterClick = onCharacterSelected,
                isChatEnabled = isChatEnabled
            )
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                val customTextSelectionColors = TextSelectionColors(
                    handleColor = MaterialTheme.colorScheme.tertiary,
                    backgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                )
                CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                    SelectionContainer {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            reverseLayout = true
                        ) {
                            if (isGenerating && streamingText.isNotBlank() && respondingCharacterId != null) {
                                item {
                                    // --- 👇 NUOVA LOGICA CHIAVE 👇 ---
                                    // Mostra solo la parte della narrazione, nascondendo i tag.
                                    val narrativeOnlyStream = streamingText.split("--- TAGS ---", limit = 2).getOrNull(0) ?: streamingText

                                    if (narrativeOnlyStream.isNotBlank()) {
                                        val streamingMessage = ChatMessage(
                                            position = -1L,
                                            authorId = respondingCharacterId,
                                            text = narrativeOnlyStream // <-- Usa il testo pulito
                                        )
                                        MessageBubble(
                                            message = streamingMessage,
                                            characters = characters,
                                            onTranslateClicked = {},
                                            onPlayClicked = {} // Disabilita il play per il testo in streaming
                                        )
                                    }
                                }
                            }
                            items(messages.reversed()) { message ->
                                MessageBubble(
                                    message = message,
                                    characters = characters,
                                    onTranslateClicked = { onTranslateMessage(message.id) },
                                    onPlayClicked = { onPlayMessage(message) })
                            }
                        }
                    }
                }
            }

            if (!isGenerating && (narrativeChoices.isNotEmpty() || disciplineChoices.isNotEmpty())) {
                Spacer(modifier = Modifier.height(4.dp))
                ChoicesContainer(
                    narrativeChoices = narrativeChoices,
                    disciplineChoices = disciplineChoices,
                    onNarrativeChoice = onNarrativeChoice,
                    onDisciplineChoice = onDisciplineChoice
                )
            }

            if (hero != null) {
                PlayerActionsBar(
                    hero = hero,
                    kaiRank = kaiRank,
                    isDiceRollEnabled = isDiceRollEnabled,
                    onDiceRollClicked = onDiceRollClicked
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isGenerating) {
                    GeneratingIndicator(
                        characterName = characters.find { it.id == respondingCharacterId }?.name
                            ?: "...", onStopClicked = onStopGeneration
                    )
                }
                if (isChatEnabled) {
                    MessageInput(onMessageSent = onMessageSent, isEnabled = !isGenerating)
                }
            }
        }
    }
}