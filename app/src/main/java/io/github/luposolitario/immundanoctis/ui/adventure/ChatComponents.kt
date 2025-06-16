package io.github.luposolitario.immundanoctis.ui.adventure

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.data.ChatMessage
import io.github.luposolitario.immundanoctis.data.GameCharacter

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
                Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(message.text)) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            "Copia",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    if (!isUserMessage) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(24.dp)
                        ) {
                            if (message.isTranslating) CircularProgressIndicator(strokeWidth = 2.dp)
                            else IconButton(
                                onClick = onTranslateClicked,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Translate,
                                    "Traduci",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    IconButton(onClick = onPlayClicked, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.PlayCircleOutline,
                            "Leggi",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
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
fun MessageInput(onMessageSent: (String) -> Unit, isEnabled: Boolean) {
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    val maxChars = 1024
    val isError = textState.text.length > maxChars // Correzione logica qui

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
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.7f
            )
        )
    }
}
