package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Save
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
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.data.CharacterType
import io.github.luposolitario.immundanoctis.data.ChatMessage
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.Genre
import io.github.luposolitario.immundanoctis.data.Scene
import io.github.luposolitario.immundanoctis.data.SessionData
import io.github.luposolitario.immundanoctis.engine.TokenInfo
import io.github.luposolitario.immundanoctis.service.TtsService
import io.github.luposolitario.immundanoctis.ui.adventure.AdventureHeader
import io.github.luposolitario.immundanoctis.ui.adventure.GeneratingIndicator
import io.github.luposolitario.immundanoctis.ui.adventure.MessageBubble
import io.github.luposolitario.immundanoctis.ui.adventure.MessageInput
import io.github.luposolitario.immundanoctis.ui.adventure.PlayerActionsBar
import io.github.luposolitario.immundanoctis.ui.adventure.TokenSemaphoreIndicator
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.GameStateManager
import io.github.luposolitario.immundanoctis.util.ModelPreferences
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import io.github.luposolitario.immundanoctis.util.TtsPreferences
import io.github.luposolitario.immundanoctis.view.MainViewModel
import io.github.luposolitario.immundanoctis.util.SavePreferences // <-- 1. Aggiungi questo import
import io.github.luposolitario.immundanoctis.view.MainViewModel.EngineLoadingState
import kotlinx.coroutines.launch
import io.github.luposolitario.immundanoctis.engine.GameLogicManager
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.lifecycleScope // <-- Importa lifecycleScope
import kotlin.lazy

class AdventureActivity : ComponentActivity() {
    private val tag: String? = this::class.simpleName
    private val viewModel: MainViewModel by viewModels()
    private val modelPreferences by lazy { ModelPreferences(applicationContext) }
    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    private val ttsPreferences by lazy { TtsPreferences(applicationContext) }
    private var ttsService: TtsService? = null
    private val savePreferences by lazy { SavePreferences(applicationContext) }
    private lateinit var gameStateManager: GameStateManager
    private lateinit var gameLogicManager: GameLogicManager
    private val currentScene = MutableStateFlow<Scene?>(null)


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

                // Raccogliamo il nuovo stato di caricamento
                val loadingState by viewModel.engineLoadingState.collectAsState()

                // Usiamo un 'when' per decidere cosa mostrare
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
                                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
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
                        val isAutoSaveEnabled = savePreferences.isAutoSaveEnabled // Leggiamo il valore
                        val tokenInfo by viewModel.activeTokenInfo.collectAsState()

                        // Effetto per la lettura automatica dei nuovi messaggi
                        LaunchedEffect(chatMessages) {
                            if (isAutoReadEnabled) {
                                chatMessages.lastOrNull()?.let { lastMessage ->
                                    val author = characters.find { it.id == lastMessage.authorId }
                                    // Leggi solo se il messaggio non è dell'eroe e non è un messaggio vuoto
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
                                isAutoSaveEnabled = isAutoSaveEnabled, // Passiamo il valore alla UI
                                sessionName = sessionName,
                                characters = characters,
                                messages = chatMessages,
                                streamingText = streamingText,
                                isGenerating = isGenerating,
                                selectedCharacterId = conversationTargetId,
                                respondingCharacterId = respondingCharacterId,
                                tokenInfo = tokenInfo, // <-- NUOVO PARAMETRO
                                onMessageSent = { messageText ->
                                    viewModel.sendMessage(messageText,conversationTargetId)
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
                                // --- 👇 AGGIUNGI QUESTO NUOVO PARAMETRO 👇 ---
                                onResetSession = { viewModel.resetSession() }
                            )
                        }
                    }
                    is EngineLoadingState.Error -> {
                        val errorMessage = (loadingState as EngineLoadingState.Error).message
                        ErrorScreen(
                            errorMessage = errorMessage ?: "Errore sconosciuto",
                            onRetry = {
                                // Rilancia il caricamento dei modelli
                                val dmModel = modelPreferences.getDmModel()
                                val playerModel = modelPreferences.getPlayerModel()
                                viewModel.loadEngines(
                                    dmModelPath = dmModel?.destination?.path,
                                    playerModelPath = playerModel?.destination?.path
                                )
                            }
                        )
                    }
                }
            }
        }

        // NUOVO: Gestione dell'inizio di una nuova avventura o ripresa di una esistente
        if (!session.isStarted) {
            currentScene.value = gameLogicManager.selectRandomStartScene(Genre.WESTERN) // Genere hardcoded per ora
            Log.d(tag, "Scena iniziale NUOVA AVVENTURA impostata da GameLogicManager: ${currentScene.value?.id ?: "Nessuna scena iniziale"}")

            lifecycleScope.launch {
                viewModel.sendInitialDmPrompt(session,currentScene.value) // Passa la sessione per aggiornare isStarted
            }
            gameStateManager.saveSession(
                SessionData(
                    sessionName = "La Prova dell'Eroe",
                    lastUpdate = System.currentTimeMillis(),
                    characters = session.characters,
                    isStarted = true // NUOVO: La sessione non è ancora "iniziata"
                )
            )
            // gameLogicManager.resetUsedScenes() // Già resettato in sendInitialDmPrompt se si avvia
        } else {
            // Se è una sessione esistente E NON è un nuovo inizio forzato, prova a caricare l'ultima scena salvata
            val lastSceneId = session.usedScenes.lastOrNull()
            currentScene.value = if (lastSceneId != null) {
                gameLogicManager.getSceneById(lastSceneId)
            } else {
                gameLogicManager.selectRandomStartScene(Genre.WESTERN) // Fallback a START casuale
            }
            Log.d(tag, "Scena sessione esistente impostata a: ${currentScene.value?.id ?: "Nessuna scena valida trovata. Riprovo con casuale START."}")
            // Non inviamo prompt iniziale qui, si suppone che la storia sia già avviata
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsService?.shutdown()
    }
}

// In AdventureActivity.kt

@Composable
fun LoadingScreen(text: String = "Caricamento motori AI...") {
    Box(
        // 👇 MODIFICA SOLO QUESTA RIGA 👇
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
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

// In AdventureActivity.kt

@Composable
fun ErrorScreen(errorMessage: String, onRetry: () -> Unit) {
    Box(
        // 👇 MODIFICA SOLO QUESTA RIGA 👇
        modifier = Modifier.fillMaxSize().padding(16.dp).background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // ... il resto del codice rimane invariato
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



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdventureChatScreen(
    isAutoSaveEnabled: Boolean, // <-- 3. Aggiungi il nuovo parametro
    sessionName: String,
    characters: List<GameCharacter>,
    messages: List<ChatMessage>,
    streamingText: String,
    isGenerating: Boolean,
    selectedCharacterId: String,
    respondingCharacterId: String?,
    tokenInfo: TokenInfo, // <-- 2. Aggiungi il nuovo parametro qui
    onMessageSent: (String) -> Unit,
    onCharacterSelected: (String) -> Unit,
    onStopGeneration: () -> Unit,
    onSaveChat: () -> Unit,
    onTranslateMessage: (String) -> Unit,
    onPlayMessage: (ChatMessage) -> Unit,
    // --- 👇 AGGIUNGI QUESTO NUOVO PARAMETRO ALLA FIRMA 👇 ---
    onResetSession: () -> Unit
) {
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }
    val hero = characters.find { it.type == CharacterType.PLAYER }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    // --- AGGIUNGI QUESTO BLOCCO PER LO SCROLL AUTOMATICO ---
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // --- 👇 MODIFICA QUI: Sostituisci il Text con una Row 👇 ---
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TokenSemaphoreIndicator(tokenInfo = tokenInfo, onResetSession = onResetSession )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(sessionName)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opzioni")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (!isAutoSaveEnabled) {
                                DropdownMenuItem(
                                    text = { Text("Salva Chat Manualmente") },
                                    onClick = {
                                        onSaveChat()
                                        showMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Save, contentDescription = "Salva") }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AdventureHeader(
                characters = characters,
                selectedCharacterId = selectedCharacterId,
                onCharacterClick = onCharacterSelected
            )
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                val customTextSelectionColors = TextSelectionColors(handleColor = MaterialTheme.colorScheme.tertiary, backgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
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
                                    val streamingMessage = ChatMessage(position = -1L,authorId=respondingCharacterId, text=streamingText)
                                    MessageBubble(
                                        message = streamingMessage,
                                        characters = characters,
                                        onTranslateClicked = {},
                                        onPlayClicked = { onPlayMessage(streamingMessage) })
                                }
                            }
                            items(messages.reversed()) { message ->
                                MessageBubble(
                                    message = message,
                                    characters = characters,
                                    onTranslateClicked = { onTranslateMessage(message.id) },
                                    onPlayClicked = { onPlayMessage(message) }
                                )
                            }
                        }
                    }
                }
            }
            if (hero != null) {
                PlayerActionsBar(hero = hero)
            }
            if (isGenerating) {
                GeneratingIndicator(
                    characterName = characters.find { it.id == respondingCharacterId }?.name
                        ?: "...",
                    onStopClicked = onStopGeneration
                )
            }
            MessageInput(onMessageSent = onMessageSent, isEnabled = !isGenerating)
        }
    }
}

