package io.github.luposolitario.immundanoctis.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.luposolitario.immundanoctis.R
import io.github.luposolitario.immundanoctis.data.*
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
            true
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
            // 'pasti' rimosso da qui
            details = HeroDetails(
                specialAbilities = listOf("Immunità alle malattie"),
                // L'inventario viene inizializzato qui, con i pasti inclusi
                inventory = mutableListOf(
                    GameItem(name = "Pasto", type = ItemType.BACKPACK_ITEM, quantity = 2)
                )
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
            portraitResId = R.drawable.portrait_elara,
            gender = "FEMALE",
            language = "it",
            isVisible = true,
            stats = LoneWolfStats(combattivita = 10, resistenza = 20)
        )

        return SessionData(
            sessionName = "L'Ultimo dei Kai",
            lastUpdate = System.currentTimeMillis(),
            characters = listOf(hero, dm, elara)
        )
    }
}