package io.github.luposolitario.immundanoctis.ui.adventure

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.luposolitario.immundanoctis.R
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.ItemType
import io.github.luposolitario.immundanoctis.data.WeaponType // <--- NUOVO IMPORT
import io.github.luposolitario.immundanoctis.data.WEAPON_TYPE_NAMES // <--- NUOVO IMPORT


@Composable
fun PlayerActionsBar(
    hero: GameCharacter,
    kaiRank: String,
    isDiceRollEnabled: Boolean,
    onDiceRollClicked: () -> Unit
) {
    val borderColor = if (isDiceRollEnabled) Color(0xFFFFD700) else Color(0xFFC0C0C0) // Oro se abilitato, altrimenti Argento
    // --- LOGICA PER TROVARE I PASTI NELL'INVENTARIO ---
    val mealItem = hero.details?.inventory?.find { it.name == "Pasto" }
    val mealCount = mealItem?.quantity ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF8D6E63) // Colore marrone/legno
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.lupo_solitario),
                        contentDescription = "Dado del Destino",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(2.dp, borderColor, CircleShape)
                            .clickable(enabled = isDiceRollEnabled, onClick = onDiceRollClicked)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(hero.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = "Grado Kai", tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = kaiRank,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFF59D) // Giallo chiaro
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatItem(icon = Icons.Default.FitnessCenter, value = hero.stats?.combattivita ?: 0)
                    Spacer(modifier = Modifier.width(16.dp))
                    StatItem(icon = Icons.Default.Favorite, value = hero.stats?.resistenza ?: 0, iconColor = Color(0xFFE57373))
                    Spacer(modifier = Modifier.width(16.dp))
                    StatItem(icon = Icons.Default.BakeryDining, value = mealCount)
                }
            }

            if (hero.kaiDisciplines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items(hero.kaiDisciplines) { disciplineId ->
                        // --- MODIFICATO: Aggiungiamo il tipo di arma per Scherma ---
                        val textToDisplay = if (disciplineId == "WEAPONSKILL") {
                            val weaponSkillType = hero.details?.weaponSkillType
                            val weaponTypeName = if (weaponSkillType != null) {
                                WEAPON_TYPE_NAMES[weaponSkillType] ?: weaponSkillType.name
                            } else {
                                "Non Definito" // Fallback se non ancora scelto o salvato
                            }
                            "$weaponTypeName"
                        }
                        else {
                            ""//getDisciplineName(disciplineId)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                            Icon(
                                imageVector = getIconForDiscipline(disciplineId),
                                contentDescription = disciplineId,
                                modifier = Modifier.size(24.dp),
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = textToDisplay,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Funzione helper per ottenere il nome localizzato della disciplina
private fun getDisciplineName(disciplineId: String): String {
    return when (disciplineId) {
        "CAMOUFLAGE" -> "Mimetismo"
        "HUNTING" -> "Caccia"
        "SIXTH_SENSE" -> "Sesto Senso"
        "TRACKING" -> "Orientamento"
        "HEALING" -> "Guarigione"
        "WEAPONSKILL" -> "Scherma"
        "MINDSHIELD" -> "Psicoschermo"
        "MINDBLAST" -> "Psicolaser"
        "ANIMAL_KINSHIP" -> "Affinità Animale"
        "MIND_OVER_MATTER" -> "Telecinesi"
        else -> disciplineId
    }
}

@Composable
private fun StatItem(icon: ImageVector, value: Int, iconColor: Color = Color.White) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = value.toString(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
    }
}