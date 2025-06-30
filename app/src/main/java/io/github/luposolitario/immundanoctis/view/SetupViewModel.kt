package io.github.luposolitario.immundanoctis.view

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random
import io.github.luposolitario.immundanoctis.data.SessionData
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.data.LoneWolfStats

// Stato della UI per la schermata di creazione
data class SetupUiState(
    val heroName: String = "Lupo Solitario",
    val combattivita: Int = 0,
    val resistenza: Int = 0,
    val stdfPrompt: String = "",
)

class SetupViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState = _uiState.asStateFlow()

    // Usiamo una MutableStateList per le discipline per integrarla facilmente con Compose
    val selectedDisciplines = mutableStateListOf<String>()

    fun updateHeroName(newName: String) {
        _uiState.update { it.copy(heroName = newName) }
    }

    fun updateStdfPrompt(newPrompt: String) {
        _uiState.update { it.copy(stdfPrompt = newPrompt) }
    }

    fun rollStats() {
        _uiState.update {
            it.copy(
                combattivita = 10 + Random.nextInt(0, 10),
                resistenza = 20 + Random.nextInt(0, 10)
            )
        }
    }


    // In fondo alla classe SetupViewModel

    fun finalizeSessionCreation(defaultSession: SessionData): SessionData {
        val currentState = _uiState.value
        val hero = defaultSession.characters.find { it.id == CharacterID.HERO }!!

        // Crea il nuovo eroe con i dati dalla UI
        val updatedHero = hero.copy(
            name = currentState.heroName,
            stats = LoneWolfStats(
                combattivita = currentState.combattivita,
                resistenza = currentState.resistenza
            ),
            kaiDisciplines = selectedDisciplines.toList()
            // In futuro, qui aggiorneremo anche il portraitResId con l'ID dell'immagine generata
        )

        // Aggiorna la lista dei personaggi
        val updatedCharacters = defaultSession.characters.map {
            if (it.id == CharacterID.HERO) updatedHero else it
        }

        // Ritorna la nuova SessionData pronta per essere salvata
        return defaultSession.copy(
            sessionName = "Avventura di ${currentState.heroName}", // Nome campagna dinamico
            lastUpdate = System.currentTimeMillis(),
            characters = updatedCharacters,
            isStarted = false, // La narrazione inizierà nella AdventureActivity
            usedScenes = mutableListOf() // Inizia con le scene usate vuote
        )
    }


    fun toggleDiscipline(disciplineId: String) {
        if (selectedDisciplines.contains(disciplineId)) {
            selectedDisciplines.remove(disciplineId)
        } else if (selectedDisciplines.size < 5) {
            selectedDisciplines.add(disciplineId)
        }
    }
}