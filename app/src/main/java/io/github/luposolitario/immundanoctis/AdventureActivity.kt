package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.data.CharacterType
import io.github.luposolitario.immundanoctis.data.ChatMessage
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.PlaceholderSkills
import io.github.luposolitario.immundanoctis.data.Skill
import io.github.luposolitario.immundanoctis.service.TtsService
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
                                    MessageBubble(message = streamingMessage, characters = characters, onTranslateClicked = {}, onPlayClicked = { onPlayMessage(streamingMessage) })
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
                    characterName = characters.find { it.id == respondingCharacterId }?.name ?: "...",
                    onStopClicked = onStopGeneration
                )
            }
            MessageInput(onMessageSent = onMessageSent, isEnabled = !isGenerating)
        }
    }
}

@Composable
fun AdventureHeader(
    characters: List<GameCharacter>,
    selectedCharacterId: String,
    onCharacterClick: (String) -> Unit
) {
    val characterOrder = mapOf("dm" to 0, "hero" to 1, "companion1" to 2, "companion2" to 3)
    val sortedCharacters = characters.sortedWith(compareBy { characterOrder[it.id] ?: Int.MAX_VALUE })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.map_dungeon),
            contentDescription = "Mappa del Dungeon",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(top = 80.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            sortedCharacters.forEach { character ->
                if (character.type == CharacterType.PLAYER) {
                    // Non mostrare l'eroe nell'header, come da soluzione del bug
                } else if (character.isVisible) {
                    CharacterPortrait(
                        character = character,
                        isSelected = character.id == selectedCharacterId,
                        modifier = Modifier.clickable { onCharacterClick(character.id) },
                        size = if (character.type == CharacterType.DM) 72.dp else 60.dp
                    )
                } else {
                    PlaceholderPortrait(size = 60.dp)
                }
            }
        }
    }
}

@Composable
fun PlayerActionsBar(hero: GameCharacter) {
    var showStrengthDialog by remember { mutableStateOf(false) }
    var showCunningDialog by remember { mutableStateOf(false) }
    var showKnowledgeDialog by remember { mutableStateOf(false) }
    var showSpellDialog by remember { mutableStateOf(false) }
    var showDiceDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showStrengthDialog) {
        SkillDialog(title = "Abilità di Forza", skills = PlaceholderSkills.strengthSkills, onDismiss = { showStrengthDialog = false })
    }
    if (showCunningDialog) {
        SkillDialog(title = "Abilità di Astuzia", skills = PlaceholderSkills.cunningSkills, onDismiss = { showCunningDialog = false })
    }
    if (showKnowledgeDialog) {
        SkillDialog(title = "Abilità di Sapere", skills = PlaceholderSkills.knowledgeSkills, onDismiss = { showKnowledgeDialog = false })
    }
    if (showSpellDialog) {
        SkillDialog(title = "Incantesimi", skills = PlaceholderSkills.spellSkills, onDismiss = { showSpellDialog = false })
    }
    if (showDiceDialog) {
        DiceDialog(onDismiss = { showDiceDialog = false })
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CharacterPortrait(
                character = hero,
                isSelected = false,
                size = 56.dp,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(context, CharacterSheetActivity::class.java))
                }
            )
        }
        ActionIcon(icon = Icons.Default.FitnessCenter, label = "Forza", onClick = { showStrengthDialog = true })
        ActionIcon(icon = Icons.Default.Lightbulb, label = "Astuzia", onClick = { showCunningDialog = true })
        ActionIcon(icon = Icons.Default.MenuBook, label = "Sapere", onClick = { showKnowledgeDialog = true })
        ActionIcon(icon = Icons.Default.AutoFixHigh, label = "Magia", onClick = { showSpellDialog = true })
        ActionIcon(icon = Icons.Default.Casino, label = "Dadi", onClick = { showDiceDialog = true })
    }
}

@Composable
fun ActionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, Modifier.size(32.dp))
        }
        Text(text = label, fontSize = 10.sp)
    }
}

@Composable
fun SkillDialog(title: String, skills: List<Skill>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(skills) { skill ->
                        SkillCard(skill = skill)
                    }
                }
            }
        }
    }
}

@Composable
fun SkillCard(skill: Skill) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = skill.icon, contentDescription = null, modifier = Modifier.size(40.dp).padding(end = 8.dp), tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(text = skill.name, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                RatingStars(currentLevel = skill.level, maxLevel = skill.maxLevel)
                Spacer(modifier = Modifier.height(8.dp))
                skill.effects.forEachIndexed { index, effect ->
                    val levelRequired = index + 1
                    val unlocked = skill.level >= levelRequired
                    Text(
                        text = "Liv $levelRequired: $effect",
                        fontSize = 14.sp,
                        color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun RatingStars(currentLevel: Int, maxLevel: Int) {
    Row {
        for (i in 1..maxLevel) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Livello $i",
                tint = if (i <= currentLevel) Color(0xFFFFD700) else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun DiceDialog(onDismiss: () -> Unit) {
    var diceCount by remember { mutableStateOf("2") }
    var modifier by remember { mutableStateOf("0") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Lancia i Dadi", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Icon(Icons.Default.Casino, contentDescription = "Dado", modifier = Modifier.size(64.dp))
                    Icon(Icons.Default.Casino, contentDescription = "Dado", modifier = Modifier.size(64.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = diceCount, onValueChange = { diceCount = it }, label = { Text("N. Dadi") }, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(value = modifier, onValueChange = { modifier = it }, label = { Text("Modificatore") }, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { /* Logica da aggiungere */ }, modifier = Modifier.fillMaxWidth()) {
                    Text("TIRA")
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    characters: List<GameCharacter>,
    onTranslateClicked: () -> Unit,
    onPlayClicked: () -> Unit
) {
    val author = characters.find { it.id == message.authorId }
    val isUserMessage = message.authorId == CharacterID.HERO
    val bubbleColor = if (isUserMessage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isUserMessage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    val clipboardManager = LocalClipboardManager.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isUserMessage) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (!isUserMessage && author != null) {
                Image(
                    painter = painterResource(id = author.portraitResId),
                    contentDescription = author.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(horizontalAlignment = if (isUserMessage) Alignment.End else Alignment.Start) {
                if (author != null && !isUserMessage) {
                    Text(
                        text = author.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
                    )
                }
                Card(
                    modifier = Modifier.widthIn(max = screenWidth * 0.8f),
                    shape = RoundedCornerShape(16.dp),
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
                Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(message.text)) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.ContentCopy, "Copia", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    if (!isUserMessage) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
                            if (message.isTranslating) CircularProgressIndicator(strokeWidth = 2.dp)
                            else IconButton(onClick = onTranslateClicked, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Translate, "Traduci", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                    IconButton(onClick = onPlayClicked, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.PlayCircleOutline, "Leggi", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
            if (isUserMessage && author != null) {
                Spacer(Modifier.width(8.dp))
                Image(
                    painter = painterResource(id = author.portraitResId),
                    contentDescription = author.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}

@Composable
fun GeneratingIndicator(onStopClicked: () -> Unit, characterName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$characterName sta pensando...",
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f, fill = false)
        )
        Button(
            onClick = onStopClicked,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
fun PlaceholderPortrait(modifier: Modifier = Modifier, size: Dp = 64.dp) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.8f))
                .border(3.dp, Color.DarkGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QuestionMark,
                contentDescription = "Personaggio Sconosciuto",
                tint = Color.Gray,
                modifier = Modifier.size(size * 0.6f)
            )
        }
        Text(
            text = "Sconosciuto",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp)
        )
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
