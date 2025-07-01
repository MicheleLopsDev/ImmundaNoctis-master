package io.github.luposolitario.immundanoctis.ui.adventure

import io.github.luposolitario.immundanoctis.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import io.github.luposolitario.immundanoctis.data.GameCharacter

/**
 * La nuova barra di stato del giocatore, che mostra tutte le informazioni vitali.
 * @param hero L'oggetto GameCharacter dell'eroe.
 * @param kaiRank Il grado Kai calcolato dal ViewModel.
 */
@Composable
fun PlayerActionsBar(
    hero: GameCharacter,
    kaiRank: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF8D6E63) // Colore personalizzato tipo legno
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
            // --- SEZIONE UNIFICATA E ORIZZONTALE ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Distribuisce lo spazio
            ) {
                // Gruppo 1: Identità del Giocatore
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.lupo_solitario),
                        contentDescription = "Ritratto di ${hero.name}",
                        modifier = Modifier
                            .size(48.dp) // Leggermente più compatto
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFFFFF59D), CircleShape)
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
                                color = Color(0xFFFFF59D)
                            )
                        }
                    }
                }

                // Gruppo 2: Statistiche Vitali
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatItem(icon = Icons.Default.FitnessCenter, value = hero.stats?.combattivita ?: 0)
                    Spacer(modifier = Modifier.width(16.dp))
                    StatItem(icon = Icons.Default.Favorite, value = hero.stats?.resistenza ?: 0, iconColor = Color(0xFFE57373))
                    Spacer(modifier = Modifier.width(16.dp))
                    StatItem(icon = Icons.Default.BakeryDining, value = hero.pasti)
                }
            }

            // Sezione Discipline (rimane sotto, se presente)
            if (hero.kaiDisciplines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items(hero.kaiDisciplines) { disciplineId ->
                        Icon(
                            imageVector = getIconForDiscipline(disciplineId),
                            contentDescription = disciplineId,
                            modifier = Modifier.padding(horizontal = 4.dp).size(24.dp),
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composable per una singola statistica (solo icona e valore).
 */
@Composable
private fun StatItem(icon: ImageVector, value: Int, iconColor: Color = Color.White) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = value.toString(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
    }
}