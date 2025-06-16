package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Save
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
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.github.luposolitario.immundanoctis.data.CharacterType
import io.github.luposolitario.immundanoctis.data.ChatMessage
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.service.TtsService
import io.github.luposolitario.immundanoctis.ui.adventure.AdventureHeader
import io.github.luposolitario.immundanoctis.ui.adventure.GeneratingIndicator
import io.github.luposolitario.immundanoctis.ui.adventure.MessageBubble
import io.github.luposolitario.immundanoctis.ui.adventure.MessageInput
import io.github.luposolitario.immundanoctis.ui.adventure.PlayerActionsBar
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.ModelPreferences
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import io.github.luposolitario.immundanoctis.util.TtsPreferences
import io.github.luposolitario.immundanoctis.view.MainViewModel

class AdventureActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val modelPreferences by lazy { ModelPreferences(applicationContext) }
    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    private val ttsPreferences by lazy { TtsPreferences(applicationContext) }
    private var ttsService: TtsService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
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
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        window.statusBarColor = Color.Black.toArgb()
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
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

                DisposableEffect(Unit) {
                    onDispose {
                        ttsService?.shutdown()
                    }
                }

                if (characters.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    AdventureChatScreen(
                        sessionName = sessionName,
                        characters = characters,
                        messages = chatMessages,
                        streamingText = streamingText,
                        isGenerating = isGenerating,
                        selectedCharacterId = conversationTargetId,
                        respondingCharacterId = respondingCharacterId,
                        onMessageSent = { messageText ->
                            viewModel.sendMessage(messageText)
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
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsService?.shutdown()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdventureChatScreen(
    sessionName: String,
    characters: List<GameCharacter>,
    messages: List<ChatMessage>,
    streamingText: String,
    isGenerating: Boolean,
    selectedCharacterId: String,
    respondingCharacterId: String?,
    onMessageSent: (String) -> Unit,
    onCharacterSelected: (String) -> Unit,
    onStopGeneration: () -> Unit,
    onSaveChat: () -> Unit,
    onTranslateMessage: (String) -> Unit,
    onPlayMessage: (ChatMessage) -> Unit
) {
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }
    val hero = characters.find { it.type == CharacterType.PLAYER }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sessionName) },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opzioni")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Salva Chat") },
                                onClick = {
                                    onSaveChat()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Outlined.Save, contentDescription = "Salva") }
                            )
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
                                    val streamingMessage = ChatMessage(respondingCharacterId, streamingText)
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

