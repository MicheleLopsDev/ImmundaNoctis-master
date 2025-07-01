package io.github.luposolitario.immundanoctis.ui.adventure

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
        // --- MODIFICA AGGIUNTA QUI ---
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF8D6E63) // Colore personalizzato tipo legno
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Sezione 1: Identità (Ritratto, Nome, Grado)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = hero.portraitResId),
                    contentDescription = "Ritratto di ${hero.name}",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.Center) {
                    Text(hero.name, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MilitaryTech, contentDescription = "Grado Kai", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = kaiRank,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFFFF59D) // Giallo chiaro per contrasto
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sezione 2: Statistiche vitali
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(icon = Icons.Default.FitnessCenter, value = hero.stats?.combattivita ?: 0, label = "Combattività")
                StatItem(icon = Icons.Default.Favorite, value = hero.stats?.resistenza ?: 0, label = "Resistenza", iconColor = Color(0xFFE57373))
                StatItem(icon = Icons.Default.BakeryDining, value = hero.pasti, label = "Pasti")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sezione 3: Discipline possedute (solo icone)
            if (hero.kaiDisciplines.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items(hero.kaiDisciplines) { disciplineId ->
                        Icon(
                            imageVector = getIconForDiscipline(disciplineId),
                            contentDescription = disciplineId,
                            modifier = Modifier.padding(horizontal = 4.dp).size(24.dp),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composable per mostrare una singola statistica con icona, valore e etichetta.
 */
@Composable
private fun StatItem(icon: ImageVector, value: Int, label: String, iconColor: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(28.dp))
        Text(text = value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
    }
}