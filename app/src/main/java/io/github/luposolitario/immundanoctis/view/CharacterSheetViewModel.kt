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
import io.github.luposolitario.immundanoctis.data.KAI_DISCIPLINES
import io.github.luposolitario.immundanoctis.data.KaiDisciplineInfo
import io.github.luposolitario.immundanoctis.data.FISTS_WEAPON
import io.github.luposolitario.immundanoctis.engine.rules.LoneWolfRules
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
    val visibleWeapons: List<GameItem> = listOf(FISTS_WEAPON, FISTS_WEAPON), // Sempre 2 armi visibili (o pugni)
    val specialItems: List<GameItem> = emptyList(),
    val backpackItems: List<GameItem> = emptyList(),
    val kaiDisciplines: List<KaiDisciplineInfo> = emptyList(),
    val kaiRank: String = "",
    val selectedWeapon: GameItem = FISTS_WEAPON // Arma attualmente selezionata per il combattimento
)

class CharacterSheetViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "CharacterSheetViewModel"
    private val gameStateManager = GameStateManager(application)
    private val gameRules = LoneWolfRules()

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

            val currentKaiRank = gameRules.getKaiRank(hero.kaiDisciplines.size)
            val heroKaiDisciplines = hero.kaiDisciplines.mapNotNull { disciplineId ->
                KAI_DISCIPLINES.find { it.id == disciplineId }
            }

            val inventory = hero.details?.inventory ?: emptyList()
            var meals = 0
            var gold = 0
            val specialItems = mutableListOf<GameItem>()
            val backpackItems = mutableListOf<GameItem>()

            val actualWeapons = inventory.filter { it.type == ItemType.WEAPON }.toMutableList()

            // Assicuriamo che visibleWeapons contenga sempre 2 armi
            val currentVisibleWeapons = mutableListOf<GameItem>()
            if (actualWeapons.isNotEmpty()) {
                currentVisibleWeapons.add(actualWeapons[0])
                if (actualWeapons.size > 1) {
                    currentVisibleWeapons.add(actualWeapons[1])
                }
            }
            // Riempiamo con i pugni se ci sono meno di due armi
            while (currentVisibleWeapons.size < 2) {
                currentVisibleWeapons.add(FISTS_WEAPON)
            }


            for (item in inventory) {
                when (item.type) {
                    ItemType.GOLD -> gold += item.quantity
                    // Armi già gestite sopra in actualWeapons
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
                    ItemType.MEAL -> meals += item.quantity
                    ItemType.WEAPON -> { /* Non fare nulla qui */ } // <--- AGGIUNTA QUESTA RIGA!
                }
            }
            val finalBackpackItems = backpackItems.filter { it.name != "Pasto" }.toMutableList()

            // Determina l'arma inizialmente selezionata: la prima arma vera, o i pugni
            val initialSelectedWeapon = currentVisibleWeapons.firstOrNull { it.id != FISTS_WEAPON.id } ?: FISTS_WEAPON


            _uiState.update { currentState ->
                currentState.copy(
                    heroCharacter = hero,
                    combatSkill = hero.stats?.combattivita ?: 0,
                    endurance = hero.stats?.resistenza ?: 0,
                    mealsCount = meals,
                    goldCount = gold,
                    visibleWeapons = currentVisibleWeapons, // Assegnata la lista di 2 armi/pugni
                    specialItems = specialItems,
                    backpackItems = finalBackpackItems,
                    kaiDisciplines = heroKaiDisciplines,
                    kaiRank = currentKaiRank,
                    selectedWeapon = initialSelectedWeapon
                )
            }
            Log.d(tag, "Dati personaggio caricati in CharacterSheetViewModel.")
        }
    }

    // Funzione per selezionare l'arma attiva
    fun selectWeapon(weapon: GameItem?) {
        _uiState.update { currentState ->
            val newSelectedWeapon = weapon ?: FISTS_WEAPON
            currentState.copy(selectedWeapon = newSelectedWeapon)
        }
        Log.d(tag, "Arma selezionata: ${weapon?.name ?: "Pugni"}")
        // TODO: In futuro, qui dovremmo anche salvare la sessione per persistenza
    }

    // --- NUOVA FUNZIONE: Aggiungi una nuova arma gestendo il limite di 2 armi ---
    fun addWeapon(newWeapon: GameItem) {
        _uiState.update { currentState ->
            val currentWeapons = currentState.visibleWeapons.filter { it.id != FISTS_WEAPON.id }.toMutableList() // Armi reali possedute

            if (currentWeapons.size < 2) {
                // Meno di 2 armi, aggiungi semplicemente la nuova
                currentWeapons.add(newWeapon)
                Log.d(tag, "Aggiungo ${newWeapon.name}. Armi attuali: ${currentWeapons.map { it.name }}")
            } else {
                // Ho già 2 armi reali, devo sostituirne una.
                // Logica di scarto automatica (la meno potente, la non selezionata, etc.)
                // Per ora, come placeholder automatico, sostituisco l'arma NON selezionata.
                val weaponToReplace = if (currentState.selectedWeapon.id == currentWeapons[0].id) {
                    // L'arma selezionata è la prima, sostituisco la seconda
                    currentWeapons[1]
                } else {
                    // L'arma selezionata è la seconda o non è nessuna delle due, sostituisco la prima
                    currentWeapons[0]
                }
                Log.d(tag, "Sostituisco ${weaponToReplace.name} con ${newWeapon.name}.")
                currentWeapons.remove(weaponToReplace)
                currentWeapons.add(newWeapon)

                // Se l'arma scartata era quella selezionata, ora seleziono la nuova
                if (currentState.selectedWeapon.id == weaponToReplace.id) {
                    Log.d(tag, "L'arma scartata era quella selezionata. Seleziono la nuova arma.")
                    selectWeapon(newWeapon) // Chiama la funzione per aggiornare selectedWeapon nello stato
                }
            }

            // Ricostruisco la lista visibleWeapons per la UI (sempre con 2 elementi)
            val updatedVisibleWeapons = currentWeapons.toMutableList()
            while (updatedVisibleWeapons.size < 2) {
                updatedVisibleWeapons.add(FISTS_WEAPON)
            }
            Log.d(tag, "Nuove armi visibili: ${updatedVisibleWeapons.map { it.name }}")

            currentState.copy(visibleWeapons = updatedVisibleWeapons)
        }
    }


    // --- Metodi futuri per l'interazione ---
    // fun useItem(item: GameItem) { /* ... */ }
    // fun updateNotes(newNotes: String) { /* ... */ }
}