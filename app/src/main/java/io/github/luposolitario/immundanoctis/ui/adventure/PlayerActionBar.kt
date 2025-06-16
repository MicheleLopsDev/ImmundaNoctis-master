package io.github.luposolitario.immundanoctis.ui.adventure

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.luposolitario.immundanoctis.CharacterSheetActivity
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.PlaceholderSkills

@Composable
fun PlayerActionsBar(hero: GameCharacter) {
    var showStrengthDialog by remember { mutableStateOf(false) }
    var showCunningDialog by remember { mutableStateOf(false) }
    var showKnowledgeDialog by remember { mutableStateOf(false) }
    var showSpellDialog by remember { mutableStateOf(false) }
    var showDiceDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showStrengthDialog) {
        SkillDialog(
            title = "Abilità di Forza",
            skills = PlaceholderSkills.strengthSkills,
            onDismiss = { showStrengthDialog = false }
        )
    }
    if (showCunningDialog) {
        SkillDialog(
            title = "Abilità di Astuzia",
            skills = PlaceholderSkills.cunningSkills,
            onDismiss = { showCunningDialog = false }
        )
    }
    if (showKnowledgeDialog) {
        SkillDialog(
            title = "Abilità di Sapere",
            skills = PlaceholderSkills.knowledgeSkills,
            onDismiss = { showKnowledgeDialog = false }
        )
    }
    if (showSpellDialog) {
        SkillDialog(
            title = "Incantesimi",
            skills = PlaceholderSkills.spellSkills,
            onDismiss = { showSpellDialog = false }
        )
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
