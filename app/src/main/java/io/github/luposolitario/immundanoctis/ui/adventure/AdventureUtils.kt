package io.github.luposolitario.immundanoctis.ui.adventure

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Funzione helper centralizzata per mappare un ID di disciplina a un'icona.
 */
fun getIconForDiscipline(disciplineId: String): ImageVector {
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
        "MIND_OVER_MATTER" -> Icons.Default.Star
        else -> Icons.Default.HelpOutline
    }
}