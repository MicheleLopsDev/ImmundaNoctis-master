package io.github.luposolitario.immundanoctis.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.luposolitario.immundanoctis.R
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.data.CharacterType
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.HeroDetails
import io.github.luposolitario.immundanoctis.data.LoneWolfStats
import io.github.luposolitario.immundanoctis.data.SessionData
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class GameStateManager(private val context: Context) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val saveFile: File = File(context.filesDir, "session.json")

    fun saveSession(sessionData: SessionData) {
        try {
            FileWriter(saveFile).use { writer ->
                gson.toJson(sessionData, writer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadSession(): SessionData? {
        if (!saveFile.exists()) {
            return null
        }
        return try {
            FileReader(saveFile).use { reader ->
                gson.fromJson(reader, SessionData::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteSession(): Boolean {
        return if (saveFile.exists()) {
            saveFile.delete()
        } else {
            true // Il file non esiste, quindi è già "cancellato"
        }
    }

    fun createDefaultSession(): SessionData {
        val hero = GameCharacter(
            id = CharacterID.HERO,
            name = "Lupo Solitario",
            type = CharacterType.PLAYER,
            characterClass = "Guerriero Kai",
            portraitResId = R.drawable.portrait_hero_male,
            gender = "MALE",
            language = "it",
            stats = LoneWolfStats(combattivita = 15, resistenza = 25),
            kaiDisciplines = listOf("SIXTH_SENSE", "HEALING", "MINDSHIELD", "WEAPONSKILL", "HUNTING"),
            pasti = 2,
            details = HeroDetails(
                specialAbilities = listOf("Immunità alle malattie"),
                equippedWeapon = "Spada",
                equippedArmor = "Cotta di maglia",
                equippedShield = "Scudo di legno",
                coins = mapOf("gold" to 12)
            )
        )

        val dm = GameCharacter(
            id = CharacterID.DM,
            name = "Dungeon Master",
            type = CharacterType.DM,
            characterClass = "Narratore",
            portraitResId = R.drawable.portrait_dm,
            gender = "NEUTRAL",
            language = "it",
            stats = null
        )

        val elara = GameCharacter(
            id = "companion1",
            name = "Elara",
            type = CharacterType.NPC,
            characterClass = "Guaritrice",
            portraitResId = R.drawable.portrait_cleric,
            gender = "FEMALE",
            language = "it",
            isVisible = true,
            stats = LoneWolfStats(combattivita = 10, resistenza = 20)
        )

        // --- COMPAGNO RIMOSSO ---
        return SessionData(
            sessionName = "L'Ultimo dei Kai",
            lastUpdate = System.currentTimeMillis(),
            characters = listOf(hero, dm, elara)
        )
    }
}