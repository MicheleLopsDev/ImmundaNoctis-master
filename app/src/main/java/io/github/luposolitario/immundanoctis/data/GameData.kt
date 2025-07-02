// File: GameData.kt
package io.github.luposolitario.immundanoctis.data

import io.github.luposolitario.immundanoctis.R
import androidx.annotation.DrawableRes
import com.fasterxml.jackson.annotation.JsonProperty
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

enum class ItemType {
    WEAPON,
    HELMET,
    ARMOR,
    SHIELD,
    BACKPACK_ITEM,
    SPECIAL_ITEM,
    GOLD
}

data class GameItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: ItemType,
    var quantity: Int = 1,
    @DrawableRes val iconResId: Int? = null,
    val description: String? = null,
    val bonuses: Map<String, Int>? = null
)

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
    val english: String?,
    val italian: String?
)

data class NarrativeChoice(
    val id: String,
    val choiceText: LocalizedText,
    val nextSceneId: String,
    val minRoll: Int? = null,
    val maxRoll: Int? = null
)

data class LoneWolfStats(
    val combattivita: Int,
    val resistenza: Int
)

data class HeroDetails(
    val id: String = UUID.randomUUID().toString(),
    val specialAbilities: List<String>,
    val inventory: MutableList<GameItem> = mutableListOf()
)

data class GameCharacter(
    val id: String,
    val name: String,
    val type: CharacterType,
    val characterClass: String,
    @DrawableRes val portraitResId: Int,
    val gender: String,
    val language: String,
    val isVisible: Boolean = true,
    val stats: LoneWolfStats?,
    val kaiDisciplines: List<String> = emptyList(),
    val notes: String = "",
    val details: HeroDetails? = null
)

data class DisciplineChoice(
    val disciplineId: String,
    val choiceText: LocalizedText?,
    val nextSceneId: String
)

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

data class KaiDisciplineInfo(val id: String, val name: String, val description: String)

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

val INITIAL_WEAPONS = listOf(
    GameItem(name = "Ascia", type = ItemType.WEAPON, description = "Un'arma affidabile e bilanciata.", iconResId = R.drawable.ic_axe),
    GameItem(name = "Spada", type = ItemType.WEAPON, description = "Veloce e letale, un classico per ogni avventuriero.", iconResId = R.drawable.ic_sword),
    GameItem(name = "Mazza", type = ItemType.WEAPON, description = "Un'arma contundente e potente.", iconResId = R.drawable.ic_mace),
    GameItem(name = "Bastone", type = ItemType.WEAPON, description = "Utile per la difesa e come supporto.", iconResId = R.drawable.ic_staff),
    GameItem(name = "Lancia", type = ItemType.WEAPON, description = "Un'arma a lunga gittata.", iconResId = R.drawable.ic_spear),
    GameItem(name = "Spada Larga", type = ItemType.WEAPON, description = "Un'arma imponente per un guerriero possente.", iconResId = R.drawable.ic_broadsword)
)

val INITIAL_SPECIAL_ITEMS = listOf(
    GameItem(name = "Mappa", type = ItemType.SPECIAL_ITEM, description = "Rivela la tua posizione nel mondo di gioco.", iconResId = R.drawable.ic_map),
    // Rimossa la Pozione di Vigorilla da qui, ora è un oggetto comune
    GameItem(name = "Elmo", type = ItemType.HELMET, description = "Aggiunge 2 punti RESISTENZA al tuo totale.", bonuses = mapOf("RESISTENZA" to 2), iconResId = R.drawable.ic_helmet),
    GameItem(name = "Gilet di maglia di ferro", type = ItemType.ARMOR, description = "Aggiunge 4 punti RESISTENZA al tuo totale.", bonuses = mapOf("RESISTENZA" to 4), iconResId = R.drawable.ic_armor)
)

// --- NUOVA LISTA PER GLI OGGETTI COMUNI INIZIALI ---
val INITIAL_COMMON_ITEMS = listOf(
    GameItem(name = "Zaino", type = ItemType.BACKPACK_ITEM, quantity = 1, description = "Permette di trasportare fino a 8 oggetti.", iconResId = R.drawable.ic_backpack),
    GameItem(name = "Pozione Curativa", type = ItemType.BACKPACK_ITEM, quantity = 1, description = "Ripristina 4 punti Resistenza quando usata.", iconResId = R.drawable.ic_potion),
    GameItem(name = "Pasto", type = ItemType.BACKPACK_ITEM, quantity = 2, description = "Un pasto nutriente per recuperare energie."),
    GameItem(name = "Corone d'Oro", type = ItemType.GOLD, quantity = 0, description = "Monete d'oro per acquisti e scambi.") // La quantità sarà randomizzata nel ViewModel
)
// --- FINE NUOVA LISTA ---

data class TagConfig(
    val id: String,
    val type: String,
    val regex: String,
    val replacement: Map<String, String>?,
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

data class SceneImage(
    val imageUrl: String,
    val caption: LocalizedText?
)

data class LocationInfo(
    val areaName: LocalizedText,
    val coordinates: String? = null,
    val isMajorLocation: Boolean = false
)

data class EquipmentItem(val name: String, val description: String, @DrawableRes val iconResId: Int, val type: ItemType)


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