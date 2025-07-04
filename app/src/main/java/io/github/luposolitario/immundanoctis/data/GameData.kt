// immundanoctis/data/GameData.kt

package io.github.luposolitario.immundanoctis.data

import io.github.luposolitario.immundanoctis.R
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

enum class ItemType {
    WEAPON,
    HELMET,
    ARMOR,
    SHIELD,
    BACKPACK_ITEM,
    SPECIAL_ITEM,
    GOLD,
    MEAL
}

data class GameItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: ItemType,
    var quantity: Int = 1,
    @DrawableRes val iconResId: Int? = null,
    val description: String? = null,
    val bonuses: Map<String, Int>? = null,
    val combatSkillBonus: Int = 0,
    val enduranceBonus: Int = 0,
    val isConsumable: Boolean = false,
    val isDiscardable: Boolean = true,
    val weaponType: WeaponType? = null,
    val notes: String? = null // <-- NUOVO CAMPO
)

val FISTS_WEAPON = GameItem(
    id = "FISTS",
    name = "Pugni",
    type = ItemType.WEAPON,
    quantity = 1,
    iconResId = R.drawable.ic_fists,
    description = "Attacco a mani nude. Non fornisce bonus significativi.",
    combatSkillBonus = 0,
    enduranceBonus = 0,
    isDiscardable = false,
    weaponType = WeaponType.FISTS
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

enum class ModifierSourceType {
    ITEM,
    DISCIPLINE,
    EVENT,
    RULE
}

enum class ModifierDuration {
    PERMANENT,
    UNTIL_UNEQUIPPED,
    UNTIL_COMBAT_END,
    UNTIL_SCENE_END,
    FOR_X_ROUNDS
}

data class StatModifier(
    val id: String = UUID.randomUUID().toString(),
    val statName: String,
    val amount: Int,
    val sourceType: ModifierSourceType,
    val sourceId: String,
    val duration: ModifierDuration,
    val roundsRemaining: Int? = null,
    val appliedTime: Long? = null
)

data class HeroDetails(
    val id: String = UUID.randomUUID().toString(),
    val specialAbilities: List<String>,
    val inventory: MutableList<GameItem> = mutableListOf(),
    val activeModifiers: MutableList<StatModifier> = mutableListOf(),
    val weaponSkillType: WeaponType? = null
)

data class GameCharacter(
    val id: String,
    val name: String,
    val type: CharacterType,
    @DrawableRes val portraitResId: Int,
    val gender: String,
    val language: String,
    val isVisible: Boolean = true,
    val stats: LoneWolfStats?,
    val kaiDisciplines: List<String> = emptyList(),
    val notes: String = "",
    val details: HeroDetails? = null
) {
    val characterClass: String = "Guerriero Kai"
}

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
    KaiDisciplineInfo("WEAPONSKILL", "Scherma", "+2 Combattività con un tipo di arma."), // Descrizione per il giocatore
    KaiDisciplineInfo("CAMOUFLAGE", "Mimetismo", "Permette di nascondersi e passare inosservato."),
    KaiDisciplineInfo("HUNTING", "Caccia", "Permette di trovare sempre cibo, non richiede Pasti."),
    KaiDisciplineInfo("SIXTH_SENSE", "Sesto Senso", "Avverte di pericoli imminenti."),
    KaiDisciplineInfo("TRACKING", "Orientamento", "Permette di seguire tracce e non perdersi."),
    KaiDisciplineInfo("HEALING", "Guarigione", "Ripristina 1 punto Resistenza per sezione senza combattimento."),
    KaiDisciplineInfo("MINDSHIELD", "Psicoschermo", "Immunità agli attacchi psichici."),
    KaiDisciplineInfo("MINDBLAST", "Psicolaser", "+2 Combattività in combattimento."),
    KaiDisciplineInfo("ANIMAL_KINSHIP", "Affinità Animale", "Permette di comunicare con gli animali."),
    KaiDisciplineInfo("MIND_OVER_MATTER", "Telecinesi", "Permette di muovere piccoli oggetti con la mente.")
)

enum class WeaponType {
    AXE,
    SWORD,
    MACE,
    STAFF,
    SPEAR,
    BROADSWORD,
    FISTS,
    GENERIC // <-- NUOVO TIPO AGGIUNTO
}

val WEAPON_TYPE_NAMES = mapOf(
    WeaponType.AXE to "Ascia",
    WeaponType.SWORD to "Spada",
    WeaponType.MACE to "Mazza",
    WeaponType.STAFF to "Bastone",
    WeaponType.SPEAR to "Lancia",
    WeaponType.BROADSWORD to "Spada Larga",
    WeaponType.FISTS to "Pugni",
    WeaponType.GENERIC to "Arma Generica" // <-- NUOVA MAPPA AGGIUNTA
)


// --- NUOVA MAPPA per i messaggi personalizzati della disciplina Scherma ---
val WEAPON_SKILL_DESCRIPTIONS = mapOf(
    WeaponType.FISTS to LocalizedText(
        english = "You've always been good at brawling, but at the monastery, they taught you the way of Bushido: bare-handed combat can be extremely lethal.",
        italian = "Sei sempre stato bravo a menare le mani, ma al monastero ti hanno insegnato la via del Bushido: il combattimento a mani nude può essere estremamente letale."
    ),
    WeaponType.SWORD to LocalizedText(
        english = "The noble Kai knights have always favored the use of what is the main weapon of Sommerlund's noble heroes, and they have blessed you with a unique talent.",
        italian = "I nobili cavalieri Kai hanno sempre privilegiato l'uso di quella che è l'arma principale dei nobili eroi del Sommerlund, e ti hanno benedetto con un talento unico."
    ),
    WeaponType.AXE to LocalizedText(
        english = "The brutality of the woods taught you that a sharp weapon in the back always does its duty, and like all animals of the forest, you are the fierce wolf.",
        italian = "La brutalità dei boschi ti ha insegnato che un'arma affilata nella schiena fa sempre il suo dovere, e come tutti gli animali dei boschi tu sei il lupo feroce."
    ),
    WeaponType.MACE to LocalizedText(
        english = "The crushing power of the mace is your innate strength. You wield it with a primal force that shatters defenses and spirits alike.",
        italian = "Il potere devastante della mazza è la tua forza innata. La brandisci con una forza primordiale che frantuma difese e spiriti allo stesso modo."
    ),
    WeaponType.STAFF to LocalizedText(
        english = "The simple staff becomes a deadly extension of your will. Your movements are fluid, unpredictable, turning defense into offense with elegant precision.",
        italian = "Il semplice bastone diventa una micidiale estensione della tua volontà. I tuoi movimenti sono fluidi, imprevedibili, trasformando la difesa in attacco con elegante precisione."
    ),
    WeaponType.SPEAR to LocalizedText(
        english = "The spear is an extension of your deadly reach. You strike from a distance with the cunning of a hunter, keeping your foes at bay with every thrust.",
        italian = "La lancia è un'estensione della tua portata micidiale. Colpisci da lontano con l'astuzia di un cacciatore, tenendo a bada i tuoi nemici con ogni affondo."
    ),
    WeaponType.BROADSWORD to LocalizedText(
        english = "The broadsword, a weapon of raw power, resonates with your martial spirit. Each swing is a declaration of dominance, cleaving through resistance with overwhelming force.",
        italian = "La spada larga, un'arma di pura potenza, risuona con il tuo spirito marziale. Ogni fendente è una dichiarazione di dominio, squarciando la resistenza con forza travolgente."
    )
)

val INITIAL_WEAPONS = listOf(
    GameItem(name = "Ascia", type = ItemType.WEAPON, description = "Un'arma affidabile e bilanciata.", iconResId = R.drawable.ic_axe, combatSkillBonus = 3, isDiscardable = true, weaponType = WeaponType.AXE),
    GameItem(name = "Spada", type = ItemType.WEAPON, description = "Veloce e letale, un classico per ogni avventuriero.", iconResId = R.drawable.ic_sword, combatSkillBonus = 4, isDiscardable = true, weaponType = WeaponType.SWORD),
    GameItem(name = "Mazza", type = ItemType.WEAPON, description = "Un'arma contundente e potente.", iconResId = R.drawable.ic_mace, combatSkillBonus = 2, isDiscardable = true, weaponType = WeaponType.MACE),
    GameItem(name = "Bastone", type = ItemType.WEAPON, description = "Utile per la difesa e come supporto.", iconResId = R.drawable.ic_staff, combatSkillBonus = 1, isDiscardable = true, weaponType = WeaponType.STAFF),
    GameItem(name = "Lancia", type = ItemType.WEAPON, description = "Un'arma a lunga gittata.", iconResId = R.drawable.ic_spear, combatSkillBonus = 3, isDiscardable = true, weaponType = WeaponType.SPEAR),
    GameItem(name = "Spada Larga", type = ItemType.WEAPON, description = "Un'arma imponente per un guerriero possente.", iconResId = R.drawable.ic_broadsword, combatSkillBonus = 5, isDiscardable = true, weaponType = WeaponType.BROADSWORD)
)

val INITIAL_SPECIAL_ITEMS = listOf(
    GameItem(name = "Mappa", type = ItemType.SPECIAL_ITEM, description = "Rivela la tua posizione nel mondo di gioco.", iconResId = R.drawable.ic_map_icon, isDiscardable = false),
    GameItem(name = "Elmo", type = ItemType.HELMET, description = "Aggiunge 2 punti RESISTENZA al tuo totale.", bonuses = mapOf("RESISTENZA" to 2), iconResId = R.drawable.ic_helmet, isDiscardable = true),
    GameItem(name = "Gilet di maglia di ferro", type = ItemType.ARMOR, description = "Aggiunge 4 punti RESISTENZA al tuo totale.", bonuses = mapOf("RESISTENZA" to 4), iconResId = R.drawable.ic_armor, isDiscardable = true)
)

val INITIAL_COMMON_ITEMS = listOf(
    GameItem(name = "Pozione Curativa", type = ItemType.BACKPACK_ITEM, quantity = 1, description = "Ripristina 4 punti Resistenza quando usata.", iconResId = R.drawable.ic_potion, isConsumable = true, isDiscardable = true),
    GameItem(name = "Pasto", type = ItemType.BACKPACK_ITEM, quantity = 2, description = "Un pasto nutriente per recuperare energie.", iconResId = R.drawable.ic_meal, isConsumable = false, isDiscardable = true),
    GameItem(name = "Corone d'Oro", type = ItemType.GOLD, quantity = 12, description = "Monete d'oro per acquisti e scambi.", iconResId = R.drawable.ic_gold, isConsumable = false, isDiscardable = false)
)

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
    val adventureName: String, // <--- Assicurati che questo campo sia presente
    val scenes: List<Scene>
)
data class SessionData(
    val sessionName: String,
    val lastUpdate: Long,
    val characters: List<GameCharacter>,
    val usedScenes: MutableList<String> = mutableListOf(),
    val isStarted: Boolean = false
)