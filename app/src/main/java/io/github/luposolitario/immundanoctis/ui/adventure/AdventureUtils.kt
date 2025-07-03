package io.github.luposolitario.immundanoctis.ui.adventure

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import io.github.luposolitario.immundanoctis.data.GameItem
import androidx.compose.material3.AlertDialog // AGGIUNTO IMPORT
import androidx.compose.material3.Button // AGGIUNTO IMPORT
import androidx.compose.material3.Text // AGGIUNTO IMPORT
import androidx.compose.material3.MaterialTheme // AGGIUNTO IMPORT
import androidx.compose.material3.ExperimentalMaterial3Api // AGGIUNTO IMPORT
import androidx.compose.foundation.layout.Column // AGGIUNTO IMPORT
import androidx.compose.foundation.layout.Spacer // AGGIUNTO IMPORT
import androidx.compose.foundation.layout.height // AGGIUNTO IMPORT
import androidx.compose.foundation.layout.padding // AGGIUNTO IMPORT
import androidx.compose.ui.unit.dp // AGGIUNTO IMPORT
import androidx.compose.ui.text.style.TextAlign // AGGIUNTO IMPORT
import androidx.compose.ui.text.font.FontWeight // AGGIUNTO IMPORT
import androidx.compose.ui.platform.LocalContext // AGGIUNTO IMPORT
import androidx.compose.ui.platform.LocalConfiguration // AGGIUNTO IMPORT
import io.github.luposolitario.immundanoctis.data.WeaponType // AGGIUNTO IMPORT
import io.github.luposolitario.immundanoctis.data.WEAPON_TYPE_NAMES // AGGIUNTO IMPORT
import io.github.luposolitario.immundanoctis.data.WEAPON_SKILL_DESCRIPTIONS // AGGIUNTO IMPORT
import java.util.Locale // AGGIUNTO IMPORT
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

/**
 * Un Composable robusto per visualizzare un'immagine da una risorsa drawable.
 * Mostra un'icona di placeholder in caso di errore di caricamento.
 */
@Composable
fun RobustImage(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: @Composable (Modifier) -> Unit = { mod ->
        Icon(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = "Immagine non caricata",
            modifier = mod
        )
    }
) {
    val painter = painterResource(id = resId)
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}





// --- NUOVO COMPOSABLE: WeaponSkillSelectionDialog (spostato da SetupActivity.kt) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeaponSkillSelectionDialog(
    weaponType: WeaponType,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val languageCode = context.resources.configuration.locales.get(0).language

    val dialogTitle = "Scherma: Talento Focalizzato" // Puoi definire questa stringa in strings.xml: R.string.weaponskill_dialog_title
    val weaponTypeName = WEAPON_TYPE_NAMES[weaponType] ?: weaponType.name // Nome localizzato dell'arma
    val descriptionText = when (languageCode) {
        "it" -> WEAPON_SKILL_DESCRIPTIONS[weaponType]?.italian
        else -> WEAPON_SKILL_DESCRIPTIONS[weaponType]?.english // Fallback a inglese
    }

    AlertDialog(
        onDismissRequest = { /* Non dismissabile senza conferma */ },
        title = { Text(dialogTitle) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "La tua affinità per la Scherma si concentra su:",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = weaponTypeName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (descriptionText != null) {
                    Text(
                        text = descriptionText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Un talento speciale per ${weaponTypeName}.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Conferma")
            }
        }
    )
}
