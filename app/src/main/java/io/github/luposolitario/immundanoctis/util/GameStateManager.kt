// immundanoctis/util/GameStateManager.kt

package io.github.luposolitario.immundanoctis.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.luposolitario.immundanoctis.R
import io.github.luposolitario.immundanoctis.data.*
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class GameStateManager(private val context: android.content.Context) {
    private val TAG: String = "GameStateManager"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val saveFile: File = File(context.filesDir, "session.json")

    fun saveSession(sessionData: SessionData) {
        val updatedHero = sessionData.hero
        val updatedCharacters = sessionData.characters.map {
            if (it.id == CharacterID.HERO) updatedHero else it
        }
        val updatedSession = sessionData.copy(characters = updatedCharacters)
        try {
            FileWriter(saveFile).use { writer ->
                gson.toJson(updatedSession, writer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadSession(): SessionData? {
        if (!saveFile.exists()) {
            Log.d(TAG, "loadSession: file not exists! ${saveFile.absolutePath}")
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
            // characterClass rimosso dal costruttore, ora è una val nella classe GameCharacter
            portraitResId = R.drawable.ic_hero_portrait_placeholder,
            gender = "MALE",
            language = "it",
            stats = LoneWolfStats(combattivita = 15, resistenza = 25),
            kaiDisciplines = listOf("SIXTH_SENSE", "HEALING", "MINDSHIELD", "WEAPONSKILL", "HUNTING"),
            details = HeroDetails(
                specialAbilities = listOf("Immunità alle malattie"),
                inventory = mutableListOf(
                    GameItem(name = "Pasto", type = ItemType.BACKPACK_ITEM, quantity = 2)
                )
            )
        )

        val dm = GameCharacter(
            id = CharacterID.DM,
            name = "Dungeon Master",
            type = CharacterType.DM,
            // characterClass rimosso
            portraitResId = R.drawable.portrait_dm,
            gender = "NEUTRAL",
            language = "it",
            stats = null
        )

        val elara = GameCharacter(
            id = "companion1",
            name = "Elara",
            type = CharacterType.NPC,
            // characterClass rimosso
            portraitResId = R.drawable.portrait_elara,
            gender = "FEMALE",
            language = "it",
            isVisible = true,
            stats = LoneWolfStats(combattivita = 10, resistenza = 20)
        )

        return SessionData(
            sessionName = "L'Ultimo dei Kai",
            lastUpdate = System.currentTimeMillis(),
            hero = hero,
            characters = listOf(hero, dm, elara)
        )
    }
}