package io.github.luposolitario.immundanoctis.ui.adventure

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctis.data.DisciplineChoice
import io.github.luposolitario.immundanoctis.data.KAI_DISCIPLINES
import io.github.luposolitario.immundanoctis.data.NarrativeChoice

/**
 * Un contenitore che mostra le scelte disponibili al giocatore in una colonna verticale.
 * @param narrativeChoices La lista delle scelte narrative standard.
 * @param disciplineChoices La lista delle prove di disciplina disponibili.
 * @param onNarrativeChoice Funzione da chiamare quando una scelta narrativa viene cliccata.
 * @param onDisciplineChoice Funzione da chiamare quando una prova di disciplina viene cliccata.
 */
@Composable
fun ChoicesContainer(
    narrativeChoices: List<NarrativeChoice>,
    disciplineChoices: List<DisciplineChoice>,
    onNarrativeChoice: (NarrativeChoice) -> Unit,
    onDisciplineChoice: (DisciplineChoice) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        narrativeChoices.forEach { choice ->
            ActionChoiceCard(
                text = choice.choiceText.italian ?: "N/A",
                isSpecial = false,
                onClick = { onNarrativeChoice(choice) }
            )
        }

        disciplineChoices.forEach { choice ->
            val disciplineInfo = KAI_DISCIPLINES.find { it.id == choice.disciplineId }
            val cardText = choice.choiceText?.italian ?: disciplineInfo?.name ?: "Azione Speciale"

            ActionChoiceCard(
                text = cardText,
                isSpecial = true,
                icon = getIconForDiscipline(choice.disciplineId),
                onClick = { onDisciplineChoice(choice) }
            )
        }
    }
}


/**
 * Il componente UI per una singola scelta cliccabile.
 * @param text Il testo da mostrare.
 * @param isSpecial Se true, applica lo stile "dorato" per le prove di abilità.
 * @param icon L'icona opzionale da mostrare per le prove di abilità.
 * @param onClick La funzione da eseguire al click.
 */
@Composable
private fun ActionChoiceCard(
    text: String,
    isSpecial: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val cardColors = if (isSpecial) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    val border = if (isSpecial) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = cardColors,
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontStyle = if (isSpecial) FontStyle.Italic else FontStyle.Normal,
                color = if(isSpecial) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- FUNZIONE DUPLICATA RIMOSSA ---