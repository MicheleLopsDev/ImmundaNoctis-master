package io.github.luposolitario.immundanoctis.ui.adventure

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
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.luposolitario.immundanoctis.R
import io.github.luposolitario.immundanoctis.data.CharacterType
import io.github.luposolitario.immundanoctis.data.GameCharacter

@Composable
fun AdventureHeader(
    characters: List<GameCharacter>,
    selectedCharacterId: String,
    onCharacterClick: (String) -> Unit
) {
    val characterOrder = mapOf("dm" to 0, "hero" to 1, "companion1" to 2, "companion2" to 3)
    val sortedCharacters = characters.sortedWith(compareBy { characterOrder[it.id] ?: Int.MAX_VALUE })

    Box(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.map_dungeon),
            contentDescription = "Mappa del Dungeon",
            contentScale = ContentScale.Companion.Crop,
            modifier = Modifier.Companion.fillMaxWidth().height(120.dp)
        )
        Row(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .align(Alignment.Companion.BottomCenter)
                .padding(top = 80.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Companion.Bottom
        ) {
            sortedCharacters.forEach { character ->
                if (character.type == CharacterType.PLAYER) {
                    // Non mostrare l'eroe nell'header, come da soluzione del bug
                } else if (character.isVisible) {
                    CharacterPortrait(
                        character = character,
                        isSelected = character.id == selectedCharacterId,
                        modifier = Modifier.Companion.clickable { onCharacterClick(character.id) },
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
fun CharacterPortrait(
    character: GameCharacter,
    isSelected: Boolean,
    modifier: Modifier = Modifier.Companion,
    size: Dp = 64.dp
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Companion.Transparent
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Companion.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = character.portraitResId),
            contentDescription = "Ritratto di ${character.name}",
            contentScale = ContentScale.Companion.Crop,
            modifier = Modifier.Companion
                .size(size)
                .clip(CircleShape)
                .border(3.dp, borderColor, CircleShape)
        )
        Text(
            text = character.name,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.Companion.padding(top = 2.dp)
        )
    }
}

@Composable
fun PlaceholderPortrait(modifier: Modifier = Modifier.Companion, size: Dp = 64.dp) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Companion.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.Companion
                .size(size)
                .clip(CircleShape)
                .background(Color.Companion.Black.copy(alpha = 0.8f))
                .border(3.dp, Color.Companion.DarkGray, CircleShape),
            contentAlignment = Alignment.Companion.Center
        ) {
            Icon(
                imageVector = Icons.Default.QuestionMark,
                contentDescription = "Personaggio Sconosciuto",
                tint = Color.Companion.Gray,
                modifier = Modifier.Companion.size(size * 0.6f)
            )
        }
        Text(
            text = "Sconosciuto",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.Companion.padding(top = 2.dp)
        )
    }
}
