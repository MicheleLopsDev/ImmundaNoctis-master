package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.ModelPreferences
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import io.github.luposolitario.immundanoctis.view.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdventureActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val modelPreferences by lazy { ModelPreferences(applicationContext) }
    private val themePreferences by lazy { ThemePreferences(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                // Logica per colorare la barra di stato di nero
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        window.statusBarColor = Color.Black.toArgb()
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
                    }
                }

                val chatMessages by viewModel.chatMessages.collectAsState()
                val streamingText by viewModel.streamingText.collectAsState()
                val isGenerating by viewModel.isGenerating.collectAsState()
                val conversationTargetId by viewModel.conversationTargetId.collectAsState()
                val respondingCharacterId by viewModel.respondingCharacterId.collectAsState()

                AdventureChatScreen(
                    viewModel = viewModel, // Passiamo il viewModel per la logica di salvataggio
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
                    onSaveChat = { characters ->
                        viewModel.onSaveChatClicked(characters)
                    },
                    onTranslateMessage = { messageId ->
                        viewModel.translateMessage(messageId)
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdventureChatScreen(
    viewModel: MainViewModel,
    messages: List<ChatMessage>,
    streamingText: String,
    isGenerating: Boolean,
    selectedCharacterId: String,
    respondingCharacterId: String?,
    onMessageSent: (String) -> Unit,
    onCharacterSelected: (String) -> Unit,
    onStopGeneration: () -> Unit,
    onSaveChat: (List<GameCharacter>) -> Unit,
    onTranslateMessage: (String) -> Unit
) {
    var thinkingTime by remember { mutableStateOf(0L) }
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // --- NUOVA LOGICA DI SALVATAGGIO ---
    var jsonToSave by remember { mutableStateOf<String?>(null) }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri: Uri? ->
            if (uri != null && jsonToSave != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonToSave!!.toByteArray())
                    }
                    Toast.makeText(context, "Chat salvata!", Toast.LENGTH_SHORT).show()
                } catch (e: IOException) {
                    Toast.makeText(context, "Errore durante il salvataggio.", Toast.LENGTH_SHORT).show()
                } finally {
                    jsonToSave = null
                }
            } else if (uri == null) {
                Toast.makeText(context, "Salvataggio annullato.", Toast.LENGTH_SHORT).show()
                jsonToSave = null
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.saveChatEvent.collectLatest { jsonContent ->
            jsonToSave = jsonContent
            val timeStamp = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()).format(Date())
            val fileName = "chat_$timeStamp.json"
            saveFileLauncher.launch(fileName)
        }
    }
    // --- FINE NUOVA LOGICA DI SALVATAGGIO ---

    val characters = listOf(
        GameCharacter(CharacterID.DM, "Master", "Dungeon Master", R.drawable.portrait_dm),
        GameCharacter(CharacterID.HERO, "hero", "Eroe", R.drawable.portrait_heroe),
        GameCharacter("player2", "Kael", "Mago", R.drawable.portrait_mage),
        GameCharacter("player3", "Elara", "Chierica", R.drawable.portrait_cleric),
        GameCharacter("player4", "Grunda", "Barbara", R.drawable.portrait_barbarian)
    )

    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            val startTime = System.currentTimeMillis()
            if (listState.firstVisibleItemIndex > 1) {
                listState.animateScrollToItem(0)
            }
            while (isActive && isGenerating) {
                thinkingTime = System.currentTimeMillis() - startTime
                delay(100)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Immunda Noctis") },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opzioni")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Salva Chat") },
                                onClick = {
                                    onSaveChat(characters)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Save, contentDescription = "Salva")
                                }
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
                                    MessageBubble(
                                        message = ChatMessage(respondingCharacterId, streamingText),
                                        characters = characters,
                                        onTranslateClicked = {} // Non serve tradurre un messaggio in streaming
                                    )
                                }
                            }
                            items(messages.reversed()) { message ->
                                MessageBubble(
                                    message = message,
                                    characters = characters,
                                    onTranslateClicked = { onTranslateMessage(message.id) }
                                )
                            }
                        }
                    }
                }
            }

            if (isGenerating) {
                GeneratingIndicator(
                    characterName = characters.find { it.id == respondingCharacterId }?.name ?: "...",
                    thinkingTime = thinkingTime,
                    onStopClicked = onStopGeneration
                )
            }
            MessageInput(onMessageSent = onMessageSent, isEnabled = !isGenerating)
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    characters: List<GameCharacter>,
    onTranslateClicked: () -> Unit
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
                    Text(
                        text = message.text,
                        color = textColor
                    )
                    if (message.translatedText != null) {
                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = textColor.copy(alpha = 0.5f)
                        )
                        Text(
                            text = message.translatedText,
                            color = textColor,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(message.text))
                    }
                ) {
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
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
            }
        }
    }
}
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
