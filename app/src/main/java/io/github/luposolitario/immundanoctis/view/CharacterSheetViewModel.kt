// File: app/src/main/java/io/github/luposolitario/immundanoctis/view/CharacterSheetViewModel.kt
package io.github.luposolitario.immundanoctis.view

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.GameItem
import io.github.luposolitario.immundanoctis.data.ItemType
import io.github.luposolitario.immundanoctis.data.LoneWolfStats
import io.github.luposolitario.immundanoctis.util.GameStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Stato della UI per la Scheda Personaggio
data class CharacterSheetUiState(
    val heroCharacter: GameCharacter? = null,
    val combatSkill: Int = 0,
    val endurance: Int = 0,
    val mealsCount: Int = 0,
    val goldCount: Int = 0,
    val equippedWeapon: GameItem? = null, // L'arma attiva/equipaggiata
    val otherWeapons: List<GameItem> = emptyList(), // Le altre armi nell'inventario
    val specialItems: List<GameItem> = emptyList(), // Oggetti speciali (non zaino)
    val backpackItems: List<GameItem> = emptyList(), // Oggetti nello zaino
    val kaiRank: String = ""
)

class CharacterSheetViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "CharacterSheetViewModel"
    private val gameStateManager = GameStateManager(application)

    private val _uiState = MutableStateFlow(CharacterSheetUiState())
    val uiState: StateFlow<CharacterSheetUiState> = _uiState.asStateFlow()

    init {
        // Carica i dati del personaggio all'inizializzazione del ViewModel
        loadCharacterData()
    }

    private fun loadCharacterData() {
        viewModelScope.launch {
            val session = gameStateManager.loadSession()
            val hero = session?.characters?.find { it.id == CharacterID.HERO }

            if (hero == null) {
                Log.e(tag, "Eroe non trovato nella sessione di gioco.")
                // Potresti voler gestire uno stato di errore qui nella UI
                return@launch
            }

            // Calcolo del rango Kai (da GameRulesEngine)
            // Per ora lo mettiamo qui, ma in un ViewModel più complesso si userebbe GameRulesEngine.
            val currentKaiRank = when (hero.kaiDisciplines.size) {
                in 0..4 -> "Novizio Kai"
                5 -> "Iniziato Kai"
                6 -> "Discepolo Kai"
                7 -> "Viandante Kai"
                8 -> "Guerriero Kai"
                9 -> "Maestro Kai"
                10 -> "Gran Maestro Kai"
                else -> "Gran Maestro Kai Supremo"
            }

            // Categorizzazione degli oggetti dell'inventario
            val inventory = hero.details?.inventory ?: emptyList()
            var meals = 0
            var gold = 0
            var equippedWeapon: GameItem? = null
            val otherWeapons = mutableListOf<GameItem>()
            val specialItems = mutableListOf<GameItem>()
            val backpackItems = mutableListOf<GameItem>()

            for (item in inventory) {
                when (item.type) {
                    ItemType.MEAL -> meals += item.quantity // Assumendo che ItemType.MEAL esista o sia gestito da BACKPACK_ITEM
                    ItemType.GOLD -> gold += item.quantity
                    ItemType.WEAPON -> {
                        // Per ora, assumiamo la prima arma nell'inventario come equipaggiata
                        // In futuro, avremo una proprietà specifica per l'arma equipaggiata
                        if (equippedWeapon == null) {
                            equippedWeapon = item
                        } else {
                            otherWeapons.add(item)
                        }
                    }
                    ItemType.HELMET, ItemType.ARMOR, ItemType.SHIELD, ItemType.SPECIAL_ITEM -> {
                        specialItems.add(item)
                    }
                    ItemType.BACKPACK_ITEM -> {
                        if (item.name == "Pasto") { // Gestione specifica per i pasti se non c'è ItemType.MEAL
                            meals += item.quantity
                        } else {
                            backpackItems.add(item)
                        }
                    }
                }
            }
            // Assicuriamoci che i pasti siano sempre il conteggio totale e non un item nello zaino individuale dopo questa categorizzazione
            // Se i pasti sono solo un contatore, potresti voler filtrare l'item "Pasto" dalla lista backpackItems
            val finalBackpackItems = backpackItems.filter { it.name != "Pasto" }.toMutableList()
            // Se il Conteggio Pasti è una somma, assicurati di non aggiungere GameItem("Pasto") alla lista normale dello zaino

            _uiState.update { currentState ->
                currentState.copy(
                    heroCharacter = hero,
                    combatSkill = hero.stats?.combattivita ?: 0,
                    endurance = hero.stats?.resistenza ?: 0,
                    mealsCount = meals,
                    goldCount = gold,
                    equippedWeapon = equippedWeapon,
                    otherWeapons = otherWeapons,
                    specialItems = specialItems,
                    backpackItems = finalBackpackItems,
                    kaiRank = currentKaiRank
                )
            }
            Log.d(tag, "Dati personaggio caricati in CharacterSheetViewModel.")
        }
    }

    // --- Metodi futuri per l'interazione ---
    // fun equipWeapon(weapon: GameItem) { /* ... */ }
    // fun useItem(item: GameItem) { /* ... */ }
    // fun updateNotes(newNotes: String) { /* ... */ }
}