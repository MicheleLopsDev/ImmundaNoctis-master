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

// Il costruttore rimane privato per il pattern Singleton.
class GameStateManager private constructor(context: android.content.Context) {
    private val TAG: String = "GameStateManager"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val saveFile: File = File(context.filesDir, "session.json")

    // --- NUOVA PROPRIETÀ PER IL CACHING ---
    // Questa variabile conterrà la sessione caricata in memoria.
    // È 'private' e 'nullable'. Sarà 'null' finché non verrà chiamato loadSession().
    private var cachedSession: SessionData? = null

    /**
     * Carica la sessione di gioco.
     * Implementa una strategia lazy + cache.
     *
     * @param forceReload Se true, ignora la cache e ricarica i dati dal file.
     * Utile quando sai che il file è stato modificato esternamente.
     * @return L'oggetto SessionData. Se non esiste un salvataggio, ne crea uno di default.
     */
    fun loadSession(forceCreate: Boolean = false,forceReload: Boolean = false ): SessionData {


       if(!forceCreate) {


           // --- LOGICA DI CACHING ---
           // Se abbiamo una sessione in cache e non stiamo forzando il ricaricamento,
           // restituiamo subito l'istanza in memoria. È super veloce.
           if (cachedSession != null && !forceReload) {
               Log.d(TAG, "Restituita sessione dalla cache.")
               return cachedSession!!
           }

           // --- LOGICA DI CARICAMENTO (CACHE MISS O FORCE RELOAD) ---
           // Se la cache è vuota o forceReload è true, leggiamo dal file.
           Log.d(TAG, "Caricamento sessione da file (forceReload=$forceReload).")
           val loadedFromFile = if (saveFile.exists()) {
               try {
                   FileReader(saveFile).use { reader ->
                       gson.fromJson(reader, SessionData::class.java)
                   }
               } catch (e: Exception) {
                   Log.e(TAG, "loadSession: file not exists! ${saveFile.absolutePath}", e)
                   null
               }
           } else {
               Log.d(TAG, "loadSession: file not exists! ${saveFile.absolutePath}")
               null
           }

           // Se il caricamento dal file fallisce o il file non esiste, creiamo una sessione di default.
           // In ogni caso, aggiorniamo la nostra cache con i dati "freschi".
           cachedSession = loadedFromFile ?: createDefaultSession()

           // Restituiamo la sessione appena caricata (o creata).
           return cachedSession!!
       }else{

           // Se il caricamento dal file fallisce o il file non esiste, creiamo una sessione di default.
           // In ogni caso, aggiorniamo la nostra cache con i dati "freschi".

           saveSession(createDefaultSession())

           // Restituiamo la sessione appena caricata (o creata).
           return cachedSession!!
       }
    }

    /**
     * Salva una data sessione su file e aggiorna la cache interna.
     *
     * @param sessionData L'oggetto SessionData da salvare.
     */
    fun saveSession(sessionData: SessionData): Boolean {
        try {
            val updatedHero = sessionData.hero
            val updatedCharacters = sessionData.characters.map {
                if (it.id == CharacterID.HERO) updatedHero else it
            }
            val updatedSession = sessionData.copy(characters = updatedCharacters)
            FileWriter(saveFile).use { writer ->
                gson.toJson(updatedSession, writer)
            }
            // --- AGGIORNAMENTO CACHE ---
            // Dopo aver salvato su file, aggiorniamo anche la nostra cache interna
            // per mantenerla sincronizzata.
            this.cachedSession = sessionData
            Log.d(TAG, "Sessione salvata su file e cache aggiornata.")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "loadSession: file not exists! ${saveFile.absolutePath}",e)
            return false
        }
    }

    fun restartAdventure() {
        // 1. Carica la sessione corrente (usando la logica di cache esistente).
        val currentSession = loadSession()

        // 2. Crea una copia della sessione con il flag 'isStarted' impostato a false.
        val updatedSession = currentSession.copy(isStarted = false)

        // 3. Usa il metodo saveSession esistente per salvare i dati aggiornati.
        // Questo si occuperà di scrivere sul file e di aggiornare la cache.
        saveSession(updatedSession)

        Log.d(TAG, "resetStartedAdventure: Sessione marcata come non avviata e salvata.")
    }

    fun setStartedAdventure() {
        // 1. Carica la sessione corrente (usando la logica di cache esistente).
        val currentSession = loadSession()

        // 2. Se l'avventura è già avviata, non facciamo nulla per evitare salvataggi inutili.
        if (currentSession.isStarted) {
            Log.d(TAG, "setStartedAdventure: L'avventura è già marcata come avviata.")
            return
        }

        // 3. Crea una copia della sessione con il flag 'isStarted' impostato a true.
        val updatedSession = currentSession.copy(isStarted = true)

        // 4. Usa il metodo saveSession esistente per salvare i dati aggiornati.
        // Questo si occuperà di scrivere sul file e di aggiornare la cache.
        saveSession(updatedSession)

        Log.d(TAG, "setStartedAdventure: Sessione marcata come avviata e salvata.")
    }

    /**
     * Elimina il file di salvataggio e invalida la cache.
     * La prossima chiamata a loadSession() creerà una nuova sessione di default.
     */
    fun deleteSession() :Boolean {
        try {
            // --- INVALIDAZIONE CACHE ---
            // Questo è un passaggio cruciale. Se eliminiamo il salvataggio, dobbiamo
            // anche eliminare la versione in memoria, altrimenti la prossima chiamata a
            // loadSession() restituirebbe dati vecchi.
            cachedSession = null
            if (saveFile.exists()) {
                saveFile.delete()
            }else{
                Log.e(TAG, "loadSession: file not exists! ${saveFile.absolutePath}")
                return false
            }
            return true
        }catch (e : Exception){
            Log.e(TAG, "loadSession: file not exists! ${saveFile.absolutePath}",e)
            return false
        }

    }

    /**
     * Crea un oggetto SessionData con i valori iniziali del gioco.
     * È privato perché viene usato solo internamente.
     */
    private fun createDefaultSession(): SessionData {
        Log.d(TAG, "Creazione di una nuova sessione di default.")
        val hero = GameCharacter(
            id = CharacterID.HERO,
            name = "Lupo Solitario",
            type = CharacterType.PLAYER,
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

    // --- COMPANION OBJECT PER IL SINGLETON (invariato) ---
    companion object {
        @Volatile
        private var INSTANCE: GameStateManager? = null

        fun getInstance(context: android.content.Context): GameStateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GameStateManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}