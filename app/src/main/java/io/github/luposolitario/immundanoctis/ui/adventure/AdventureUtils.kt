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
import io.github.luposolitario.immundanoctis.R
import io.github.luposolitario.immundanoctis.data.GameItem

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

@DrawableRes
private fun getIconForItem(item: GameItem?): Int? {
    if (item == null) return null
    return item.iconResId ?: when(item.name) {
        "Ascia" -> R.drawable.ic_axe
        "Spada" -> R.drawable.ic_sword
        "Mappa" -> R.drawable.ic_map_icon
        "Zaino" -> R.drawable.ic_backpack
        "Pozione di Vigorilla" -> R.drawable.ic_potion
        "Pasto" -> R.drawable.ic_meal
        "Corone d'Oro" -> R.drawable.ic_gold
        "Elmo" -> R.drawable.ic_helmet
        "Gilet di maglia di ferro" -> R.drawable.ic_armor
        "Mazza" -> R.drawable.ic_mace
        "Bastone" -> R.drawable.ic_staff
        "Lancia" -> R.drawable.ic_spear
        "Spada larga" -> R.drawable.ic_broadsword
        else -> null
    }
}
