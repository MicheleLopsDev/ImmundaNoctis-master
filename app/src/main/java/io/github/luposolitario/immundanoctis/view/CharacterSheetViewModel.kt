// immundanoctis/view/CharacterSheetViewModel.kt

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
import io.github.luposolitario.immundanoctis.data.StatModifier
import io.github.luposolitario.immundanoctis.data.ModifierSourceType
import io.github.luposolitario.immundanoctis.data.ModifierDuration
import io.github.luposolitario.immundanoctis.engine.rules.LoneWolfRules
import io.github.luposolitario.immundanoctis.util.GameStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// Stato della UI per la Scheda Personaggio
data class CharacterSheetUiState(
    val heroCharacter: GameCharacter? = null,
    val baseCombatSkill: Int = 0,
    val effectiveCombatSkill: Int = 0,
    val baseEndurance: Int = 0,
    val effectiveEndurance: Int = 0,
    val mealsCount: Int = 0,
    val goldCount: Int = 0,
    val visibleWeapons: List<GameItem> = listOf(FISTS_WEAPON, FISTS_WEAPON),
    val specialItems: List<GameItem> = emptyList(),
    val backpackItems: List<GameItem> = emptyList(),
    val kaiDisciplines: List<KaiDisciplineInfo> = emptyList(),
    val kaiRank: String = "",
    val selectedWeapon: GameItem = FISTS_WEAPON,
    val activeModifiers: List<StatModifier> = emptyList()
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

            val currentVisibleWeapons = mutableListOf<GameItem>()
            if (actualWeapons.isNotEmpty()) {
                currentVisibleWeapons.add(actualWeapons[0])
                if (actualWeapons.size > 1) {
                    currentVisibleWeapons.add(actualWeapons[1])
                }
            }
            while (currentVisibleWeapons.size < 2) {
                currentVisibleWeapons.add(FISTS_WEAPON)
            }

            for (item in inventory) {
                when (item.type) {
                    ItemType.GOLD -> gold += item.quantity
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
                    ItemType.WEAPON -> { /* Already handled */ }
                }
            }
            val finalBackpackItems = backpackItems.filter { it.name != "Pasto" }.toMutableList()

            val initialSelectedWeapon = currentVisibleWeapons.firstOrNull { it.id != FISTS_WEAPON.id } ?: FISTS_WEAPON

            val baseCS = hero.stats?.combattivita ?: 0
            val baseEND = hero.stats?.resistenza ?: 0
            val initialActiveModifiers = hero.details?.activeModifiers?.toMutableList() ?: mutableListOf()

            // Assicurati che l'arma selezionata sia tra i modificatori attivi (se ha un bonus)
            // Rimuovi eventuali modificatori di armi precedenti e aggiungi quello dell'arma selezionata
            // Questa logica verrà centralizzata in selectWeapon, ma la teniamo qui per il setup iniziale.
            initialActiveModifiers.removeAll { it.sourceType == ModifierSourceType.ITEM && it.statName == "COMBATTIVITA" && it.sourceId != FISTS_WEAPON.id }
            if (initialSelectedWeapon.combatSkillBonus != 0) {
                initialActiveModifiers.add(
                    StatModifier(
                        id = "weapon_bonus_${initialSelectedWeapon.id}", // ID specifico per l'arma
                        statName = "COMBATTIVITA",
                        amount = initialSelectedWeapon.combatSkillBonus,
                        sourceType = ModifierSourceType.ITEM,
                        sourceId = initialSelectedWeapon.id,
                        duration = ModifierDuration.UNTIL_UNEQUIPPED
                    )
                )
            }
            // NUOVO: Applica la penalità per i pugni se sono l'arma iniziale
            if (initialSelectedWeapon.id == FISTS_WEAPON.id) {
                initialActiveModifiers.add(
                    StatModifier(
                        id = "rule_no_weapon_penalty", // ID unico per questa regola
                        statName = "COMBATTIVITA",
                        amount = -4, // Penalità di -4
                        sourceType = ModifierSourceType.RULE, // Nuovo tipo di fonte, da aggiungere se non lo è già
                        sourceId = "no_weapon",
                        duration = ModifierDuration.UNTIL_UNEQUIPPED
                    )
                )
            }

            // Salva i modificatori aggiornati nell'eroe prima di usarli per il calcolo UI
            val updatedHeroDetails = hero.details?.copy(activeModifiers = initialActiveModifiers)
            val updatedHeroWithModifiers = hero.copy(details = updatedHeroDetails)

            val effectiveCS = calculateEffectiveCombatSkill(baseCS, initialActiveModifiers)
            val effectiveEND = calculateEffectiveEndurance(baseEND, initialActiveModifiers)

            _uiState.update { currentState ->
                currentState.copy(
                    heroCharacter = updatedHeroWithModifiers, // Aggiorna l'eroe con i modificatori salvati
                    baseCombatSkill = baseCS,
                    effectiveCombatSkill = effectiveCS,
                    baseEndurance = baseEND,
                    effectiveEndurance = effectiveEND,
                    mealsCount = meals,
                    goldCount = gold,
                    visibleWeapons = currentVisibleWeapons,
                    specialItems = specialItems,
                    backpackItems = finalBackpackItems,
                    kaiDisciplines = heroKaiDisciplines,
                    kaiRank = currentKaiRank,
                    selectedWeapon = initialSelectedWeapon,
                    activeModifiers = initialActiveModifiers
                )
            }
            Log.d(tag, "Dati personaggio caricati in CharacterSheetViewModel. CS Effettiva: $effectiveCS, END Effettiva: $effectiveEND.")
            Log.d(tag, "Modificatori attivi iniziali: ${initialActiveModifiers.map { "${it.statName}: ${it.amount}" }}")
        }
    }

    private fun calculateEffectiveCombatSkill(baseCS: Int, modifiers: List<StatModifier>): Int {
        var effectiveCS = baseCS
        modifiers.forEach { modifier ->
            if (modifier.statName == "COMBATTIVITA") {
                effectiveCS += modifier.amount
            }
        }
        return effectiveCS
    }

    private fun calculateEffectiveEndurance(baseEND: Int, modifiers: List<StatModifier>): Int {
        var effectiveEND = baseEND
        modifiers.forEach { modifier ->
            if (modifier.statName == "RESISTENZA") {
                effectiveEND += modifier.amount
            }
        }
        return effectiveEND
    }

    // --- selectWeapon MODIFICATA per gestire i modificatori delle armi e della penalità "no weapon" ---
    fun selectWeapon(weapon: GameItem?) {
        viewModelScope.launch {
            val session = gameStateManager.loadSession() ?: return@launch
            val hero = session.characters.find { it.id == CharacterID.HERO } ?: return@launch

            val newSelectedWeapon = weapon ?: FISTS_WEAPON
            val currentModifiers = hero.details?.activeModifiers?.toMutableList() ?: mutableListOf()

            // Rimuovi TUTTI i modificatori di Combattività da ITEM (inclusi bonus arma e penalità pugni)
            currentModifiers.removeAll { it.sourceType == ModifierSourceType.ITEM && it.statName == "COMBATTIVITA" }
            // Rimuovi anche la penalità specifica della regola "no weapon" se presente
            currentModifiers.removeAll { it.id == "rule_no_weapon_penalty" }


            // Applica il bonus della nuova arma selezionata
            if (newSelectedWeapon.combatSkillBonus != 0) {
                currentModifiers.add(
                    StatModifier(
                        id = "weapon_bonus_${newSelectedWeapon.id}",
                        statName = "COMBATTIVITA",
                        amount = newSelectedWeapon.combatSkillBonus,
                        sourceType = ModifierSourceType.ITEM,
                        sourceId = newSelectedWeapon.id,
                        duration = ModifierDuration.UNTIL_UNEQUIPPED
                    )
                )
            }

            // NUOVO: Applica la penalità per i pugni se sono l'arma selezionata
            if (newSelectedWeapon.id == FISTS_WEAPON.id) {
                currentModifiers.add(
                    StatModifier(
                        id = "rule_no_weapon_penalty", // ID unico per questa regola
                        statName = "COMBATTIVITA",
                        amount = -4, // Penalità di -4
                        sourceType = ModifierSourceType.RULE, // Indica che la fonte è una regola di gioco
                        sourceId = "no_weapon", // ID specifico per questa regola
                        duration = ModifierDuration.PERMANENT // Dura finché si hanno i pugni selezionati
                    )
                )
            }


            // Aggiorna i dettagli dell'eroe con la nuova lista di modificatori
            val updatedHeroDetails = hero.details?.copy(activeModifiers = currentModifiers)
            val updatedHero = hero.copy(details = updatedHeroDetails)

            val updatedCharacters = session.characters.map {
                if (it.id == CharacterID.HERO) updatedHero else it
            }
            val updatedSession = session.copy(characters = updatedCharacters)
            gameStateManager.saveSession(updatedSession)

            // Aggiorna lo stato UI con le nuove statistiche effettive
            val baseCS = updatedHero.stats?.combattivita ?: 0
            val baseEND = updatedHero.stats?.resistenza ?: 0
            val effectiveCS = calculateEffectiveCombatSkill(baseCS, currentModifiers)
            val effectiveEND = calculateEffectiveEndurance(baseEND, currentModifiers)

            _uiState.update { currentState ->
                currentState.copy(
                    selectedWeapon = newSelectedWeapon,
                    activeModifiers = currentModifiers,
                    effectiveCombatSkill = effectiveCS,
                    effectiveEndurance = effectiveEND
                )
            }
            Log.d(tag, "Arma selezionata: ${newSelectedWeapon.name}. CS Effettiva: $effectiveCS.")
            Log.d(tag, "Modificatori attivi dopo selectWeapon: ${currentModifiers.map { "${it.statName}: ${it.amount} (Source: ${it.sourceType}, ID: ${it.sourceId})" }}")
        }
    }

    // --- useBackpackItem MODIFICATA per usare effectiveEndurance e aggiornare i modificatori ---
    fun useBackpackItem(item: GameItem) {
        if (!item.isConsumable) {
            Log.d(tag, "L'oggetto '${item.name}' non può essere usato direttamente tramite click.")
            return
        }

        if (item.quantity <= 0) {
            Log.w(tag, "Tentativo di usare un oggetto con quantità 0: ${item.name}")
            return
        }

        viewModelScope.launch {
            val session = gameStateManager.loadSession() ?: return@launch
            val hero = session.characters.find { it.id == CharacterID.HERO } ?: return@launch

            val updatedInventory = hero.details?.inventory?.toMutableList() ?: mutableListOf()
            val currentModifiers = hero.details?.activeModifiers?.toMutableList() ?: mutableListOf()

            var updatedBaseEndurance = hero.stats?.resistenza ?: 0
            val maxEndurance = hero.stats?.resistenza ?: 0

            val itemToModify = updatedInventory.find { it.id == item.id }

            if (itemToModify != null) {
                when (itemToModify.name) {
                    "Pozione Curativa" -> {
                        val healingAmount = 4
                        updatedBaseEndurance = (updatedBaseEndurance + healingAmount).coerceAtMost(maxEndurance)
                        itemToModify.quantity--
                        Log.d(tag, "Usata Pozione Curativa. Resistenza Base: $updatedBaseEndurance. Quantità: ${itemToModify.quantity}")
                    }
                    else -> {
                        Log.e(tag, "Errore logico: Oggetto '${item.name}' è isConsumable ma senza logica di consumo specifica.")
                        return@launch
                    }
                }

                if (itemToModify.quantity <= 0) {
                    updatedInventory.remove(itemToModify)
                    Log.d(tag, "Oggetto '${item.name}' rimosso dall'inventario (quantità 0).")
                }

                // Aggiorna i dettagli dell'eroe e la sessione
                val updatedHeroStats = hero.stats?.copy(resistenza = updatedBaseEndurance)
                val updatedHeroDetails = hero.details?.copy(inventory = updatedInventory, activeModifiers = currentModifiers)
                val updatedHero = hero.copy(details = updatedHeroDetails, stats = updatedHeroStats)

                val updatedCharacters = session.characters.map {
                    if (it.id == CharacterID.HERO) updatedHero else it
                }
                val updatedSession = session.copy(characters = updatedCharacters)
                gameStateManager.saveSession(updatedSession)

                // Aggiorna lo stato UI con le nuove statistiche effettive
                val baseCS = updatedHero.stats?.combattivita ?: 0
                val effectiveCS = calculateEffectiveCombatSkill(baseCS, currentModifiers)
                val effectiveEND = calculateEffectiveEndurance(updatedBaseEndurance, currentModifiers)

                _uiState.update { currentState ->
                    currentState.copy(
                        heroCharacter = updatedHero,
                        baseCombatSkill = updatedHero.stats?.combattivita ?: 0,
                        effectiveCombatSkill = effectiveCS,
                        baseEndurance = updatedBaseEndurance,
                        effectiveEndurance = effectiveEND,
                        mealsCount = updatedInventory.find { it.name == "Pasto" }?.quantity ?: 0,
                        backpackItems = updatedInventory.filter { it.type == ItemType.BACKPACK_ITEM && it.name != "Pasto" },
                        activeModifiers = currentModifiers
                    )
                }
                Log.d(tag, "Inventario e statistiche dell'eroe aggiornati dopo l'uso dell'oggetto: ${item.name}. END Effettiva: $effectiveEND.")
            } else {
                Log.w(tag, "Oggetto '${item.name}' non trovato nell'inventario per l'uso (dopo il filtro isConsumable).")
            }
        }
    }

    // --- discardItem MODIFICATA per aggiornare i modificatori ---
    fun discardItem(item: GameItem) {
        if (!item.isDiscardable) {
            Log.w(tag, "Tentativo di scartare un oggetto non scartabile: ${item.name}")
            return
        }

        if (item.quantity <= 0) {
            Log.w(tag, "Tentativo di scartare un oggetto con quantità 0: ${item.name}")
            return
        }

        viewModelScope.launch {
            val session = gameStateManager.loadSession() ?: return@launch
            val hero = session.characters.find { it.id == CharacterID.HERO } ?: return@launch

            val updatedInventory = hero.details?.inventory?.toMutableList() ?: mutableListOf()
            val currentModifiers = hero.details?.activeModifiers?.toMutableList() ?: mutableListOf()

            val itemToDiscard = updatedInventory.find { it.id == item.id }

            if (itemToDiscard != null) {
                // Decrementa la quantità o rimuovi completamente l'oggetto
                if (itemToDiscard.quantity > 1) {
                    itemToDiscard.quantity--
                    Log.d(tag, "Scartata 1 unità di '${itemToDiscard.name}'. Quantità rimanente: ${itemToDiscard.quantity}")
                } else {
                    updatedInventory.remove(itemToDiscard)
                    Log.d(tag, "Oggetto '${item.name}' rimosso dall'inventario.")
                }

                // Rimuovi tutti i modificatori associati all'oggetto scartato
                currentModifiers.removeAll { it.sourceId == item.id && it.sourceType == ModifierSourceType.ITEM }
                Log.d(tag, "Rimossi modificatori associati all'oggetto scartato: ${item.name}. Modificatori attivi rimanenti: ${currentModifiers.size}")

                // Se l'oggetto scartato era un'arma selezionata, seleziona i pugni
                // La chiamata a selectWeapon gestirà l'aggiornamento dei modificatori
                if (item.type == ItemType.WEAPON && _uiState.value.selectedWeapon.id == item.id) {
                    selectWeapon(FISTS_WEAPON)
                    Log.d(tag, "Arma selezionata '${item.name}' scartata. Selezionati i Pugni.")
                } else {
                    // Se non è un'arma selezionata, aggiorna comunque la sessione e la UI State
                    val updatedHeroDetails = hero.details?.copy(activeModifiers = currentModifiers, inventory = updatedInventory)
                    val updatedHero = hero.copy(details = updatedHeroDetails)

                    val updatedCharacters = session.characters.map {
                        if (it.id == CharacterID.HERO) updatedHero else it
                    }
                    val updatedSession = session.copy(characters = updatedCharacters)
                    gameStateManager.saveSession(updatedSession)

                    val baseCS = updatedHero.stats?.combattivita ?: 0
                    val baseEND = updatedHero.stats?.resistenza ?: 0
                    val effectiveCS = calculateEffectiveCombatSkill(baseCS, currentModifiers)
                    val effectiveEND = calculateEffectiveEndurance(baseEND, currentModifiers)

                    _uiState.update { currentState ->
                        currentState.copy(
                            heroCharacter = updatedHero,
                            backpackItems = updatedInventory.filter { it.type == ItemType.BACKPACK_ITEM && it.name != "Pasto" },
                            visibleWeapons = updatedInventory.filter { it.type == ItemType.WEAPON }.toMutableList().apply {
                                while (size < 2) add(FISTS_WEAPON)
                            },
                            specialItems = updatedInventory.filter { it.type == ItemType.SPECIAL_ITEM || it.type == ItemType.HELMET || it.type == ItemType.ARMOR },
                            activeModifiers = currentModifiers,
                            effectiveCombatSkill = effectiveCS,
                            effectiveEndurance = effectiveEND
                        )
                    }
                    Log.d(tag, "Oggetto '${item.name}' scartato con successo. Inventario e modificatori aggiornati.")
                }
            } else {
                Log.w(tag, "Oggetto '${item.name}' non trovato nell'inventario per lo scarto.")
            }
        }
    }

    // --- addWeapon MODIFICATA per aggiornare i modificatori ---
    fun addWeapon(newWeapon: GameItem) {
        viewModelScope.launch {
            val session = gameStateManager.loadSession() ?: return@launch
            val hero = session.characters.find { it.id == CharacterID.HERO } ?: return@launch

            val updatedInventory = hero.details?.inventory?.toMutableList() ?: mutableListOf()
            val currentModifiers = hero.details?.activeModifiers?.toMutableList() ?: mutableListOf()

            val currentRealWeapons = updatedInventory.filter { it.type == ItemType.WEAPON && it.id != FISTS_WEAPON.id }.toMutableList()

            if (currentRealWeapons.size < 2) {
                updatedInventory.add(newWeapon)
                Log.d(tag, "Aggiungo ${newWeapon.name} all'inventario.")
            } else {
                val weaponToReplace = if (currentRealWeapons.size == 2 && _uiState.value.selectedWeapon.id == currentRealWeapons[0].id) {
                    currentRealWeapons[1]
                } else if (currentRealWeapons.size == 2 && _uiState.value.selectedWeapon.id == currentRealWeapons[1].id) {
                    currentRealWeapons[0]
                } else {
                    currentRealWeapons.firstOrNull()
                }

                if (weaponToReplace != null) {
                    updatedInventory.remove(weaponToReplace)
                    currentModifiers.removeAll { it.sourceId == weaponToReplace.id && it.sourceType == ModifierSourceType.ITEM }
                    Log.d(tag, "Sostituisco ${weaponToReplace.name} con ${newWeapon.name} nell'inventario.")
                } else {
                    Log.w(tag, "Impossibile trovare un'arma da sostituire. Aggiungo comunque la nuova arma.")
                }
                updatedInventory.add(newWeapon)
            }

            // Salva la sessione aggiornata con il nuovo inventario e modificatori
            val updatedHeroDetails = hero.details?.copy(inventory = updatedInventory, activeModifiers = currentModifiers)
            val updatedHero = hero.copy(details = updatedHeroDetails)
            val updatedCharacters = session.characters.map {
                if (it.id == CharacterID.HERO) updatedHero else it
            }
            val updatedSession = session.copy(characters = updatedCharacters)
            gameStateManager.saveSession(updatedSession)

            // Se la nuova arma ha un bonus, assicurati che sia selezionata per applicare il bonus
            // La chiamata a selectWeapon aggiorna anche i modificatori e la UI
            if (newWeapon.combatSkillBonus != 0) {
                selectWeapon(newWeapon)
            } else {
                // Se la nuova arma non ha bonus o non è selezionata, ricarica semplicemente i dati per aggiornare la UI
                // Questo è necessario perché l'inventario è stato modificato
                loadCharacterData()
            }
        }
    }
}