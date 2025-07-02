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
import io.github.luposolitario.immundanoctis.data.KAI_DISCIPLINES // Import KAI_DISCIPLINES
import io.github.luposolitario.immundanoctis.data.KaiDisciplineInfo // Import KaiDisciplineInfo
import io.github.luposolitario.immundanoctis.engine.rules.LoneWolfRules // Import LoneWolfRules per getKaiRank
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
    val kaiDisciplines: List<KaiDisciplineInfo> = emptyList(), // Aggiunto per le discipline complete
    val kaiRank: String = ""
)

class CharacterSheetViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "CharacterSheetViewModel"
    private val gameStateManager = GameStateManager(application)
    private val gameRules = LoneWolfRules() // Istanzia le regole per ottenere il rango Kai

    private val _uiState = MutableStateFlow(CharacterSheetUiState())
    val uiState: StateFlow<CharacterSheetUiState> = _uiState.asStateFlow()

    init {
        loadCharacterData()
    }

    private fun loadCharacterData() {
        viewModelScope.launch {
            val session = gameStateManager.loadSession()
            val hero = session?.characters?.find { it.id == CharacterID.HERO }

            if (hero == null) {
                Log.e(tag, "Eroe non trovato nella sessione di gioco.")
                return@launch
            }

            // Calcolo del rango Kai usando le regole del gioco
            val currentKaiRank = gameRules.getKaiRank(hero.kaiDisciplines.size)

            // Mappatura delle stringhe delle discipline a oggetti KaiDisciplineInfo completi
            val heroKaiDisciplines = hero.kaiDisciplines.mapNotNull { disciplineId ->
                KAI_DISCIPLINES.find { it.id == disciplineId }
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
                    ItemType.GOLD -> gold += item.quantity
                    ItemType.WEAPON -> {
                        // Per ora, assumiamo la prima arma nell'inventario come equipaggiata
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
                        if (item.name == "Pasto") {
                            meals += item.quantity
                        } else {
                            backpackItems.add(item)
                        }
                    }
                    ItemType.MEAL -> meals += item.quantity // Gestisce anche ItemType.MEAL se definito
                }
            }
            val finalBackpackItems = backpackItems.filter { it.name != "Pasto" }.toMutableList()

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
                    kaiDisciplines = heroKaiDisciplines, // Assegna la lista di KaiDisciplineInfo
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