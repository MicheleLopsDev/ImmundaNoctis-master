package io.github.luposolitario.immundanoctis.ui.adventure

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.luposolitario.immundanoctis.CharacterSheetActivity
import io.github.luposolitario.immundanoctis.R
import io.github.luposolitario.immundanoctis.data.CharacterType
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.engine.TokenInfo
import io.github.luposolitario.immundanoctis.engine.TokenStatus

@Composable
fun AdventureHeader(
    characters: List<GameCharacter>,
    selectedCharacterId: String,
    onCharacterClick: (String) -> Unit
) {
    // Ripristiniamo la logica di ordinamento originale per mostrare tutti i personaggi
    val characterOrder = mapOf("dm" to 0, "hero" to 1, "companion1" to 2, "companion2" to 3)
    val sortedCharacters = characters.sortedWith(compareBy { characterOrder[it.id] ?: Int.MAX_VALUE })
    val context = LocalContext.current

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
            // Mostriamo tutti i personaggi che sono visibili
            sortedCharacters.forEach { character ->
                if (character.isVisible) {
                    val isHero = character.type == CharacterType.PLAYER

                    // Applichiamo un'azione di click diversa per l'eroe
                    val clickModifier = if (isHero) {
                        Modifier.clickable {
                            context.startActivity(Intent(context, CharacterSheetActivity::class.java))
                        }
                    } else {
                        Modifier.clickable { onCharacterClick(character.id) }
                    }

                    CharacterPortrait(
                        character = character,
                        isSelected = character.id == selectedCharacterId,
                        modifier = clickModifier,
                        size = if (character.type == CharacterType.DM) 72.dp else 60.dp
                    )
                }
            }
        }
    }
}

@Composable
fun TokenSemaphoreIndicator(tokenInfo: TokenInfo, onResetSession: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    val semaphoreColor = when (tokenInfo.status) {
        TokenStatus.GREEN -> Color.Green
        TokenStatus.YELLOW -> Color(0xFFFBC02D)
        TokenStatus.RED -> Color.Red
        TokenStatus.CRITICAL -> Color.Red
    }

    IconButton(onClick = { showDialog = true }) {
        Icon(
            imageVector = Icons.Default.Circle,
            contentDescription = "Stato Utilizzo Token",
            tint = semaphoreColor
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "Dettaglio Utilizzo Token") },
            text = {
                val criticalWarning = if (tokenInfo.status == TokenStatus.CRITICAL) {
                    "ATTENZIONE: Limite token quasi raggiunto! Resetta la sessione per continuare.\n\n"
                } else {
                    ""
                }
                Text(
                    criticalWarning +
                            "Hai utilizzato il ${tokenInfo.percentage}% dei token disponibili.\n" +
                            "Utilizzati: ${tokenInfo.count} / Massimi: ${tokenInfo.maxTokens}"
                )
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                if (tokenInfo.status == TokenStatus.CRITICAL) {
                    TextButton(
                        onClick = {
                            onResetSession()
                            showDialog = false
                        }
                    ) {
                        Text("RESET SESSIONE", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
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