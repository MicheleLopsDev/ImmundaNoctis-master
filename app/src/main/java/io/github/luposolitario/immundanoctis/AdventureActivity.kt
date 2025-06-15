package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.data.ChatMessage
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.service.TtsService
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.ModelPreferences
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import io.github.luposolitario.immundanoctis.util.TtsPreferences
import io.github.luposolitario.immundanoctis.view.MainViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

class AdventureActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val modelPreferences by lazy { ModelPreferences(applicationContext) }
    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    private val ttsPreferences by lazy { TtsPreferences(applicationContext) }
    private var ttsService: TtsService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadGameSession()

        // Inizializza il TtsService
        ttsService = TtsService(this) {
            // Questo blocco viene eseguito quando il TTS è pronto.
            // Potremmo voler fare qualcosa qui, ma per ora non è necessario.
        }

        val dmModel = modelPreferences.getDmModel()
        val playerModel = modelPreferences.getPlayerModel()

        if (dmModel == null && playerModel == null) {
            viewModel.log("Nessun modello configurato. Vai in ModelActivity per scaricarli.")
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
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
                    }
                }

                val characters by viewModel.gameCharacters.collectAsState()
                val chatMessages by viewModel.chatMessages.collectAsState()
                val streamingText by viewModel.streamingText.collectAsState()
                val isGenerating by viewModel.isGenerating.collectAsState()
                val conversationTargetId by viewModel.conversationTargetId.collectAsState()
                val respondingCharacterId by viewModel.respondingCharacterId.collectAsState()
                val isAutoReadEnabled = ttsPreferences.isAutoReadEnabled()

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

                // Gestisce il ciclo di vita del TtsService
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
                        // Nuova callback per il TTS
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
        // Assicurati di rilasciare le risorse del TTS
        ttsService?.shutdown()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdventureChatScreen(
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
    onPlayMessage: (ChatMessage) -> Unit // Nuova callback
) {
    var thinkingTime by remember { mutableStateOf(0L) }
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }
    val hero = characters.find { it.id == CharacterID.HERO }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(hero?.name ?: "Immunda Noctis") },
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
            AdventureHeader(characters = characters, selectedCharacterId = selectedCharacterId, onCharacterClick = onCharacterSelected)
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
                                    MessageBubble(message = streamingMessage, characters = characters, onTranslateClicked = {}, onPlayClicked = { onPlayMessage(streamingMessage) })
                                }
                            }
                            items(messages.reversed()) { message ->
                                MessageBubble(
                                    message = message,
                                    characters = characters,
                                    onTranslateClicked = { onTranslateMessage(message.id) },
                                    onPlayClicked = { onPlayMessage(message) } // Passa la callback
                                )
                            }
                        }
                    }
                }
            }

            if (isGenerating) {
                GeneratingIndicator(characterName = characters.find { it.id == respondingCharacterId }?.name ?: "...", thinkingTime = thinkingTime, onStopClicked = onStopGeneration)
            }
            MessageInput(onMessageSent = onMessageSent, isEnabled = !isGenerating)
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    characters: List<GameCharacter>,
    onTranslateClicked: () -> Unit,
    onPlayClicked: () -> Unit // Nuova callback
) {
    val author = characters.find { it.id == message.authorId }
    val isUserMessage = message.authorId == CharacterID.HERO
    val alignment = if (isUserMessage) Alignment.End else Alignment.Start
    val bubbleColor = if (isUserMessage) Color(0xFF005AC1) else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isUserMessage) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
    val clipboardManager = LocalClipboardManager.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isUserMessage) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(horizontalAlignment = if (isUserMessage) Alignment.End else Alignment.Start) {
            if (author != null) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = author.portraitResId),
                        contentDescription = "Ritratto di ${author.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = author.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            Card(
                modifier = Modifier.widthIn(max = screenWidth * 0.85f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = bubbleColor)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(text = message.text, color = textColor)
                    if (message.translatedText != null) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = textColor.copy(alpha = 0.5f))
                        Text(text = message.translatedText, color = textColor, fontStyle = FontStyle.Italic)
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                IconButton(onClick = { clipboardManager.setText(AnnotatedString(message.text)) }) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copia messaggio",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(44.dp)
                    )
                }

                if (!isUserMessage) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(contentAlignment = Alignment.Center) {
                        if (message.isTranslating) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = onTranslateClicked) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "Traduci messaggio",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }
                }

                // --- NUOVO PULSANTE PLAY ---
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onPlayClicked) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleOutline,
                        contentDescription = "Leggi messaggio",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
    }
}

// Il resto del file AdventureActivity.kt rimane invariato...
@Composable
fun GeneratingIndicator(
    characterName: String,
    thinkingTime: Long,
    onStopClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Il motore di $characterName sta pensando... (${String.format("%.1f", thinkingTime / 1000.0)}s)",
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f, fill = false)
        )
        Button(
            onClick = onStopClicked,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Ferma")
        }
    }
}

@Composable
fun CharacterPortrait(
    character: GameCharacter,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = character.portraitResId),
            contentDescription = "Ritratto di ${character.name}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(3.dp, borderColor, CircleShape)
        )
        Text(
            text = character.name,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun AdventureHeader(
    characters: List<GameCharacter>,
    selectedCharacterId: String,
    onCharacterClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.map_dungeon),
            contentDescription = "Mappa del Dungeon",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(top = 80.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            characters.forEach { character ->
                val isClickable = character.id != CharacterID.DM

                val portraitModifier = if (isClickable) {
                    Modifier.clickable { onCharacterClick(character.id) }
                } else {
                    Modifier
                }

                CharacterPortrait(
                    character = character,
                    isSelected = character.id == selectedCharacterId,
                    modifier = portraitModifier,
                    size = if (character.id == CharacterID.DM) 72.dp else 60.dp
                )
            }
        }
    }
}

@Composable
fun MessageInput(onMessageSent: (String) -> Unit, isEnabled: Boolean) {
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    val maxChars = 1024
    val isError = textState.text.length >= maxChars

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = textState,
            onValueChange = {
                if (it.text.length <= maxChars) {
                    textState = it
                }
            },
            placeholder = { Text("Cosa fai?") },
            enabled = isEnabled,
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (textState.text.isNotBlank()) {
                            onMessageSent(textState.text)
                            textState = TextFieldValue("")
                        }
                    },
                    enabled = isEnabled && textState.text.isNotBlank() && !isError
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_send),
                        contentDescription = "Invia messaggio",
                        tint = if (isEnabled && textState.text.isNotBlank() && !isError) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        )
        Text(
            text = "${textState.text.length} / $maxChars",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, end = 8.dp),
            textAlign = TextAlign.End,
            fontSize = 12.sp,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
