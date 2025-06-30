package io.github.luposolitario.immundanoctis.data

import androidx.annotation.DrawableRes
import java.util.UUID

object CharacterID {
    const val HERO = "hero"
    const val DM = "dm"
}

enum class CharacterType {
    DM,
    PLAYER,
    NPC
}

enum class ChallengeLevel {
    BASE,
    MEDIUM,
    ADVANCED,
    MASTER;

    companion object {
        fun fromString(level: String): ChallengeLevel? {
            return entries.firstOrNull { it.name.equals(level, ignoreCase = true) }
        }
    }
}

enum class SceneType {
    START,
    ENDING,
    TRANSITION
}

enum class Genre {
    ACTION,
    COMEDY,
    DRAMA,
    SCI_FI,
    HORROR,
    ADVENTURE,
    FANTASY,
    ANIMATION,
    THRILLER,
    WESTERN
}

data class LocalizedText(
    val en: String?,
    val it: String?
)

// DA COSÌ:
// val choiceText: String,

// A COSÌ:
data class NarrativeChoice(
    val id: String,
    val choiceText: LocalizedText, // <-- MODIFICA QUI
    val nextSceneId: String
)

/**
 * Contiene le statistiche fondamentali per il sistema di regole di Lupo Solitario.
 */
data class LoneWolfStats(
    val combattivita: Int,
    val resistenza: Int
    // In futuro potremmo aggiungere qui altri campi come 'rank' o 'oro'.
)

data class HeroDetails(
    val id: String = UUID.randomUUID().toString(),
    val specialAbilities: List<String>,
    val equippedWeapon: String,
    val equippedArmor: String,
    val equippedShield: String?,
    val coins: Map<String, Int>
)

// VERSIONE MODIFICATA
data class GameCharacter(
    val id: String,
    val name: String,
    val type: CharacterType,
    val characterClass: String, // Potremmo rinominarlo 'rankName' in futuro
    @DrawableRes val portraitResId: Int,
    val gender: String,
    val language: String,
    val isVisible: Boolean = true,
    // SOSTITUIAMO IL VECCHIO CAMPO 'stats'
    val stats: LoneWolfStats?,
    // AGGIUNGIAMO IL NUOVO CAMPO PER LE DISCIPLINE
    val kaiDisciplines: List<String> = emptyList(),
    val details: HeroDetails? = null // 'details' potrebbe contenere l'inventario in futuro
)

// Contenitore per le scelte legate alle discipline
data class DisciplineChoice(
    val disciplineId: String,
    val nextSceneId: String
)

// --- CLASSE SCENE FINALE E COMPLETA ---
data class Scene(
    val id: String,
    val sceneType: SceneType,
    val genre: Genre,
    val narrativeText: LocalizedText,
    val images: List<SceneImage>? = null,
    val choices: List<NarrativeChoice>? = null,
    val disciplineChoices: List<DisciplineChoice>? = null,
    val location: LocationInfo? = null,
    val challengeLevel: ChallengeLevel
)



data class TagParameter(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String?,
    val defaultValue: Any?,
    val value: Any?
)
// In fondo a GameData.kt

/**
 * Contiene le informazioni per una singola Disciplina Kai.
 */
data class KaiDisciplineInfo(val id: String, val name: String, val description: String)

/**
 * Lista ufficiale delle 10 Discipline Kai iniziali.
 */
val KAI_DISCIPLINES = listOf(
    KaiDisciplineInfo("CAMOUFLAGE", "Mimetismo", "Permette di nascondersi e passare inosservato."),
    KaiDisciplineInfo("HUNTING", "Caccia", "Permette di trovare sempre cibo, non richiede Pasti."),
    KaiDisciplineInfo("SIXTH_SENSE", "Sesto Senso", "Avverte di pericoli imminenti."),
    KaiDisciplineInfo("TRACKING", "Orientamento", "Permette di seguire tracce e non perdersi."),
    KaiDisciplineInfo("HEALING", "Guarigione", "Ripristina 1 punto Resistenza per sezione senza combattimento."),
    KaiDisciplineInfo("WEAPONSKILL", "Scherma", "+2 Combattività con un tipo di arma."),
    KaiDisciplineInfo("MINDSHIELD", "Psicoschermo", "Immunità agli attacchi psichici."),
    KaiDisciplineInfo("MINDBLAST", "Psicolaser", "+2 Combattività in combattimento."),
    KaiDisciplineInfo("ANIMAL_KINSHIP", "Affinità Animale", "Permette di comunicare con gli animali."),
    KaiDisciplineInfo("MIND_OVER_MATTER", "Telecinesi", "Permette di muovere piccoli oggetti con la mente.")
)
data class TagConfig(
    val id: String,
    val type: String,
    val regex: String,
    val replacement: Map<String, String>?, // <-- MODIFICA QUI
    val parameters: List<TagParameter>?,
    val actor: String,
    val command: String?,
    val replace: Boolean
)
data class TagsConfigWrapper(
    val tags: List<TagConfig>
)

data class EngineCommand(
    val commandName: String,
    val parameters: Map<String, Any?>
)

data class GameChallenge(
    val id: String = UUID.randomUUID().toString(),
    val abilityType: String,
    val description: String,
    val successText: String,
    val failureText: String
)

/**
 * Rappresenta un'immagine all'interno di una scena, con una didascalia localizzata.
 */
data class SceneImage(
    val imageUrl: String,
    val caption: LocalizedText? // La didascalia è opzionale
)

/**
 * Contiene informazioni geografiche su una scena.
 */
data class LocationInfo(
    val areaName: LocalizedText,
    val coordinates: String? = null,
    val isMajorLocation: Boolean = false
)

data class ScenesWrapper(
    val scenes: List<Scene>
)

data class SessionData(
    val sessionName: String,
    val lastUpdate: Long,
    val characters: List<GameCharacter>,
    val usedScenes: MutableList<String> = mutableListOf(),
    val isStarted: Boolean = false
)