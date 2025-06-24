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
) {
    fun getLocalizedText(lang: String): String? {
        return when (lang.lowercase()) {
            "en" -> this.en
            "it" -> this.it
            else -> this.en
        }
    }
}

data class CharacterStats(
    val id: String = UUID.randomUUID().toString(),
    val currentHp: Int,
    val maxHp: Int,
    val armorClass: Int,
    val strength: Int,
    val dexterity: Int,
    val constitution: Int,
    val intelligence: Int,
    val wisdom: Int,
    val charisma: Int
)

data class HeroDetails(
    val id: String = UUID.randomUUID().toString(),
    val specialAbilities: List<String>,
    val equippedWeapon: String,
    val equippedArmor: String,
    val equippedShield: String?,
    val coins: Map<String, Int>
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
    val stats: CharacterStats? = null,
    val details: HeroDetails? = null
)

data class TagParameter(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String?,
    val defaultValue: Any?,
    val value: Any?
)

data class TagConfig(
    val id: String,
    val type: String,
    val regex: String,
    val replacement: String?,
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

data class NarrativeChoice(
    val id: String = UUID.randomUUID().toString(),
    val choiceText: String,
    val nextSceneId: String,
    val consequenceText: String? = null
)

data class Scene(
    val id: String,
    val sceneType: SceneType,
    val genre: Genre,
    val challengeLevel: ChallengeLevel,
    val narrativeText: String,
    val challenges: List<GameChallenge>? = null,
    val choices: List<NarrativeChoice>? = null,
    val directionalChoicesTags: String? = null
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