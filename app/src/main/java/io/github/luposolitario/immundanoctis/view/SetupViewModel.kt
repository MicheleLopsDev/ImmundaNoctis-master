// immundanoctis/view/SetupViewModel.kt
package io.github.luposolitario.immundanoctis.view

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.luposolitario.immundanoctis.R
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.data.CharacterType
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.GameItem
import io.github.luposolitario.immundanoctis.data.HeroDetails
import io.github.luposolitario.immundanoctis.data.INITIAL_COMMON_ITEMS
import io.github.luposolitario.immundanoctis.data.ItemType
import io.github.luposolitario.immundanoctis.data.LoneWolfStats
import io.github.luposolitario.immundanoctis.data.ModifierDuration
import io.github.luposolitario.immundanoctis.data.ModifierSourceType
import io.github.luposolitario.immundanoctis.data.SessionData
import io.github.luposolitario.immundanoctis.data.StatModifier
import io.github.luposolitario.immundanoctis.data.WeaponType
import io.github.luposolitario.immundanoctis.engine.GameLogicManager
import io.github.luposolitario.immundanoctis.util.SavePreferences
import io.github.luposolitario.immundanoctis.util.getAppSpecificDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random


class SetupViewModel() : ViewModel() {

    private val tag = "SetupViewModel" // Abbreviato per visibilità nei log

    private val defaultHero = GameCharacter(
        id = CharacterID.HERO,
        name = "Lupo Solitario",
        type = CharacterType.PLAYER,
        portraitResId = R.drawable.ic_hero_portrait_placeholder,
        gender = "MALE",
        language = "it",
        stats = LoneWolfStats(combattivita = 15, resistenza = 25),
        kaiDisciplines = listOf("Sixth Sense", "Healing", "Mindshield", "Weaponskill", "Hunting"),
        details = HeroDetails()
    )

    private val _uiState = MutableStateFlow(defaultHero)
    val uiState = _uiState.asStateFlow()
    val selectedDisciplines = mutableStateListOf<String>()

    // Questi due stati ora riflettono la necessità del dialogo, ma il valore finale va nella uiState
    private val _showWeaponSkillDialog = MutableStateFlow(false)
    val showWeaponSkillDialog: StateFlow<Boolean> = _showWeaponSkillDialog.asStateFlow()

    // Questo sarà il valore temporaneo rollato per il dialogo, verrà poi spostato in uiState.chosenWeaponSkillType
    private val _dialogRolledWeaponSkillType = MutableStateFlow<WeaponType?>(null)
    val dialogRolledWeaponSkillType: StateFlow<WeaponType?> =
        _dialogRolledWeaponSkillType.asStateFlow()
    private lateinit var savePreferences: SavePreferences
    private lateinit var applicationContext: Context

    // NUOVO: Dichiarazione di gameLogicManager
    fun initialize(context: Context) {

        this.applicationContext = context
        this.savePreferences = SavePreferences(context)
        // NUOVO: Inizializzazione di gameLogicManager qui
        _uiState.update { it.copy(currentScenesJsonPath = savePreferences.scenesPath) }
        Log.d(tag, "ViewModel Inizializzato.")
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
        Log.d(
            tag,
            "Statistiche rollate: CS=${_uiState.value.combattivita}, RES=${_uiState.value.resistenza}"
        )
    }

    fun copyAndSaveScenesJson(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val scenesDirectory = getAppSpecificDirectory(applicationContext, "scenes")
                scenesDirectory?.mkdirs()

                var displayName: String? = null
                applicationContext.contentResolver.query(uri, null, null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                displayName = cursor.getString(nameIndex)
                            }
                        }
                    }

                val fileName = displayName ?: "scenes_uploaded_${System.currentTimeMillis()}.json"
                val destinationFile = File(scenesDirectory, fileName)

                applicationContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destinationFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                savePreferences.scenesPath = destinationFile.absolutePath
                _uiState.update { it.copy(currentScenesJsonPath = destinationFile.absolutePath) }
                Log.d(tag, "File JSON copiato con successo: ${destinationFile.absolutePath}")

                // Usa il nuovo metodo per forzare il ricaricamento e invalidare la cache
                GameLogicManager.forceReloadScenesFromFile(applicationContext)

            } catch (e: Exception) {
                Log.e(tag, "Errore durante la copia del file JSON: ${e.message}", e)
            }
        }
    }

    fun onWeaponSelected(weapon: GameItem) {
        _uiState.update { it.copy(selectedWeapon = weapon) }
        Log.d(tag, "Arma iniziale selezionata: ${weapon.name} (Tipo: ${weapon.weaponType})")
    }

    fun onSpecialItemSelected(item: GameItem) {
        _uiState.update { it.copy(selectedSpecialIntialItem = item) }
        Log.d(tag, "Oggetto speciale iniziale selezionato: ${item.name}")
    }

    // --- LOGICA SPECIFICA PER SCHERMA ---
    fun toggleDiscipline(disciplineId: String) {
        if (selectedDisciplines.contains(disciplineId)) {
            selectedDisciplines.remove(disciplineId)
            Log.d(
                tag,
                "Disciplina rimossa: ${disciplineId}. Discipline attuali: ${selectedDisciplines.joinToString()}"
            )
            // Se la disciplina rimossa è Scherma, resetta la scelta del tipo di arma nella UI State
            if (disciplineId == "Weaponskill") {
                _uiState.update { it.copy(chosenWeaponSkillType = null) }
                _dialogRolledWeaponSkillType.value = null
                _showWeaponSkillDialog.value = false // Assicurati che il dialogo si chiuda
                Log.d(
                    tag,
                    "Disciplina Scherma rimossa, tipo di arma resettato nella UI State e dialogo chiuso."
                )
            }
        } else if (selectedDisciplines.size < 5) {
            selectedDisciplines.add(disciplineId)
            Log.d(
                tag,
                "Disciplina aggiunta: ${disciplineId}. Discipline attuali: ${selectedDisciplines.joinToString()}"
            )
            // Se la disciplina aggiunta è Scherma, triggera il roll del tipo di arma
            if (disciplineId == "Weaponskill") {
                rollWeaponSkillTypeForScherma()
            }
        } else {
            Log.d(tag, "Limite di discipline raggiunto (5). Non posso aggiungere ${disciplineId}.")
        }
    }

    private fun rollWeaponSkillTypeForScherma() {
        // MODIFICATO: Rimosso completamente il filtro per DAGGER e FISTS.
        // Adesso WeaponType.entries include FISTS, e DAGGER non esiste più.
        val availableWeaponTypes = WeaponType.entries // <--- MODIFICA QUI
        Log.d(
            tag,
            "Tipi di arma disponibili per roll Scherma: ${availableWeaponTypes.map { it.name }}"
        )

        if (availableWeaponTypes.isNotEmpty()) {
            val rolledType = availableWeaponTypes.random(Random)
            _dialogRolledWeaponSkillType.value = rolledType // Imposta il valore per il dialogo
            _showWeaponSkillDialog.value = true // Mostra il pop-up
            Log.d(
                tag,
                "Tipo di arma per Scherma rollato per dialogo: ${rolledType.name}. Dialogo mostrato."
            )
        } else {
            Log.w(tag, "Nessun tipo di arma disponibile per Scherma dopo il filtro.")
            _dialogRolledWeaponSkillType.value = null
            _showWeaponSkillDialog.value = false
        }
    }

    fun confirmWeaponSkillSelection() {
        Log.d(
            tag,
            "Conferma selezione Scherma avviata. Valore dialogRolledWeaponSkillType: ${_dialogRolledWeaponSkillType.value?.name}"
        )
        _uiState.update { it.copy(chosenWeaponSkillType = _dialogRolledWeaponSkillType.value) } // <--- CRUCIALE: Associa il valore rollato allo stato UI
        _showWeaponSkillDialog.value = false // Nasconde il pop-up
        Log.d(
            tag,
            "Selezione Scherma confermata. chosenWeaponSkillType in UI State: ${_uiState.value.chosenWeaponSkillType?.name}. Dialogo chiuso."
        )
    }
    // --- FINE LOGICA SPECIFICA PER SCHERMA ---

    // In SetupViewModel.kt

    fun finalizeSessionCreation(defaultSession: SessionData): SessionData {
        Log.d(tag, "Inizio finalizeSessionCreation().")
        val heroState = _uiState.value

        // --- 1. COSTRUISCI L'INVENTARIO FINALE ---
        val finalInventory = mutableListOf<GameItem>()
        heroState.selectedWeapon?.let { finalInventory.add(it) }
        heroState.selectedSpecialIntialItem?.let { finalInventory.add(it) }

        INITIAL_COMMON_ITEMS.forEach { commonItem ->
            val itemToAdd = commonItem.copy(
                quantity = if (commonItem.type == ItemType.GOLD) Random.nextInt(10, 20) else commonItem.quantity
            )
            finalInventory.add(itemToAdd)
        }
        Log.d(tag, "Inventario finale costruito: ${finalInventory.map { it.name }}")

        // --- 2. CALCOLA LE STATS FINALI ---
        var mod:StatModifier?  = null
        val finalMod:MutableList<StatModifier> = mutableListOf<StatModifier>()


        var finalResistenza = heroState.resistenza
        heroState.selectedSpecialIntialItem?.bonuses?.get("RESISTENZA")?.let { bonus ->
            mod = StatModifier(
                id = "special_bonus_${heroState.selectedSpecialIntialItem.id}",
                statName = "RESISTENZA",
                amount = heroState.selectedSpecialIntialItem.bonuses["RESISTENZA"]!!,
                sourceType = ModifierSourceType.ITEM,
                sourceId = heroState.selectedSpecialIntialItem.id,
                duration = ModifierDuration.UNTIL_UNEQUIPPED
            )
            finalMod.add(mod)
            finalResistenza += bonus
        }



        val finalStats = heroState.stats?.copy(
            combattivita = heroState.combattivita,
            resistenza = finalResistenza
        )
        Log.d(tag, "Statistiche finali calcolate: CS=${finalStats?.combattivita}, RES=${finalStats?.resistenza}")



        if(heroState.chosenWeaponSkillType !=null )
        {
            if(heroState.chosenWeaponSkillType == heroState.selectedWeapon?.weaponType)
            {
                Log.d(tag, "WeaponSkillType selezionato Activate bonus: ${heroState.chosenWeaponSkillType.name}")
                mod = StatModifier(
                    id = "weapon_bonus_${heroState.selectedWeapon.id}",
                    statName = "COMBATTIVITA",
                    amount = heroState.selectedWeapon.combatSkillBonus,
                    sourceType = ModifierSourceType.ITEM,
                    sourceId = heroState.selectedWeapon.id,
                    duration = ModifierDuration.UNTIL_UNEQUIPPED
                )
                finalMod.add(mod)
            }
        }

        // --- 3. CREA I DETTAGLI FINALI DELL'EROE (Senza le discipline) ---
        val finalDetails = heroState.details?.copy(
            inventory = finalInventory,
            weaponSkillType = heroState.chosenWeaponSkillType,
            activeModifiers = finalMod
        )
        Log.d(tag, "Dettagli finali creati. WeaponSkillType: ${finalDetails?.weaponSkillType}")

        // --- 4. CREA L'EROE FINALE CON I DETTAGLI, STATS E DISCIPLINE AGGIORNATE ---
        val finalHero = heroState.copy(
            stats = finalStats,
            details = finalDetails,
            // *** ECCO LA CORREZIONE: Assegno le discipline qui, all'oggetto GameCharacter ***
            kaiDisciplines = selectedDisciplines.toList()
        )

        // --- 5. CREA LA SESSIONE FINALE CON L'EROE AGGIORNATO ---
        val finalSession = defaultSession.copy(
            sessionName = GameLogicManager.adventureName,
            lastUpdate = System.currentTimeMillis(),
            hero = finalHero, // Usa l'eroe appena creato!
            isStarted = false,
            usedScenes = mutableListOf()
        )
        Log.d(tag, "Sessione finalizzata. Eroe: ${finalSession.hero.name}, Discipline: ${finalSession.hero.kaiDisciplines}")

        return finalSession
    }
}