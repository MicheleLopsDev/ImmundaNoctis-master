package io.github.luposolitario.immundanoctis.ui.adventure

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.luposolitario.immundanoctis.CharacterSheetActivity
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.KAI_DISCIPLINES

@Composable
fun PlayerActionsBar(
    hero: GameCharacter,
    usableDisciplines: Set<String>,
    onDisciplineClicked: (String) -> Unit
)  {
    var showStrengthDialog by remember { mutableStateOf(false) }
    var showCunningDialog by remember { mutableStateOf(false) }
    var showKnowledgeDialog by remember { mutableStateOf(false) }
    var showSpellDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val playerDisciplines = KAI_DISCIPLINES.filter { hero.kaiDisciplines.contains(it.id) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        ActionIcon(icon = Icons.Default.FitnessCenter, label = "Forza", enabled = true ,onClick = { showStrengthDialog = true })
        ActionIcon(icon = Icons.Default.Lightbulb, label = "Astuzia", enabled = true , onClick = { showCunningDialog = true })
        ActionIcon(icon = Icons.Default.MenuBook, label = "Sapere", enabled = true , onClick = { showKnowledgeDialog = true })
        ActionIcon(icon = Icons.Default.AutoFixHigh, label = "Magia", enabled = true , onClick = { showSpellDialog = true })

        }
    }


// Funzione helper per mappare un ID di disciplina a un'icona
private fun getIconForDiscipline(disciplineId: String): ImageVector {
    return when (disciplineId) {
        "CAMOUFLAGE" -> Icons.Default.VisibilityOff
        "HUNTING" -> Icons.Default.Pets
        "SIXTH_SENSE" -> Icons.Default.Hearing
        "TRACKING" -> Icons.Default.LocationSearching
        "HEALING" -> Icons.Default.Healing
        "WEAPONSKILL" -> Icons.Default.Shield
        "MINDSHIELD" -> Icons.Default.Security
        "MINDBLAST" -> Icons.Default.Psychology
        "ANIMAL_KINSHIP" -> Icons.Default.Group
        "MIND_OVER_MATTER" -> Icons.Default.Star // La nuova icona a stella
        else -> Icons.Default.HelpOutline
    }
}

@Composable
private fun ActionIcon(label: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
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
