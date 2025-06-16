package io.github.luposolitario.immundanoctis.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Rappresenta una singola abilità con i suoi livelli e descrizioni.
 * @param name Il nome dell'abilità.
 * @param icon L'icona che rappresenta l'abilità.
 * @param level Il livello attuale posseduto dal giocatore (da 1 a 3).
 * @param effects La lista di testi descrittivi per ogni livello. Deve avere 3 elementi.
 */
data class Skill(
    val name: String,
    val icon: ImageVector,
    val level: Int,
    val effects: List<String>
) {
    // Il livello massimo è sempre 3
    val maxLevel: Int = 3
}

/**
 * Un oggetto che contiene i dati placeholder per tutte le abilità del gioco,
 * divisi per categoria.
 */
object PlaceholderSkills {

    val strengthSkills = listOf(
        Skill(
            name = "Colpo Potente",
            icon = Icons.Default.AccessibilityNew,
            level = 2,
            effects = listOf(
                "Aggiungi +2 ai danni con armi a due mani.",
                "Il colpo può far barcollare un nemico di taglia media.",
                "Il bonus ai danni aumenta a +4."
            )
        ),
        Skill(
            name = "Spallata",
            icon = Icons.Default.SwapHoriz,
            level = 1,
            effects = listOf(
                "Puoi tentare di spingere via un nemico, facendogli perdere l'equilibrio.",
                "Se la spallata riesce, il nemico è considerato 'Prono'.",
                "La spallata non provoca attacchi di opportunità."
            )
        )
    )

    val cunningSkills = listOf(
        Skill(
            name = "Colpo Preciso",
            icon = Icons.Default.Visibility,
            level = 1,
            effects = listOf(
                "Aggiungi +1 al tiro per colpire quando usi armi leggere o a distanza.",
                "Puoi mirare a punti deboli, ignorando 1 punto di armatura del bersaglio.",
                "Il bonus al tiro per colpire aumenta a +2."
            )
        )
    )

    val knowledgeSkills = listOf(
        Skill(
            name = "Medicina",
            icon = Icons.Default.HealthAndSafety,
            level = 3,
            effects = listOf(
                "Puoi usare bende per curare 1d4 ferite.",
                "Puoi identificare veleni o malattie comuni.",
                "Le tue cure con le bende sono più efficaci (1d6 ferite)."
            )
        )
    )

    val spellSkills = listOf(
        Skill(
            name = "Dardo Magico",
            icon = Icons.Default.Bolt,
            level = 2,
            effects = listOf(
                "Lancia un dardo che infligge 1d6 danni magici.",
                "Il bersaglio subisce una penalità di -1 al prossimo tiro per colpire.",
                "Puoi lanciare un secondo dardo che infligge 1d4 danni."
            )
        ),
        Skill(
            name = "Armatura Magica",
            icon = Icons.Default.Shield,
            level = 1,
            effects = listOf(
                "Aumenta la tua classe armatura di +2 per 10 minuti.",
                "La durata aumenta a 30 minuti.",
                "Il bonus alla classe armatura aumenta a +3."
            )
        )
    )
}
