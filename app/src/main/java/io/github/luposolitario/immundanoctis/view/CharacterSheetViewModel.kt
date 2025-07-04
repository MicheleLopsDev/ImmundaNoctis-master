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
import io.github.luposolitario.immundanoctis.data.WeaponType
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
    val activeModifiers: List<StatModifier> = emptyList(),
    val selectedWeaponModifierAmount: Int = 0
)

class CharacterSheetViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "CSViewModel" // Abbreviato per visibilità nei log
    private val gameStateManager = GameStateManager(application)
    private val gameRules = LoneWolfRules()

    private val _uiState = MutableStateFlow(CharacterSheetUiState())
    val uiState: StateFlow<CharacterSheetUiState> = _uiState.asStateFlow()


    init {
        Log.d(tag, "ViewModel Inizializzato.")
        // L'init ora chiama semplicemente la funzione pubblica
        loadCharacterData()
    }

    // --- loadCharacterData() REFACTORIZZATA ---
    // --- loadCharacterData() ORA È PUBBLICA ---
    fun loadCharacterData() { // Rimuovi 'private'
        viewModelScope.launch {
            Log.d(tag, "Inizio caricamento dati personaggio...") // Aggiunto log per chiarezza
            val session = gameStateManager.loadSession() ?: run { Log.e(tag, "Sessione non caricata in loadCharacterData."); return@launch }
            val hero = session.characters.find { it.id == CharacterID.HERO } ?: run { Log.e(tag, "Eroe non trovato in loadCharacterData."); return@launch }
            Log.d(tag, "Eroe trovato: ${hero.name}. Discipline: ${hero.kaiDisciplines.joinToString()}. WeaponSkillType: ${hero.details?.weaponSkillType}")
            Log.d(tag, "Inventario iniziale dell'eroe: ${hero.details?.inventory?.map { it.name }}")
            Log.d(tag, "Modificatori attivi dell'eroe caricati dalla sessione: ${hero.details?.activeModifiers?.map { it.id + ":" + it.amount }}")

            val currentKaiRank = gameRules.getKaiRank(hero.kaiDisciplines.size)
            val heroKaiDisciplines = hero.kaiDisciplines.mapNotNull { disciplineId ->
                KAI_DISCIPLINES.find { it.id == disciplineId }
            }.sortedBy { it.id }

            // CORREZIONE: Assicurati che 'inventory' sia MutableList
            val inventory = hero.details?.inventory ?: mutableListOf() // <--- MODIFICA QUI: emptyList() -> mutableListOf()
            var meals = 0
            var gold = 0
            val specialItems = mutableListOf<GameItem>()
            val backpackItems = mutableListOf<GameItem>()

            val actualWeaponsInInventory = inventory.filter { it.type == ItemType.WEAPON }.toMutableList()
            val currentVisibleWeapons = mutableListOf<GameItem>()
            if (actualWeaponsInInventory.isNotEmpty()) {
                currentVisibleWeapons.add(actualWeaponsInInventory[0])
                if (actualWeaponsInInventory.size > 1) {
                    currentVisibleWeapons.add(actualWeaponsInInventory[1])
                }
            }
            while (currentVisibleWeapons.size < 2) {
                currentVisibleWeapons.add(FISTS_WEAPON)
            }
            Log.d(tag, "Armi visibili calcolate (loadCharacterData): ${currentVisibleWeapons.map { it.name }}")

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
            Log.d(tag, "Inventario categorizzato (loadCharacterData). Pasti: $meals, Oro: $gold, Backpack: ${finalBackpackItems.map { it.name }}, Special: ${specialItems.map { it.name }}")

            val initialSelectedWeapon = currentVisibleWeapons.firstOrNull { it.id != FISTS_WEAPON.id } ?: FISTS_WEAPON
            Log.d(tag, "Arma inizialmente selezionata (loadCharacterData, dopo la selezione): ${initialSelectedWeapon.name}")

            val initialActiveModifiers = hero.details?.activeModifiers?.toMutableList() ?: mutableListOf()
            initialActiveModifiers.removeAll { modifier ->
                modifier.statName == "COMBATTIVITA" && (
                        modifier.sourceType == ModifierSourceType.ITEM ||
                                modifier.sourceType == ModifierSourceType.RULE ||
                                (modifier.sourceType == ModifierSourceType.DISCIPLINE && modifier.sourceId == "WEAPONSKILL")
                        )
            }

            var initialWeaponNetCombatModifier = 0
            if (initialSelectedWeapon.id != FISTS_WEAPON.id) {
                if (initialSelectedWeapon.combatSkillBonus != 0) {
                    initialWeaponNetCombatModifier += initialSelectedWeapon.combatSkillBonus
                    initialActiveModifiers.add(
                        StatModifier(
                            id = "weapon_bonus_${initialSelectedWeapon.id}",
                            statName = "COMBATTIVITA",
                            amount = initialSelectedWeapon.combatSkillBonus,
                            sourceType = ModifierSourceType.ITEM,
                            sourceId = initialSelectedWeapon.id,
                            duration = ModifierDuration.UNTIL_UNEQUIPPED
                        )
                    )
                }
            } else { // FISTS_WEAPON
                val hasWeaponSkill = hero.kaiDisciplines.contains("WEAPONSKILL")
                val weaponSkillTypeChosen = hero.details?.weaponSkillType
                if (hasWeaponSkill && weaponSkillTypeChosen == WeaponType.FISTS) {
                    initialWeaponNetCombatModifier += 0
                } else {
                    initialWeaponNetCombatModifier += -4
                }
            }

            val updatedHeroDetails = hero.details?.copy(activeModifiers = initialActiveModifiers, inventory = inventory)
            val updatedHero = hero.copy(details = updatedHeroDetails)

            val baseCS = updatedHero.stats?.combattivita ?: 0
            val baseEND = updatedHero.stats?.resistenza ?: 0
            val effectiveCS = calculateEffectiveCombatSkill(baseCS, initialActiveModifiers)
            val effectiveEND = calculateEffectiveEndurance(baseEND, initialActiveModifiers)

            _uiState.update { currentState ->
                currentState.copy(
                    heroCharacter = updatedHero,
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
                    activeModifiers = initialActiveModifiers,
                    selectedWeaponModifierAmount = initialWeaponNetCombatModifier
                )
            }
            Log.d(tag, "loadCharacterData() completata. UI State impostata. CS Effettiva: $effectiveCS, END Effettiva: $effectiveEND, Modificatore Arma Selezionata: $initialWeaponNetCombatModifier")
            Log.d(tag, "Modificatori attivi finali (loadCharacterData): ${initialActiveModifiers.map { "${it.statName}: ${it.amount} (Source: ${it.sourceType}, ID: ${it.sourceId})" }}")
        }
    }

    private fun calculateEffectiveCombatSkill(baseCS: Int, modifiers: List<StatModifier>): Int {
        var effectiveCS = baseCS
        modifiers.forEach { modifier ->
            if (modifier.statName == "COMBATTIVITA") {
                effectiveCS += modifier.amount
            }
        }
        Log.d(tag, "Calcolo Effettivo CS: Base=$baseCS, Modificatori=${modifiers.filter { it.statName == "COMBATTIVITA" }.map { it.amount }}, Effettivo=$effectiveCS")
        return effectiveCS
    }

    private fun calculateEffectiveEndurance(baseEND: Int, modifiers: List<StatModifier>): Int {
        var effectiveEND = baseEND
        modifiers.forEach { modifier ->
            if (modifier.statName == "RESISTENZA") {
                effectiveEND += modifier.amount
            }
        }
        Log.d(tag, "Calcolo Effettivo END: Base=$baseEND, Modificatori=${modifiers.filter { it.statName == "RESISTENZA" }.map { it.amount }}, Effettivo=$effectiveEND")
        return effectiveEND
    }

    // --- selectWeapon MODIFICATA ---
    fun selectWeapon(weapon: GameItem?) {
        viewModelScope.launch {
            Log.d(tag, "Inizio selectWeapon() per: ${weapon?.name}.")
            val session = gameStateManager.loadSession() ?: run { Log.e(tag, "Sessione non caricata in selectWeapon."); return@launch }
            val hero = session.characters.find { it.id == CharacterID.HERO } ?: run { Log.e(tag, "Eroe non trovato in selectWeapon."); return@launch }
            Log.d(tag, "Eroe caricato in selectWeapon. Discipline: ${hero.kaiDisciplines.joinToString()}. WeaponSkillType: ${hero.details?.weaponSkillType}")


            val newSelectedWeapon = weapon ?: FISTS_WEAPON
            val currentModifiers = hero.details?.activeModifiers?.toMutableList() ?: mutableListOf()
            Log.d(tag, "Modificatori prima della rimozione: ${currentModifiers.map { it.id + ":" + it.amount }}")

            // Rimuovi modificatori di Combattività da ITEM, RULE e DISCIPLINE (specifici di WEAPONSKILL)
            currentModifiers.removeAll { modifier ->
                modifier.statName == "COMBATTIVITA" && (
                        modifier.sourceType == ModifierSourceType.ITEM ||
                                modifier.sourceType == ModifierSourceType.RULE ||
                                (modifier.sourceType == ModifierSourceType.DISCIPLINE && modifier.sourceId == "WEAPONSKILL")
                        )
            }
            Log.d(tag, "Modificatori dopo la rimozione: ${currentModifiers.map { it.id + ":" + it.amount }}")


            // Variabile per tenere traccia del bonus/malus netto dell'arma selezionata
            var weaponNetCombatModifier = 0

            // 1. Applica il bonus intrinseco dell'arma selezionata (se non sono i pugni)
            if (newSelectedWeapon.id != FISTS_WEAPON.id) { // Solo se non sono i pugni
                if (newSelectedWeapon.combatSkillBonus != 0) {
                    weaponNetCombatModifier += newSelectedWeapon.combatSkillBonus
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
                    Log.d(tag, "Aggiunto bonus intrinseco arma: ${newSelectedWeapon.combatSkillBonus}")
                }
            }


            // 2. Gestione della penalità per i pugni E del bonus Scherma (se applicabile)
            if (newSelectedWeapon.id == FISTS_WEAPON.id) {
                val hasWeaponSkill = hero.kaiDisciplines.contains("WEAPONSKILL")
                val weaponSkillTypeChosen = hero.details?.weaponSkillType

                val penaltyAmount = if (hasWeaponSkill && weaponSkillTypeChosen == WeaponType.FISTS) {
                    Log.d(tag, "Scherma con Pugni selezionata: nessuna penalità (-4 negata, diventa 0).")
                    0
                } else {
                    Log.d(tag, "Pugni selezionati senza Scherma specifica: applico penalità -4.")
                    -4
                }

                if (penaltyAmount != 0) {
                    weaponNetCombatModifier += penaltyAmount
                    currentModifiers.add(
                        StatModifier(
                            id = "rule_no_weapon_penalty",
                            statName = "COMBATTIVITA",
                            amount = penaltyAmount,
                            sourceType = ModifierSourceType.RULE,
                            sourceId = "no_weapon",
                            duration = ModifierDuration.PERMANENT
                        )
                    )
                    Log.d(tag, "Aggiunta penalità/bonus Pugni: $penaltyAmount")
                }
            } else { // Se è selezionata una VERA arma (non i pugni)
                val hasWeaponSkill = hero.kaiDisciplines.contains("WEAPONSKILL")
                val weaponSkillTypeChosen = hero.details?.weaponSkillType

                // 3. Applica il bonus +2 da Scherma se l'arma corrisponde al tipo scelto
                if (hasWeaponSkill && weaponSkillTypeChosen == newSelectedWeapon.weaponType) {
                    val schermaBonus = 2
                    weaponNetCombatModifier += schermaBonus
                    currentModifiers.add(
                        StatModifier(
                            id = "discipline_weaponskill_match_bonus",
                            statName = "COMBATTIVITA",
                            amount = schermaBonus,
                            sourceType = ModifierSourceType.DISCIPLINE,
                            sourceId = "WEAPONSKILL",
                            duration = ModifierDuration.PERMANENT
                        )
                    )
                    Log.d(tag, "Bonus +2 Scherma applicato per ${newSelectedWeapon.name} (tipo: ${newSelectedWeapon.weaponType}).")
                }
            }

            Log.d(tag, "Arma: ${newSelectedWeapon.name}, Netto Modificatore Arma: $weaponNetCombatModifier")
            Log.d(tag, "Modificatori finali da salvare: ${currentModifiers.map { it.id + ":" + it.amount }}")

            // Aggiorna i dettagli dell'eroe con la nuova lista di modificatori
            val updatedHeroDetails = hero.details?.copy(activeModifiers = currentModifiers)
            val updatedHero = hero.copy(details = updatedHeroDetails)

            val updatedCharacters = session.characters.map {
                if (it.id == CharacterID.HERO) updatedHero else it
            }
            val updatedSession = session.copy(characters = updatedCharacters)
            gameStateManager.saveSession(updatedSession)
            Log.d(tag, "Sessione salvata dopo selectWeapon().")


            // Ricalcola le armi visibili dall'inventario aggiornato dell'eroe
            val actualWeaponsInInventory = updatedHero.details?.inventory?.filter { it.type == ItemType.WEAPON }?.toMutableList() ?: mutableListOf()
            val updatedVisibleWeapons = mutableListOf<GameItem>()
            if (actualWeaponsInInventory.isNotEmpty()) {
                updatedVisibleWeapons.add(actualWeaponsInInventory[0])
                if (actualWeaponsInInventory.size > 1) {
                    updatedVisibleWeapons.add(actualWeaponsInInventory[1])
                }
            }
            while (updatedVisibleWeapons.size < 2) {
                updatedVisibleWeapons.add(FISTS_WEAPON)
            }
            Log.d(tag, "Armi visibili ricalcolate (selectWeapon): ${updatedVisibleWeapons.map { it.name }}")


            // Aggiorna lo stato UI con le nuove statistiche effettive
            val baseCS = updatedHero.stats?.combattivita ?: 0
            val baseEND = updatedHero.stats?.resistenza ?: 0
            val effectiveCS = calculateEffectiveCombatSkill(baseCS, currentModifiers)
            val effectiveEND = calculateEffectiveEndurance(baseEND, currentModifiers)

            _uiState.update { currentState ->
                currentState.copy(
                    heroCharacter = updatedHero, // Aggiorna heroCharacter nella UI State
                    selectedWeapon = newSelectedWeapon,
                    activeModifiers = currentModifiers,
                    effectiveCombatSkill = effectiveCS,
                    effectiveEndurance = effectiveEND,
                    selectedWeaponModifierAmount = weaponNetCombatModifier,
                    visibleWeapons = updatedVisibleWeapons // AGGIORNATO QUI!
                )
            }
            Log.d(tag, "UI State aggiornata in selectWeapon().")
            Log.d(tag, "Fine selectWeapon(). CS Effettiva: $effectiveCS. END Effettiva: $effectiveEND. Modificatore Arma Selezionata: $weaponNetCombatModifier")
        }
    }

    // --- useBackpackItem MODIFICATA ---
    fun useBackpackItem(item: GameItem) {
        Log.d(tag, "Inizio useBackpackItem() per: ${item.name}.")
        if (!item.isConsumable) {
            Log.d(tag, "L'oggetto '${item.name}' non può essere usato direttamente tramite click. Termino.")
            return
        }

        if (item.quantity <= 0) {
            Log.w(tag, "Tentativo di usare un oggetto con quantità 0: ${item.name}. Termino.")
            return
        }

        viewModelScope.launch {
            val session = gameStateManager.loadSession() ?: run { Log.e(tag, "Sessione non caricata in useBackpackItem."); return@launch }
            val hero = session.characters.find { it.id == CharacterID.HERO } ?: run { Log.e(tag, "Eroe non trovato in useBackpackItem."); return@launch }
            Log.d(tag, "Eroe caricato in useBackpackItem. Resistenza Base attuale: ${hero.stats?.resistenza}")


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
                        Log.d(tag, "Usata Pozione Curativa. Nuova Resistenza Base: $updatedBaseEndurance. Quantità rimanente: ${itemToModify.quantity}")
                    }
                    else -> {
                        Log.e(tag, "Errore logico: Oggetto '${item.name}' è isConsumable ma senza logica di consumo specifica. Termino.")
                        return@launch
                    }
                }

                if (itemToModify.quantity <= 0) {
                    updatedInventory.remove(itemToModify)
                    Log.d(tag, "Oggetto '${item.name}' rimosso dall'inventario (quantità 0).")
                }

                val updatedHeroStats = hero.stats?.copy(resistenza = updatedBaseEndurance)
                val updatedHeroDetails = hero.details?.copy(inventory = updatedInventory, activeModifiers = currentModifiers)
                val updatedHero = hero.copy(details = updatedHeroDetails, stats = updatedHeroStats)

                val updatedCharacters = session.characters.map {
                    if (it.id == CharacterID.HERO) updatedHero else it
                }
                val updatedSession = session.copy(characters = updatedCharacters)
                gameStateManager.saveSession(updatedSession)
                Log.d(tag, "Sessione salvata dopo useBackpackItem().")


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
                        activeModifiers = currentModifiers,
                        selectedWeaponModifierAmount = _uiState.value.selectedWeaponModifierAmount
                    )
                }
                Log.d(tag, "UI State aggiornata in useBackpackItem().")
                Log.d(tag, "Fine useBackpackItem(). END Effettiva: $effectiveEND.")
            } else {
                Log.w(tag, "Oggetto '${item.name}' non trovato nell'inventario per l'uso (dopo il filtro isConsumable). Termino.")
            }
        }
    }

    // --- discardItem MODIFICATA ---
    fun discardItem(item: GameItem) {
        Log.d(tag, "Inizio discardItem() per: ${item.name}.")
        if (!item.isDiscardable) {
            Log.w(tag, "Tentativo di scartare un oggetto non scartabile: ${item.name}. Termino.")
            return
        }

        if (item.quantity <= 0) {
            Log.w(tag, "Tentativo di scartare un oggetto con quantità 0: ${item.name}. Termino.")
            return
        }

        viewModelScope.launch {
            val session = gameStateManager.loadSession() ?: run { Log.e(tag, "Sessione non caricata in discardItem."); return@launch }
            val hero = session.characters.find { it.id == CharacterID.HERO } ?: run { Log.e(tag, "Eroe non trovato in discardItem."); return@launch }
            Log.d(tag, "Eroe caricato in discardItem.")

            val updatedInventory = hero.details?.inventory?.toMutableList() ?: mutableListOf()
            val currentModifiers = hero.details?.activeModifiers?.toMutableList() ?: mutableListOf()
            Log.d(tag, "Modificatori prima della rimozione (discard): ${currentModifiers.map { it.id + ":" + it.amount }}")


            val itemToDiscard = updatedInventory.find { it.id == item.id }

            if (itemToDiscard != null) {
                if (itemToDiscard.quantity > 1) {
                    itemToDiscard.quantity--
                    Log.d(tag, "Scartata 1 unità di '${itemToDiscard.name}'. Quantità rimanente: ${itemToDiscard.quantity}")
                } else {
                    updatedInventory.remove(itemToDiscard)
                    Log.d(tag, "Oggetto '${item.name}' rimosso dall'inventario.")
                }

                // Rimuovi tutti i modificatori associati all'oggetto scartato
                currentModifiers.removeAll { it.sourceId == item.id && it.sourceType == ModifierSourceType.ITEM }
                // Rimuovi anche i modificatori specifici della disciplina se l'oggetto scartato è un'arma
                if (item.type == ItemType.WEAPON) {
                    currentModifiers.removeAll { it.id == "discipline_weaponskill_match_bonus" && it.sourceId == "WEAPONSKILL" }
                    currentModifiers.removeAll { it.id == "rule_no_weapon_penalty" && it.sourceId == "no_weapon" }
                }
                Log.d(tag, "Modificatori dopo la rimozione (discard): ${currentModifiers.map { it.id + ":" + it.amount }}")


                if (item.type == ItemType.WEAPON && _uiState.value.selectedWeapon.id == item.id) {
                    Log.d(tag, "Arma scartata era quella selezionata. Seleziono i Pugni.")
                    selectWeapon(FISTS_WEAPON)
                } else {
                    val updatedHeroDetails = hero.details?.copy(activeModifiers = currentModifiers, inventory = updatedInventory)
                    val updatedHero = hero.copy(details = updatedHeroDetails)

                    val updatedCharacters = session.characters.map {
                        if (it.id == CharacterID.HERO) updatedHero else it
                    }
                    val updatedSession = session.copy(characters = updatedCharacters)
                    gameStateManager.saveSession(updatedSession)
                    Log.d(tag, "Sessione salvata dopo discardItem() (path non selectWeapon).")

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
                            effectiveEndurance = effectiveEND,
                            selectedWeaponModifierAmount = _uiState.value.selectedWeaponModifierAmount
                        )
                    }
                    Log.d(tag, "UI State aggiornata in discardItem() (path non selectWeapon).")
                    Log.d(tag, "Fine discardItem(). Oggetto '${item.name}' scartato con successo. Inventario e modificatori aggiornati.")
                }
            } else {
                Log.w(tag, "Oggetto '${item.name}' non trovato nell'inventario per lo scarto. Termino.")
            }
        }
    }

    // --- addWeapon MODIFICATA ---
    fun addWeapon(newWeapon: GameItem) {
        Log.d(tag, "Inizio addWeapon() per: ${newWeapon.name}.")
        viewModelScope.launch {
            val session = gameStateManager.loadSession() ?: run { Log.e(tag, "Sessione non caricata in addWeapon."); return@launch }
            val hero = session.characters.find { it.id == CharacterID.HERO } ?: run { Log.e(tag, "Eroe non trovato in addWeapon."); return@launch }
            Log.d(tag, "Eroe caricato in addWeapon.")


            val updatedInventory = hero.details?.inventory?.toMutableList() ?: mutableListOf()
            val currentModifiers = hero.details?.activeModifiers?.toMutableList() ?: mutableListOf()

            val currentRealWeapons = updatedInventory.filter { it.type == ItemType.WEAPON && it.id != FISTS_WEAPON.id }.toMutableList()
            Log.d(tag, "Armi reali attuali nell'inventario prima dell'aggiunta: ${currentRealWeapons.map { it.name }}")


            if (currentRealWeapons.size < 2) {
                updatedInventory.add(newWeapon)
                Log.d(tag, "Inventario: Aggiungo ${newWeapon.name} direttamente (meno di 2 armi reali).")
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
                    currentModifiers.removeAll { it.id == "discipline_weaponskill_match_bonus" && it.sourceId == "WEAPONSKILL" }
                    Log.d(tag, "Inventario: Sostituisco ${weaponToReplace.name} con ${newWeapon.name}.")
                } else {
                    Log.w(tag, "Inventario: Impossibile trovare un'arma da sostituire (già 2 armi reali). Aggiungo comunque la nuova arma.")
                }
                updatedInventory.add(newWeapon)
            }

            val updatedHeroDetails = hero.details?.copy(inventory = updatedInventory, activeModifiers = currentModifiers)
            val updatedHero = hero.copy(details = updatedHeroDetails)
            val updatedCharacters = session.characters.map {
                if (it.id == CharacterID.HERO) updatedHero else it
            }
            val updatedSession = session.copy(characters = updatedCharacters)
            gameStateManager.saveSession(updatedSession)
            Log.d(tag, "Sessione salvata dopo addWeapon().")


            // La chiamata a selectWeapon aggiorna anche i modificatori e la UI
            // Assicurati che l'arma selezionata sia impostata correttamente dopo l'aggiunta.
            val selectedWeaponAfterAdd = if (newWeapon.combatSkillBonus != 0 || newWeapon.id == FISTS_WEAPON.id) {
                newWeapon
            } else {
                _uiState.value.selectedWeapon
            }
            selectWeapon(selectedWeaponAfterAdd)
            Log.d(tag, "Fine addWeapon().")
        }
    }
}