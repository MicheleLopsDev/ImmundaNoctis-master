package io.github.luposolitario.immundanoctis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctis.data.ChatMessage
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.ModelPreferences
import io.github.luposolitario.immundanoctis.view.CharacterID
import io.github.luposolitario.immundanoctis.view.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class AdventureActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val modelPreferences by lazy { ModelPreferences(applicationContext) }

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
            ImmundaNoctisTheme {
                // Questa riga ora funziona perché viewModel.chatMessages esiste.
                val chatMessages by viewModel.chatMessages.collectAsState()
                val streamingText by viewModel.streamingText.collectAsState()
                val isGenerating by viewModel.isGenerating.collectAsState()

                AdventureChatScreen(
                    // E qui passiamo la variabile corretta.
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

// ... Il resto di AdventureActivity.kt rimane invariato ...
@Composable
fun AdventureChatScreen(
    messages: List<ChatMessage>,
    streamingText: String,
    isGenerating: Boolean,
    onMessageSent: (String) -> Unit
) {
    var thinkingTime by remember { mutableStateOf(0L) }
    val listState = rememberLazyListState()

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
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            reverseLayout = true
        ) {
            if (isGenerating && streamingText.isNotBlank()) {
                item {
                    MessageBubble(
                        message = ChatMessage(CharacterID.DM, streamingText)
                    )
                }
            }
            items(messages.reversed()) { message ->
                MessageBubble(message = message)
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

@Preview(showBackground = true, name = "Chat - Stato Normale")
@Composable
fun AdventureChatScreenPreview_Idle() {
    val previewMessages = listOf(
        ChatMessage(
            authorId = CharacterID.DM,
            text = "Una caverna buia si apre davanti a te."
        ),
        ChatMessage(
            authorId = CharacterID.HERO,
            text = "Accendo una torcia."
        )
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

@Preview(showBackground = true, name = "Chat - In Generazione")
@Composable
fun AdventureChatScreenPreview_Generating() {
    val previewMessages = listOf(
        ChatMessage(
            authorId = CharacterID.DM,
            text = "Una caverna buia si apre davanti a te."
        ),
        ChatMessage(
            authorId = CharacterID.HERO,
            text = "Accendo una torcia."
        )
    )
    ImmundaNoctisTheme {
        Surface {
            AdventureChatScreen(
                messages = previewMessages,
                streamingText = "La luce rivela antiche incisioni...",
                isGenerating = true,
                onMessageSent = {}
            )
        }
    }
}
