package io.github.luposolitario.immundanoctis.util

import android.content.Context
import com.google.gson.Gson
import io.github.luposolitario.immundanoctis.R
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.SessionData

/**
 * Gestisce la logica di salvataggio e caricamento della sessione di gioco
 * tramite SharedPreferences e JSON.
 */
class GameStateManager(context: Context) {

    private val prefs = context.getSharedPreferences("game_session_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_SESSION_DATA = "session_data_json"
    }

    /**
     * Salva l'intero oggetto SessionData come stringa JSON.
     */
    fun saveSession(session: SessionData) {
        val sessionJson = gson.toJson(session)
        prefs.edit().putString(KEY_SESSION_DATA, sessionJson).apply()
    }

    /**
     * Carica la stringa JSON della sessione e la converte kembali in un oggetto SessionData.
     * @return L'oggetto SessionData se esiste, altrimenti null.
     */
    fun loadSession(): SessionData? {
        val sessionJson = prefs.getString(KEY_SESSION_DATA, null)
        return if (sessionJson != null) {
            gson.fromJson(sessionJson, SessionData::class.java)
        } else {
            null
        }
    }

    /**
     * Crea una sessione di default, con un party iniziale e un eroe "vuoto".
     * Questa funzione viene chiamata solo la prima volta o quando si crea una nuova partita.
     */
    fun createDefaultSession(): SessionData {
        val defaultCharacters = listOf(
            // Personaggi Non Giocanti
            GameCharacter(
                id = CharacterID.DM,
                name = "Master",
                characterClass = "Dungeon Master",
                portraitResId = R.drawable.portrait_dm,
                gender = "MALE",
                language = "it"
            ),
            GameCharacter(
                id = "player2",
                name = "Kael",
                characterClass = "Mago",
                portraitResId = R.drawable.portrait_mage,
                gender = "MALE",
                language = "it"
            ),
            GameCharacter(
                id = "player3",
                name = "Elara",
                characterClass = "Saggio", // Aggiornato da Chierica a Saggio
                portraitResId = R.drawable.portrait_cleric,
                gender = "FEMALE",
                language = "it"
            ),
            // Eroe vuoto, da personalizzare
            GameCharacter(
                id = CharacterID.HERO,
                name = "", // Nome vuoto
                characterClass = "", // Classe vuota
                portraitResId = R.drawable.portrait_hero_male, // Ritratto di default
                gender = "MALE", // Genere di default
                language = "it" // Lingua di default
            )
        )
        // Restituisce un oggetto SessionData completo
        return SessionData("Nuova Campagna", System.currentTimeMillis(), defaultCharacters)
    }
}
