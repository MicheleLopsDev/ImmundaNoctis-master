package io.github.luposolitario.immundanoctis

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.luposolitario.immundanoctis.data.ChatMessage
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.ModelPreferences
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import io.github.luposolitario.immundanoctis.view.CharacterID
import io.github.luposolitario.immundanoctis.view.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class AdventureActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val modelPreferences by lazy { ModelPreferences(applicationContext) }
    private val themePreferences by lazy { ThemePreferences(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val savedModel = modelPreferences.getLastModel()
        if (savedModel != null) {
            viewModel.log("Caricamento modello: ${savedModel.name}")
            viewModel.load(savedModel.destination.path)
        } else {
            viewModel.log("Nessun modello trovato. Vai in ModelActivity per scaricarne uno.")
        }

        setContent {
            val useDarkTheme = themePreferences.useDarkTheme(isSystemInDarkTheme())
            ImmundaNoctisTheme(darkTheme = useDarkTheme) {
                val chatMessages by viewModel.chatMessages.collectAsState()
                val streamingText by viewModel.streamingText.collectAsState()
                val isGenerating by viewModel.isGenerating.collectAsState()

                AdventureChatScreen(
                    messages = chatMessages,
                    streamingText = streamingText,
                    isGenerating = isGenerating,
                    onMessageSent = { messageText ->
                        viewModel.sendMessage(messageText)
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
    onMessageSent: (String) -> Unit
) {
    var thinkingTime by remember { mutableStateOf(0L) }
    val listState = rememberLazyListState()
    val lastSpeakerId = messages.lastOrNull()?.authorId

    val characters = listOf(
        GameCharacter(CharacterID.DM, "Master", "Dungeon Master", R.drawable.portrait_dm),
        GameCharacter("player1", "Elara", "Maga", R.drawable.portrait_mage),
        GameCharacter("player2", "Kael", "Chierico", R.drawable.portrait_cleric),
        GameCharacter("player3", "Grog", "Barbaro", R.drawable.portrait_barbarian)
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
        AdventureHeader(characters = characters, speakingCharacterId = lastSpeakerId)
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                reverseLayout = true
            ) {
                if (isGenerating && streamingText.isNotBlank()) {
                    item {
                        MessageBubble(message = ChatMessage(CharacterID.DM, streamingText))
                    }
                }
                items(messages.reversed()) { message ->
                    MessageBubble(message = message)
                }
            }
        }

        if (isGenerating) {
            Text(
                text = "Il Master sta pensando... (${String.format("%.1f", thinkingTime / 1000.0)}s)",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        MessageInput(onMessageSent = onMessageSent, isEnabled = !isGenerating)
    }
}

@Composable
fun AdventureHeader(characters: List<GameCharacter>, speakingCharacterId: String?) {
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
                CharacterPortrait(
                    character = character,
                    isSpeaking = character.id == speakingCharacterId,
                    size = if (character.id == CharacterID.DM) 72.dp else 60.dp
                )
            }
        }
    }
}

@Composable
fun CharacterPortrait(
    character: GameCharacter,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val borderColor = if (isSpeaking) MaterialTheme.colorScheme.primary else Color.Transparent
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
fun MessageBubble(message: ChatMessage) {
    val isHero = message.authorId == CharacterID.HERO
    val alignment = if (isHero) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isHero) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isHero) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = textColor
            )
        }
    }
}

@Composable
fun MessageInput(onMessageSent: (String) -> Unit, isEnabled: Boolean) {
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    OutlinedTextField(
        value = textState,
        onValueChange = { textState = it },
        placeholder = { Text("Cosa fai?") },
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        trailingIcon = {
            IconButton(
                onClick = {
                    if (textState.text.isNotBlank()) {
                        onMessageSent(textState.text)
                        textState = TextFieldValue("")
                    }
                },
                enabled = isEnabled
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_send),
                    contentDescription = "Invia messaggio",
                    tint = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    )
}

@Preview(showBackground = true, name = "Chat - DM Parla (Light)")
@Composable
fun AdventureChatScreenPreview() {
    val previewMessages = listOf(
        ChatMessage(CharacterID.HERO, "Accendo una torcia e avanzo con cautela."),
        ChatMessage(CharacterID.DM, "La luce rivela antiche incisioni...")
    )
    ImmundaNoctisTheme {
        Surface {
            AdventureChatScreen(
                messages = previewMessages,
                streamingText = "",
                isGenerating = false,
                onMessageSent = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Chat - DM Parla (Dark)", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AdventureChatScreenPreview_Dark() {
    val previewMessages = listOf(
        ChatMessage(CharacterID.HERO, "Accendo una torcia e avanzo con cautela."),
        ChatMessage(CharacterID.DM, "La luce rivela antiche incisioni...")
    )
    ImmundaNoctisTheme {
        Surface {
            AdventureChatScreen(
                messages = previewMessages,
                streamingText = "",
                isGenerating = false,
                onMessageSent = {}
            )
        }
    }
}
