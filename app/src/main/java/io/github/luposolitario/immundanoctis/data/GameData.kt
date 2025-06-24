package io.github.luposolitario.immundanoctis.data

import androidx.annotation.DrawableRes
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

object CharacterID {
    const val HERO = "hero"
    const val DM = "dm"
}

/**
 * Definisce il tipo di personaggio nel gioco.
 */
enum class CharacterType {
    DM,
    PLAYER,
    NPC
}

// Definisci l'enum per i livelli di sfida
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

// Nuova classe per gestire i testi localizzati (se decideremo di usarla in futuro)
data class LocalizedText(
    @JsonProperty("en") val en: String?,
    @JsonProperty("it") val it: String?
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

/**
 * La classe principale che rappresenta un personaggio nel gioco.
 */
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

// --- Classi per il Parser ---
data class TagParameter(
    val id: String = UUID.randomUUID().toString(),
    @JsonProperty("name") val name: String,
    @JsonProperty("description") val description: String?,
    @JsonProperty("default_value") val defaultValue: Any?,
    @JsonProperty("value") val value: Any?
)

data class TagConfig(
    @JsonProperty("id") val id: String,
    @JsonProperty("type") val type: String,
    @JsonProperty("regex") val regex: String,
    @JsonProperty("replacement") val replacement: String?,
    @JsonProperty("parameters") val parameters: List<TagParameter>?,
    @JsonProperty("actor") val actor: String,
    @JsonProperty("command") val command: String?,
    @JsonProperty("replace") val replace: Boolean
)

data class TagsConfigWrapper(
    @JsonProperty("tags") val tags: List<TagConfig>
)

data class EngineCommand(
    val commandName: String,
    val parameters: Map<String, Any?>
)

// --- NUOVE CLASSI DATI PER SCENE DI GIOCO (Macro-Attività 2) ---

/**
 * Rappresenta una prova di abilità all'interno di una scena.
 * Questi dati verranno estratti dai tag e usati per creare i pulsanti UI.
 */
data class GameChallenge(
    val id: String = UUID.randomUUID().toString(),
    @JsonProperty("ability_type") val abilityType: String, // es. "strength", "dexterity"
    @JsonProperty("challenge_level") val challengeLevel: String, // es. "base", "medium"
    @JsonProperty("description") val description: String, // Testo descrittivo della prova per la UI
    @JsonProperty("success_text") val successText: String, // Testo per successo
    @JsonProperty("failure_text") val failureText: String, // Testo per fallimento
    @JsonProperty("difficulty") val difficulty: Int // La difficoltà numerica per il tiro di dadi
)

/**
 * Rappresenta una scelta narrativa che porta a un'altra scena.
 * Questi dati verranno estratti dai tag e usati per creare i pulsanti UI.
 */
data class NarrativeChoice(
    val id: String = UUID.randomUUID().toString(),
    @JsonProperty("choice_text") val choiceText: String, // Testo della scelta per la UI
    @JsonProperty("next_scene_id") val nextSceneId: String, // ID della scena successiva
    @JsonProperty("consequence_text") val consequenceText: String? = null // Testo opzionale per la conseguenza
)

/**
 * Rappresenta una singola scena del gioco.
 * Contiene il testo narrativo e le liste di sfide e scelte.
 */
data class Scene(
    @JsonProperty("id") val id: String, // ID univoco della scena
    @JsonProperty("narrative_text") val narrativeText: String, // Testo descrittivo della scena
    @JsonProperty("challenges") val challenges: List<GameChallenge>? = null, // Prove di abilità nella scena
    @JsonProperty("choices") val choices: List<NarrativeChoice>? = null // Scelte narrative nella scena
)

/**
 * Wrapper per la lista di scene nel file JSON (per la deserializzazione).
 */
data class ScenesWrapper(
    @JsonProperty("scenes") val scenes: List<Scene>
)