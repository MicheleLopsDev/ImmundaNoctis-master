package io.github.luposolitario.immundanoctis.view

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns // Importa OpenableColumns per ottenere il nome del file
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random
import io.github.luposolitario.immundanoctis.data.SessionData
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.data.LoneWolfStats
import io.github.luposolitario.immundanoctis.util.SavePreferences
import io.github.luposolitario.immundanoctis.util.getAppSpecificDirectory
import android.util.Log // Importa Log per il debug

// Stato della UI per la schermata di creazione
data class SetupUiState(
    val heroName: String = "Lupo Solitario",
    val combattivita: Int = 0,
    val resistenza: Int = 0,
    val stdfPrompt: String = "",
    val currentScenesJsonPath: String? = null // Stato per il percorso del JSON delle scene
)

class SetupViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState = _uiState.asStateFlow()

    // Usiamo una MutableStateList per le discipline per integrarla facilmente con Compose
    val selectedDisciplines = mutableStateListOf<String>()

    private lateinit var savePreferences: SavePreferences
    private lateinit var applicationContext: Context // Sarà inizializzato tramite initialize()

    /**
     * Inizializza il ViewModel con il contesto dell'applicazione.
     * Dovrebbe essere chiamato una sola volta, ad esempio nell'onCreate della tua Activity.
     * @param context Il contesto dell'applicazione.
     */
    fun initialize(context: Context) {
        this.applicationContext = context
        this.savePreferences = SavePreferences(context)
        // Carica il percorso salvato all'avvio del ViewModel
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

    /**
     * Copia il file JSON delle scene dalla URI fornita in una directory interna dell'app
     * e aggiorna il percorso nelle SharedPreferences.
     * @param uri L'URI del file JSON selezionato dall'utente.
     */
    fun copyAndSaveScenesJson(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) { // Esegue l'operazione di I/O su un thread in background
            try {
                // Ottieni la directory specifica per i file delle scene (es. "scenes")
                val scenesDirectory = getAppSpecificDirectory(applicationContext, "scenes")
                scenesDirectory?.mkdirs() // Assicurati che la directory esista

                // Ottieni il nome del file dal Content Resolver per un nome più leggibile
                var displayName: String? = null
                applicationContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex)
                        }
                    }
                }

                // Usa il nome ottenuto o un nome predefinito con timestamp
                val fileName = displayName ?: "scenes_uploaded_${System.currentTimeMillis()}.json"
                val destinationFile = File(scenesDirectory, fileName)

                // Copia il contenuto del file
                applicationContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destinationFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                // Salva il percorso del file copiato nelle SharedPreferences
                savePreferences.scenesPath = destinationFile.absolutePath

                // Aggiorna lo stato della UI con il nuovo percorso (torna sul main thread per UI update)
                _uiState.update { it.copy(currentScenesJsonPath = destinationFile.absolutePath) }

                Log.d("SetupViewModel", "File JSON copiato con successo: ${destinationFile.absolutePath}")

            } catch (e: Exception) {
                // Gestisci l'errore (es. logga l'eccezione)
                Log.e("SetupViewModel", "Errore durante la copia del file JSON: ${e.message}", e)
                e.printStackTrace() // Stampa lo stack trace per debug
            }
        }
    }

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
        } else if (selectedDisciplines.size < 5) { // Limite di 5 discipline
            selectedDisciplines.add(disciplineId)
        }
    }
}