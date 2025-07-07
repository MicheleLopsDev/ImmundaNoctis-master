package io.github.luposolitario.immundanoctis.engine

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.luposolitario.immundanoctis.data.*
import io.github.luposolitario.immundanoctis.util.SavePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.InputStream
import java.util.Collections
import kotlin.random.Random

/**
 * Gestisce il caricamento e l'accesso alle scene del gioco da un file JSON.
 * Utilizza un pattern Singleton per garantire che i dati delle scene siano caricati una sola volta.
 */
object GameLogicManager {
    private val tag = "GameLogicManager"
    private var _adventureName: String = "" // Nuovo campo per memorizzare il nome dell'avventura
    val adventureName: String get() = _adventureName  // Esposizione pubblica del nome
    // Variabili per la cache
    private var scenesCache: List<Scene> = emptyList()

    private val usedScenesInSession: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    private var currentLoadedPath: String? = null

    /**
     * Carica tutte le scene dal file JSON specificato nelle preferenze.
     * Esegue il parsing solo se il file non è già stato caricato in memoria.
     */
    suspend fun loadAllScenes(context: Context) = withContext(Dispatchers.IO) {

        val scenesPath = SavePreferences(context).scenesPath ?: return@withContext
        val inputStream: InputStream?  = null

            // GUARDIA DI CONTROLLO: Se il file richiesto è già in cache, non fare nulla.
        if (scenesPath == currentLoadedPath && scenesCache.isNotEmpty()) {
            Log.d("GameLogicManager", "Scene da '$scenesPath' già presenti in cache. Salto parsing.")
            return@withContext
        }

        Log.d("GameLogicManager", "Inizio caricamento e parsing da '$scenesPath'.")
        try {
            val inputStream = FileInputStream(scenesPath)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val gson = Gson()
            val type = object : TypeToken<ScenesWrapper>() {}.type

            // Esegui il parsing solo se necessario
            val wrapper: ScenesWrapper = gson.fromJson(jsonString, type)

            // Salva i dati parsati nella cache
            _adventureName = wrapper.adventureName ?: "Avventura Caricata" // <-- Gestione del null qui, con un fallback
            scenesCache = wrapper.scenes
            currentLoadedPath = scenesPath // Aggiorna il percorso del file in cache

            Log.d("GameLogicManager", "Parsing completato. Avventura: '${wrapper.adventureName}'. Trovate ${scenesCache.size} scene.")
        } catch (e: Exception) {
            Log.e("GameLogicManager", "Errore durante il caricamento delle scene da '$scenesPath'", e)
            // Pulisci la cache in caso di errore
            _adventureName = "Avventura Sconosciuta" // Fallback in caso di errore di caricamento
            scenesCache = emptyList()
            currentLoadedPath = null
        }finally {
            inputStream?.close()
        }
    }

    /**
     * Forza il ricaricamento delle scene dal file, invalidando la cache attuale.
     * Da usare solo quando l'utente cambia esplicitamente il file delle scene.
     */
    suspend fun forceReloadScenesFromFile(context: Context) {
        Log.d("GameLogicManager", "Forzatura ricaricamento scene.")
        // Invalida la cache
        currentLoadedPath = null
        scenesCache = emptyList()
        // Chiama la logica di caricamento
        loadAllScenes(context)
    }

    fun getSceneById(id: String): Scene? {
        if (scenesCache.isEmpty()) {
            Log.w("GameLogicManager", "Cache delle scene vuota. Chiamare loadAllScenes prima.")
            return null
        }
        return scenesCache.find { it.id == id }
    }

    fun selectRandomStartScene(genre: Genre): Scene? {
        if (scenesCache.isEmpty()) return null
        val startScenes = scenesCache.filter { it.sceneType == SceneType.START  && it.genre == genre }
        return if (startScenes.isNotEmpty()) {
            startScenes[Random.nextInt(startScenes.size)]
        } else {
            scenesCache.firstOrNull()
        }
    }

    /**
     * Aggiunge l'ID di una scena alla lista delle scene usate in questa sessione.
     * @param sceneId L'ID della scena da marcare come usata.
     */
    fun markSceneAsUsed(sceneId: String) {
        usedScenesInSession.add(sceneId)
        Log.d(tag, "Scena marcata come usata: $sceneId. Totale scene usate in sessione: ${usedScenesInSession.size}")
    }

    /**
     * Resetta lo stato delle scene usate per una nuova avventura.
     */
    fun resetUsedScenes() {
        usedScenesInSession.clear()
        Log.d(tag, "Lista scene usate resettata.")
    }


}