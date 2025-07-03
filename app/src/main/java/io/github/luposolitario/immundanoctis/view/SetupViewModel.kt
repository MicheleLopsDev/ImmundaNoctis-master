// immundanoctis/view/SetupViewModel.kt
package io.github.luposolitario.immundanoctis.view

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.luposolitario.immundanoctis.R
import io.github.luposolitario.immundanoctis.data.*
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

// --- SetupUiState MODIFICATA per includere chosenWeaponSkillType ---
data class SetupUiState(
    val heroName: String = "Lupo Solitario",
    val combattivita: Int = 0,
    val resistenza: Int = 0,
    val stdfPrompt: String = "",
    val currentScenesJsonPath: String? = null,
    val selectedWeapon: GameItem? = null,
    val selectedSpecialItem: GameItem? = null,
    val chosenWeaponSkillType: WeaponType? = null // <--- Spostato qui per coerenza e persistenza
)

class SetupViewModel() : ViewModel() {

    private val tag = "SetupViewModel" // Abbreviato per visibilità nei log

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState = _uiState.asStateFlow()

    val selectedDisciplines = mutableStateListOf<String>()

    // Questi due stati ora riflettono la necessità del dialogo, ma il valore finale va nella uiState
    private val _showWeaponSkillDialog = MutableStateFlow(false)
    val showWeaponSkillDialog: StateFlow<Boolean> = _showWeaponSkillDialog.asStateFlow()

    // Questo sarà il valore temporaneo rollato per il dialogo, verrà poi spostato in uiState.chosenWeaponSkillType
    private val _dialogRolledWeaponSkillType = MutableStateFlow<WeaponType?>(null)
    val dialogRolledWeaponSkillType: StateFlow<WeaponType?> = _dialogRolledWeaponSkillType.asStateFlow()

    private lateinit var savePreferences: SavePreferences
    private lateinit var applicationContext: Context

    fun initialize(context: Context) {
        this.applicationContext = context
        this.savePreferences = SavePreferences(context)
        _uiState.update { it.copy(currentScenesJsonPath = savePreferences.scenesPath) }
        Log.d(tag, "ViewModel Inizializzato.")
    }

    fun updateHeroName(newName: String) {
        _uiState.update { it.copy(heroName = newName) }
        Log.d(tag, "Nome eroe aggiornato: ${newName}")
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
        Log.d(tag, "Statistiche rollate: CS=${_uiState.value.combattivita}, RES=${_uiState.value.resistenza}")
    }

    fun copyAndSaveScenesJson(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val scenesDirectory = getAppSpecificDirectory(applicationContext, "scenes")
                scenesDirectory?.mkdirs()

                var displayName: String? = null
                applicationContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
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
        _uiState.update { it.copy(selectedSpecialItem = item) }
        Log.d(tag, "Oggetto speciale iniziale selezionato: ${item.name}")
    }

    // --- LOGICA SPECIFICA PER SCHERMA ---
    fun toggleDiscipline(disciplineId: String) {
        if (selectedDisciplines.contains(disciplineId)) {
            selectedDisciplines.remove(disciplineId)
            Log.d(tag, "Disciplina rimossa: ${disciplineId}. Discipline attuali: ${selectedDisciplines.joinToString()}")
            // Se la disciplina rimossa è Scherma, resetta la scelta del tipo di arma nella UI State
            if (disciplineId == "WEAPONSKILL") {
                _uiState.update { it.copy(chosenWeaponSkillType = null) }
                _dialogRolledWeaponSkillType.value = null
                _showWeaponSkillDialog.value = false // Assicurati che il dialogo si chiuda
                Log.d(tag, "Disciplina Scherma rimossa, tipo di arma resettato nella UI State e dialogo chiuso.")
            }
        } else if (selectedDisciplines.size < 5) {
            selectedDisciplines.add(disciplineId)
            Log.d(tag, "Disciplina aggiunta: ${disciplineId}. Discipline attuali: ${selectedDisciplines.joinToString()}")
            // Se la disciplina aggiunta è Scherma, triggera il roll del tipo di arma
            if (disciplineId == "WEAPONSKILL") {
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
        Log.d(tag, "Tipi di arma disponibili per roll Scherma: ${availableWeaponTypes.map { it.name }}")

        if (availableWeaponTypes.isNotEmpty()) {
            val rolledType = availableWeaponTypes.random(Random)
            _dialogRolledWeaponSkillType.value = rolledType // Imposta il valore per il dialogo
            _showWeaponSkillDialog.value = true // Mostra il pop-up
            Log.d(tag, "Tipo di arma per Scherma rollato per dialogo: ${rolledType.name}. Dialogo mostrato.")
        } else {
            Log.w(tag, "Nessun tipo di arma disponibile per Scherma dopo il filtro.")
            _dialogRolledWeaponSkillType.value = null
            _showWeaponSkillDialog.value = false
        }
    }

    fun confirmWeaponSkillSelection() {
        Log.d(tag, "Conferma selezione Scherma avviata. Valore dialogRolledWeaponSkillType: ${_dialogRolledWeaponSkillType.value?.name}")
        _uiState.update { it.copy(chosenWeaponSkillType = _dialogRolledWeaponSkillType.value) } // <--- CRUCIALE: Associa il valore rollato allo stato UI
        _showWeaponSkillDialog.value = false // Nasconde il pop-up
        Log.d(tag, "Selezione Scherma confermata. chosenWeaponSkillType in UI State: ${_uiState.value.chosenWeaponSkillType?.name}. Dialogo chiuso.")
    }
    // --- FINE LOGICA SPECIFICA PER SCHERMA ---

    fun finalizeSessionCreation(defaultSession: SessionData): SessionData {
        Log.d(tag, "Inizio finalizeSessionCreation().")
        val currentState = _uiState.value
        val hero = defaultSession.characters.find { it.id == CharacterID.HERO }!!
        Log.d(tag, "Stato UI al finalizza: CS=${currentState.combattivita}, RES=${currentState.resistenza}, Arma=${currentState.selectedWeapon?.name}, Special=${currentState.selectedSpecialItem?.name}, SchermaType=${currentState.chosenWeaponSkillType?.name}")


        val initialInventory = mutableListOf<GameItem>()
        var finalResistenza = currentState.resistenza

        currentState.selectedWeapon?.let {
            initialInventory.add(it)
            Log.d(tag, "Aggiunto ${it.name} all'inventario finale.")
        } ?: Log.w(tag, "Nessuna arma selezionata in finalizeSessionCreation.")

        currentState.selectedSpecialItem?.let { item ->
            initialInventory.add(item)
            item.bonuses?.get("RESISTENZA")?.let { bonus ->
                finalResistenza += bonus
                Log.d(tag, "Bonus Resistenza applicato da ${item.name}: +$bonus. Nuova Resistenza: $finalResistenza")
            }
            Log.d(tag, "Aggiunto ${item.name} all'inventario finale.")
        } ?: Log.w(tag, "Nessun oggetto speciale selezionato in finalizeSessionCreation.")


        INITIAL_COMMON_ITEMS.forEach { commonItem ->
            val itemToAdd = commonItem.copy() // Crea una copia per evitare modifiche alla lista originale
            if (itemToAdd.type == ItemType.GOLD) {
                // Randomizza la quantità di oro
                itemToAdd.quantity = Random.nextInt(10, 20)
            }
            initialInventory.add(itemToAdd)
            Log.d(tag, "Aggiunto oggetto comune: ${itemToAdd.name} (x${itemToAdd.quantity})")
        }
        Log.d(tag, "Inventario finale costruito in finalizeSessionCreation: ${initialInventory.map { it.name }}")


        // --- SALVA IL TIPO DI ARMA PER SCHERMA NEI DETTAGLI DELL'EROE ---
        val finalHeroDetails = hero.details?.copy(
            specialAbilities = listOf("Immunità alle malattie"),
            inventory = initialInventory, // Assicurati di passare la lista mutabile finale
            weaponSkillType = if (selectedDisciplines.contains("WEAPONSKILL")) currentState.chosenWeaponSkillType else null // Prende il valore dallo stato UI
        )
        // --- FINE SALVATAGGIO TIPO ARMA ---

        val updatedHero = hero.copy(
            name = currentState.heroName,
            stats = LoneWolfStats(
                combattivita = currentState.combattivita,
                resistenza = finalResistenza
            ),
            kaiDisciplines = selectedDisciplines.toList(),
            details = finalHeroDetails
        )
        Log.d(tag, "Eroe aggiornato in finalizeSessionCreation. Final WeaponSkillType: ${updatedHero.details?.weaponSkillType}")


        val updatedCharacters = defaultSession.characters.map {
            if (it.id == CharacterID.HERO) updatedHero else it
        }

        val finalSession = defaultSession.copy(
            sessionName = "Avventura di ${currentState.heroName}",
            lastUpdate = System.currentTimeMillis(),
            characters = updatedCharacters,
            isStarted = false, // Verrà impostato a true da MainViewModel.sendInitialDmPrompt
            usedScenes = mutableListOf()
        )
        Log.d(tag, "Sessione finalizzata e pronta per il salvataggio. Arma Skill Type: ${finalSession.characters.find{it.id == CharacterID.HERO}?.details?.weaponSkillType}")
        return finalSession
    }
}