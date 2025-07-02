// File: SetupViewModel.kt
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
import io.github.luposolitario.immundanoctis.data.* // Assicurati che questo import sia presente
import io.github.luposolitario.immundanoctis.util.SavePreferences
import io.github.luposolitario.immundanoctis.util.getAppSpecificDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

data class SetupUiState(
    val heroName: String = "Lupo Solitario",
    val combattivita: Int = 0,
    val resistenza: Int = 0,
    val stdfPrompt: String = "",
    val currentScenesJsonPath: String? = null,
    val selectedWeapon: GameItem? = null,
    val selectedSpecialItem: GameItem? = null
)

class SetupViewModel() : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState = _uiState.asStateFlow()

    val selectedDisciplines = mutableStateListOf<String>()

    private lateinit var savePreferences: SavePreferences
    private lateinit var applicationContext: Context

    fun initialize(context: Context) {
        this.applicationContext = context
        this.savePreferences = SavePreferences(context)
        _uiState.update { it.copy(currentScenesJsonPath = savePreferences.scenesPath) }
    }

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
                Log.d("SetupViewModel", "File JSON copiato con successo: ${destinationFile.absolutePath}")

            } catch (e: Exception) {
                Log.e("SetupViewModel", "Errore durante la copia del file JSON: ${e.message}", e)
            }
        }
    }

    fun onWeaponSelected(weapon: GameItem) {
        _uiState.update { it.copy(selectedWeapon = weapon) }
    }

    fun onSpecialItemSelected(item: GameItem) {
        _uiState.update { it.copy(selectedSpecialItem = item) }
    }

    fun finalizeSessionCreation(defaultSession: SessionData): SessionData {
        val currentState = _uiState.value
        val hero = defaultSession.characters.find { it.id == CharacterID.HERO }!!

        val initialInventory = mutableListOf<GameItem>()
        var finalResistenza = currentState.resistenza

        // 1. Aggiungi l'arma scelta
        currentState.selectedWeapon?.let {
            initialInventory.add(it)
        }

        // 2. Aggiungi l'oggetto speciale scelto e applica i bonus
        currentState.selectedSpecialItem?.let { item ->
            initialInventory.add(item)
            item.bonuses?.get("RESISTENZA")?.let { bonus ->
                finalResistenza += bonus
                Log.d("SetupViewModel", "Bonus Resistenza applicato da ${item.name}: +$bonus. Nuova Resistenza: $finalResistenza")
            }
        }

        // 3. Aggiungi tutti gli oggetti comuni iniziali
        INITIAL_COMMON_ITEMS.forEach { commonItem ->
            val itemToAdd = commonItem.copy() // Crea una copia per evitare modifiche alla lista originale
            if (itemToAdd.type == ItemType.GOLD) {
                // Randomizza la quantità di oro
                itemToAdd.quantity = Random.nextInt(10, 20)
            }
            initialInventory.add(itemToAdd)
        }

        val updatedHero = hero.copy(
            name = currentState.heroName,
            stats = LoneWolfStats(
                combattivita = currentState.combattivita,
                resistenza = finalResistenza
            ),
            kaiDisciplines = selectedDisciplines.toList(),
            details = HeroDetails(
                specialAbilities = listOf("Immunità alle malattie"),
                inventory = initialInventory
            )
        )

        val updatedCharacters = defaultSession.characters.map {
            if (it.id == CharacterID.HERO) updatedHero else it
        }

        return defaultSession.copy(
            sessionName = "Avventura di ${currentState.heroName}",
            lastUpdate = System.currentTimeMillis(),
            characters = updatedCharacters,
            isStarted = false,
            usedScenes = mutableListOf()
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