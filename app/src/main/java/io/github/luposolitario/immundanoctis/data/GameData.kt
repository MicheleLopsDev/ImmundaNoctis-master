package io.github.luposolitario.immundanoctis.data

import androidx.annotation.DrawableRes

object CharacterID {
    const val HERO = "hero"
    const val DM = "dm"
}

// --- NUOVA ENUM CLASS ---
/**
 * Definisce il tipo di personaggio nel gioco.
 */
enum class CharacterType {
    DM,     // Dungeon Master
    PLAYER, // Il personaggio controllato dal giocatore
    NPC     // Personaggio non giocante (compagni, nemici, ecc.)
}

data class CharacterStats(
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
    val specialAbilities: List<String>,
    val equippedWeapon: String,
    val equippedArmor: String,
    val equippedShield: String?,
    val coins: Map<String, Int>
)

/**
 * La classe principale che rappresenta un personaggio nel gioco.
 * AGGIORNATA per includere il tipo di personaggio.
 */
data class GameCharacter(
    val id: String,
    val name: String,
    // --- NUOVO CAMPO AGGIUNTO ---
    val type: CharacterType,
    val characterClass: String,
    @DrawableRes val portraitResId: Int,
    val gender: String,
    val language: String,
    val isVisible: Boolean = true,
    val stats: CharacterStats? = null,
    val details: HeroDetails? = null
)
