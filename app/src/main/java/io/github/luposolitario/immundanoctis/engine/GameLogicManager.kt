package io.github.luposolitario.immundanoctis.engine

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.github.luposolitario.immundanoctis.data.Genre
import io.github.luposolitario.immundanoctis.data.Scene
import io.github.luposolitario.immundanoctis.data.SceneType
import io.github.luposolitario.immundanoctis.data.ScenesWrapper
import io.github.luposolitario.immundanoctis.util.SavePreferences
import io.github.luposolitario.immundanoctis.util.getAppSpecificDirectory
import java.io.FileInputStream
import java.io.InputStream
import java.util.Collections
import kotlin.random.Random

/**
 * Gestisce la logica centrale relativa alle scene di gioco.
 * Include il caricamento, la selezione casuale di scene iniziali e il tracciamento delle scene usate.
 * Ora è responsabile anche del caricamento delle scene da assets.
 */
class GameLogicManager(private val context: Context) {

    private val savePreferences by lazy { SavePreferences(context) }
    private val tag = "GameLogicManager"
    private var allScenes: List<Scene> = emptyList()
    private val usedScenesInSession: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    init {
        loadAllScenes()
    }

    private fun loadAllScenes() {
        var scenesStream: InputStream? = null
        try {
            scenesStream = FileInputStream(savePreferences.scenesPath!!)
            val gson = GsonBuilder().create()
            val type = object : TypeToken<ScenesWrapper>() {}.type
            val wrapper: ScenesWrapper = gson.fromJson(scenesStream.reader(), type)
            allScenes = wrapper.scenes
            Log.d(tag, "Scene di gioco caricate con successo (${allScenes.size} scene).")
        } catch (e: Exception) {
            Log.e(tag, "Errore durante il caricamento delle scene di gioco da scenes.json: ${e.message}", e)
            allScenes = emptyList()
        } finally {
            scenesStream?.close()
        }
    }

    /**
     * Seleziona casualmente una scena di tipo START per un genere specifico.
     * @param genre Il genere desiderato (es. Genre.WESTERN).
     * @return Una Scene casuale di tipo START, o null se non ne vengono trovate.
     */
    fun selectRandomStartScene(genre: Genre): Scene? {
        val startScenes = allScenes.filter {
            it.sceneType == SceneType.START && it.genre == genre
            // Potresti aggiungere anche && !usedScenesInSession.contains(it.id)
            // se non vuoi ripetere scene START nella stessa sessione.
        }

        if (startScenes.isEmpty()) {
            Log.e(tag, "Nessuna scena START trovata per il genere: $genre")
            return null
        }

        val selectedScene = startScenes.random(Random)
        Log.d(tag, "Scena START casuale selezionata: ${selectedScene.id} per genere $genre")
        // Non la aggiungiamo a usedScenesInSession qui, perché viene gestita a livello di ViewModel
        // quando la scena è effettivamente "presentata" al giocatore.
        return selectedScene
    }

    /**
     * Recupera una scena dal suo ID.
     * @param sceneId L'ID della scena da cercare.
     * @return La Scene corrispondente, o null se non trovata.
     */
    fun getSceneById(sceneId: String): Scene? {
        return allScenes.find { it.id == sceneId }
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