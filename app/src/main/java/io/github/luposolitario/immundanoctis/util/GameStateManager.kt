package io.github.luposolitario.immundanoctis.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.luposolitario.immundanoctis.R
import io.github.luposolitario.immundanoctis.data.CharacterType
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.SessionData
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Gestisce il salvataggio e il caricamento dello stato della sessione di gioco
 * su un file JSON nella memoria interna dell'app.
 */
class GameStateManager(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    val savesDir = getAppSpecificDirectory(context, "saves")
    private val sessionFile = File(savesDir, SESSION_FILE_NAME)
    private val chatFile = File(savesDir, CHAT_FILE_NAME)

    fun saveSession(sessionData: SessionData) {
        FileWriter(sessionFile).use { writer ->
            gson.toJson(sessionData, writer)
        }
    }

    fun loadSession(): SessionData? {
        return if (sessionFile.exists()) {
            try {
                FileReader(sessionFile).use { reader ->
                    gson.fromJson(reader, SessionData::class.java)
                }
            } catch (e: Exception) {
                // In caso di file corrotto, restituisce null
                null
            }
        } else {
            null
        }
    }

    /**
     * Cancella il file della sessione di gioco e la chat associata.
     * Utile per resettare lo stato in caso di dati corrotti o per un nuovo inizio.
     *
     * @return `true` se i file sono stati cancellati con successo o non esistevano, `false` altrimenti.
     */
    fun deleteSession(): Boolean {
        if (chatFile.exists()) {
            chatFile.delete()
        }
        return if (sessionFile.exists()) {
            sessionFile.delete()
        } else {
            true // Consideriamo successo anche se il file non c'era
        }
    }

    fun createDefaultSession(): SessionData {
        val defaultCharacters = listOf(
            GameCharacter(
                "hero",
                "Eroe",
                CharacterType.PLAYER,
                "Guerriero",
                R.drawable.portrait_hero_male,
                "MALE",
                "it",
                isVisible = false,
                stats = null, // Inizializza con stats appropriate
                details = null
            ),
            GameCharacter(
                "dm",
                "Dungeon Master",
                CharacterType.DM,
                "Narratore",
                R.drawable.portrait_dm,
                "MALE",
                "it",
                isVisible = true
            ),
            GameCharacter(
                "companion1",
                "Elara",
                CharacterType.NPC,
                "Saggio",
                R.drawable.portrait_cleric,
                "FEMALE",
                "it",
                isVisible = true
            ),
            GameCharacter(
                "companion2",
                "Baldur",
                CharacterType.NPC,
                "Mago",
                R.drawable.portrait_mage,
                "MALE",
                "it",
                isVisible = false
            )
        )
        return SessionData(
            sessionName = "La Prova dell'Eroe",
            lastUpdate = System.currentTimeMillis(),
            characters = defaultCharacters,
            isStarted = false // NUOVO: La sessione non è ancora "iniziata"
        )
    }

    companion object {
        private const val SESSION_FILE_NAME = "game_session.json"
        private const val CHAT_FILE_NAME = "autosave_chat.json"
    }
}