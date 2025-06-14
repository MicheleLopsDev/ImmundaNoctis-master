package io.github.luposolitario.immundanoctis

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.data.ChatMessage
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.ModelPreferences
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import io.github.luposolitario.immundanoctis.view.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
                val chatMessages by viewModel.chatMessages.collectAsState()
                val streamingText by viewModel.streamingText.collectAsState()
                val isGenerating by viewModel.isGenerating.collectAsState()
                val conversationTargetId by viewModel.conversationTargetId.collectAsState()
                val respondingCharacterId by viewModel.respondingCharacterId.collectAsState()

                AdventureChatScreen(
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
                    }
                )
            }
        }
    }
}

@Composable
fun AdventureChatScreen(
    messages: List<ChatMessage>,
    streamingText: String,
    isGenerating: Boolean,
    selectedCharacterId: String,
    respondingCharacterId: String?,
    onMessageSent: (String) -> Unit,
    onCharacterSelected: (String) -> Unit,
    onStopGeneration: () -> Unit
) {
    var thinkingTime by remember { mutableStateOf(0L) }
    val listState = rememberLazyListState()

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
            listState.animateScrollToItem(0)
            while (isActive && isGenerating) {
                thinkingTime = System.currentTimeMillis() - startTime
                delay(100)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                                    characters = characters
                                )
                            }
                        }
                        items(messages.reversed()) { message ->
                            MessageBubble(message = message, characters = characters)
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
fun MessageBubble(message: ChatMessage, characters: List<GameCharacter>) {
    val author = characters.find { it.id == message.authorId }
    val isUserMessage = message.authorId == CharacterID.HERO
    val alignment = if (isUserMessage) Alignment.CenterEnd else Alignment.CenterStart

    val bubbleColor = if (isUserMessage) Color(0xFF005AC1) else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isUserMessage) Color.White else MaterialTheme.colorScheme.onSecondaryContainer

    val clipboardManager = LocalClipboardManager.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Column(horizontalAlignment = if (isUserMessage) Alignment.End else Alignment.Start) {
            // Riga con icona e nome autore
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
                            .size(64.dp) // Dimensione impostata dall'utente
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

            // --- CORREZIONE APPLICATA QUI ---
            // La bolla e il pulsante di copia sono ora in una Colonna,
            // così il pulsante andrà a capo naturalmente.
            // L'allineamento è gestito dalla Colonna esterna.

            // Card con il testo del messaggio
            Card(
                // Limita la larghezza massima per evitare che una parola molto lunga
                // occupi l'intero schermo, migliorando la leggibilità.
                modifier = Modifier.widthIn(max = screenWidth * 0.85f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = bubbleColor)
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(12.dp),
                    color = textColor
                )
            }

            // Pulsante Copia posizionato sotto la bolla
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(message.text))
                },
                modifier = Modifier.padding(top = 2.dp) // Aggiunge un piccolo spazio
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copia messaggio",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(44.dp) // Dimensione impostata dall'utente
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

// =======================================================================
// == Sezione Anteprime (Preview)
// =======================================================================

object PreviewData {
    val characters = listOf(
        GameCharacter(CharacterID.DM, "Master", "Dungeon Master", R.drawable.portrait_heroe),
        GameCharacter(CharacterID.HERO, "hero", "Eroe", R.drawable.portrait_mage),
        GameCharacter("player2", "Kael", "Mago", R.drawable.portrait_mage),
        GameCharacter("player3", "Elara", "Chierica", R.drawable.portrait_cleric)
    )

    val messages = listOf(
        ChatMessage(CharacterID.HERO, "Avanzo con cautela nella stanza, tenendo la bacchetta pronta."),
        ChatMessage(CharacterID.DM, "Mentre ti muovi, noti delle crepe sottili sul pavimento. Un'iscrizione quasi illeggibile recita 'Solo il penitente potrà passare'."),
        ChatMessage(CharacterID.HERO, "Mi inginocchio, chinando il capo in segno di umiltà."),
        ChatMessage(CharacterID.DM, "Le crepe si illuminano di una luce fioca e poi si spengono. Il passaggio sembra sicuro.")
    )
}

@Preview(name = "Schermata Chat - Normale", showBackground = true)
@Preview(name = "Schermata Chat - Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AdventureChatScreenPreview_Idle() {
    ImmundaNoctisTheme {
        AdventureChatScreen(
            messages = PreviewData.messages,
            streamingText = "",
            isGenerating = false,
            selectedCharacterId = CharacterID.HERO,
            respondingCharacterId = null,
            onMessageSent = {},
            onCharacterSelected = {},
            onStopGeneration = {}
        )
    }
}

@Preview(name = "Schermata Chat - Durante la Generazione", showBackground = true)
@Composable
fun AdventureChatScreenPreview_Generating() {
    ImmundaNoctisTheme {
        AdventureChatScreen(
            messages = PreviewData.messages,
            streamingText = "Senti un'eco lontana...",
            isGenerating = true,
            selectedCharacterId = CharacterID.DM,
            respondingCharacterId = CharacterID.DM,
            onMessageSent = {},
            onCharacterSelected = {},
            onStopGeneration = {}
        )
    }
}

@Preview(name = "Bolla Messaggio - Utente (hero)", showBackground = true, backgroundColor = 0xFFCCCCCC)
@Composable
fun MessageBubble_UserMessagePreview() {
    ImmundaNoctisTheme {
        MessageBubble(
            message = ChatMessage(CharacterID.HERO, "Estraggo la spada e mi preparo al peggio. Questo è un testo molto lungo per testare come il layout si comporta quando il contenuto della card occupa molto più spazio del previsto, spingendo gli altri elementi."),
            characters = PreviewData.characters
        )
    }
}

@Preview(name = "Bolla Messaggio - DM", showBackground = true, backgroundColor = 0xFFCCCCCC)
@Composable
fun MessageBubble_DmMessagePreview() {
    ImmundaNoctisTheme {
        MessageBubble(
            message = ChatMessage(CharacterID.DM, "Un'ombra si muove nell'angolo della stanza."),
            characters = PreviewData.characters
        )
    }
}

@Preview(name = "Indicatore di Generazione", showBackground = true)
@Composable
fun GeneratingIndicatorPreview() {
    ImmundaNoctisTheme {
        GeneratingIndicator(
            characterName = "Master",
            thinkingTime = 12345, // 12.3 secondi
            onStopClicked = {}
        )
    }
}

@Preview(name = "Input Messaggio - Errore Limite Caratteri", showBackground = true)
@Composable
fun MessageInput_ErrorLimitReachedPreview() {
    ImmundaNoctisTheme {
        // Simula lo stato di errore direttamente nel preview
        var textState by remember { mutableStateOf(TextFieldValue("a".repeat(1024))) }
        val maxChars = 1024
        val isError = textState.text.length >= maxChars

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = textState,
                onValueChange = { if (it.text.length <= maxChars) textState = it },
                placeholder = { Text("Cosa fai?") },
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
                isError = isError,
                trailingIcon = {
                    IconButton(onClick = {}, enabled = !isError) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_send),
                            contentDescription = "Invia messaggio"
                        )
                    }
                }
            )
            Text(
                text = "${textState.text.length} / $maxChars",
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 8.dp),
                textAlign = TextAlign.End,
                fontSize = 12.sp,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(name = "Header Avventura", showBackground = true)
@Composable
fun AdventureHeaderPreview() {
    ImmundaNoctisTheme {
        AdventureHeader(
            characters = PreviewData.characters,
            selectedCharacterId = CharacterID.HERO,
            onCharacterClick = {}
        )
    }
}
