// app/src/main/java/io/github/luposolitario/immundanoctis/data/GameData.kt

package io.github.luposolitario.immundanoctis.data

import androidx.annotation.DrawableRes
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID // Per generare gli UUID se necessario

object CharacterID {
    const val HERO = "hero"
    const val DM = "dm"
}

/**
 * Definisce il tipo di personaggio nel gioco.
 */
enum class CharacterType {
    DM,     // Dungeon Master
    PLAYER, // Il personaggio controllato dal giocatore
    NPC     // Personaggio non giocante (compagni, nemici, ecc.)
}

// Definisci l'enum per i livelli di sfida
enum class ChallengeLevel {
    BASE,
    MEDIUM,
    ADVANCED,
    MASTER;

    // Funzione helper per convertire una stringa (dal JSON o dal tag) nell'enum
    companion object {
        fun fromString(level: String): ChallengeLevel? {
            return entries.firstOrNull { it.name.equals(level, ignoreCase = true) }
        }
    }
}

// Nuova classe per gestire i testi localizzati (per futuri sviluppi con il parser)
data class LocalizedText(
    @JsonProperty("en") val en: String?,
    @JsonProperty("it") val it: String?
) {
    fun getLocalizedText(lang: String): String? {
        return when (lang.lowercase()) {
            "en" -> this.en
            "it" -> this.it
            else -> this.en // Fallback predefinito all'inglese o lingua base
        }
    }
}


data class CharacterStats(
    val id: String = UUID.randomUUID().toString(), // Aggiunto UUID
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
    val id: String = UUID.randomUUID().toString(), // Aggiunto UUID
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
    val id: String, // Assicurati che questo ID sia un UUID quando crei un GameCharacter
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

// Classe per i singoli parametri all'interno di un tag
data class TagParameter(
    val id: String = UUID.randomUUID().toString(), // Opzionale: Aggiunto UUID per l'istanza del parametro
    @JsonProperty("name") val name: String,
    // Il description e value del TagParameter saranno LocalizedText se deciderai di localizzarli
    @JsonProperty("description") val description: String?, // Attualmente String, ma potrebbe diventare LocalizedText
    @JsonProperty("default_value") val defaultValue: Any?,
    @JsonProperty("value") val value: Any? // Attualmente Any?, potrebbe diventare LocalizedText per descrizioni
)

// Classe per la configurazione di un singolo tag (TagConfig)
data class TagConfig(
    @JsonProperty("id") val id: String,
    @JsonProperty("type") val type: String,
    @JsonProperty("regex") val regex: String,
    // Il replacement sarà LocalizedText se deciderai di localizzarlo
    @JsonProperty("replacement") val replacement: String?, // Attualmente String, ma potrebbe diventare LocalizedText
    @JsonProperty("parameters") val parameters: List<TagParameter>?,
    @JsonProperty("actor") val actor: String,
    @JsonProperty("command") val command: String?,
    @JsonProperty("replace") val replace: Boolean
)

// Wrapper per la lista di tag (utile per la deserializzazione JSON)
data class TagsConfigWrapper(
    @JsonProperty("tags") val tags: List<TagConfig>
)

// Classe che rappresenta un'azione che il motore deve eseguire
data class EngineCommand(
    val commandName: String,
    val parameters: Map<String, Any?> // Mappa dei parametri del comando
)