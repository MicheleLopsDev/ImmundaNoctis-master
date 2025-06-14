package io.github.luposolitario.immundanoctis.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.luposolitario.immundanoctis.R
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
    private val sessionFile = File(context.filesDir, SESSION_FILE_NAME)

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
     * NUOVA FUNZIONE: Cancella il file della sessione di gioco.
     * Utile per resettare lo stato in caso di dati corrotti.
     *
     * @return `true` se il file è stato cancellato con successo o non esisteva, `false` altrimenti.
     */
    fun deleteSession(): Boolean {
        return if (sessionFile.exists()) {
            sessionFile.delete()
        } else {
            true // Consideriamo successo anche se il file non c'era
        }
    }

    fun createDefaultSession(): SessionData {
        // ... (il resto del metodo createDefaultSession rimane invariato)
        val defaultCharacters = listOf(
            GameCharacter("hero", "Eroe", "Guerriero", R.drawable.portrait_hero_male, "MALE", "it"),
            GameCharacter("dm", "Dungeon Master", "Narratore", R.drawable.portrait_dm, "MALE", "it"),
            GameCharacter("companion1", "Elara", "Saggio", R.drawable.portrait_cleric, "FEMALE", "it"),
            GameCharacter("companion2", "Baldur", "Mago", R.drawable.portrait_mage, "MALE", "it")
        )
        return SessionData(
            sessionName = "La Prova dell'Eroe",
            lastUpdate = System.currentTimeMillis(),
            characters = defaultCharacters
        )
    }

    companion object {
        private const val SESSION_FILE_NAME = "game_session.json"
    }
}
