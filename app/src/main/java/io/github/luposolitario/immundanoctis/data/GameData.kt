package io.github.luposolitario.immundanoctis.data

import androidx.annotation.DrawableRes

object CharacterID {
    const val HERO = "hero"
    const val DM = "dm"
}

/**
 * Contiene le statistiche di combattimento e le caratteristiche base
 * di un personaggio in stile D&D.
 */
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

/**
 * Contiene i dettagli aggiuntivi del personaggio, come abilità speciali,
 * equipaggiamento indossato e inventario.
 */
data class HeroDetails(
    val specialAbilities: List<String>,
    val equippedWeapon: String,
    val equippedArmor: String,
    val equippedShield: String?, // Opzionale, può essere null
    val coins: Map<String, Int> // Es. "GP" -> 10, "SP" -> 50
)

/**
 * La classe principale che rappresenta un personaggio nel gioco.
 * Assembla tutte le altre informazioni (anagrafica, statistiche, dettagli)
 * in un unico oggetto.
 *
 * AGGIORNATA per includere genere e lingua.
 */
data class GameCharacter(
    val id: String,
    val name: String,
    val characterClass: String,
    @DrawableRes val portraitResId: Int,
    val gender: String,      // "MALE" o "FEMALE"
    val language: String,    // "it" o "en"
    val stats: CharacterStats? = null,
    val details: HeroDetails? = null
)
