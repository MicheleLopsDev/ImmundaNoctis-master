package io.github.luposolitario.immundanoctis.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.github.luposolitario.immundanoctis.stdf.data.StdfModel
import io.github.luposolitario.immundanoctis.stdf.data.StdfModelRepository
import io.github.luposolitario.immundanoctis.util.ImageGenerationPreferences
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 1. Aggiorna lo stato della UI
data class StdfScreenState(
    val models: List<StdfModel> = emptyList(),
    val hfToken: String? = null,
    val selectedModelId: String? = null // <-- Aggiungi questo
)


class StdfViewModel(application: Application) : AndroidViewModel(application) {

    private val imageGenerationPreferences = ImageGenerationPreferences(application)
    private val modelRepository = StdfModelRepository(application)
    private val workManager = WorkManager.getInstance(application)
    private val themePreferences = ThemePreferences(application)

    private val _uiState = MutableStateFlow(StdfScreenState())
    val uiState: StateFlow<StdfScreenState> = _uiState.asStateFlow()

    // Espone il LiveData che l'Activity osserverà
    val workInfosLiveData: LiveData<List<WorkInfo>> =
        workManager.getWorkInfosByTagLiveData("stdf_download_tag")

    init {
        reloadModels()
        loadHfToken()
        loadSelectedModel() // <-- Carica il modello selezionato all'avvio
    }

    fun reloadModels() {
        // Ricarica la lista dei modelli, ad esempio dopo un download completato
        _uiState.update { it.copy(models = modelRepository.models) }
    }

    private fun loadHfToken() {
        _uiState.update { it.copy(hfToken = themePreferences.getToken()) }
    }

    // ... (reloadModels e loadHfToken rimangono invariati)

    private fun loadSelectedModel() {
        _uiState.update { it.copy(selectedModelId = imageGenerationPreferences.getSelectedModelId()) }
    }

    // --- NUOVO METODO PER SELEZIONARE UN MODELLO ---
    fun selectModel(modelId: String) {
        imageGenerationPreferences.saveSelectedModelId(modelId)
        _uiState.update { it.copy(selectedModelId = modelId) }
    }

    fun startDownload(model: StdfModel) {
        val token = _uiState.value.hfToken
        model.download(getApplication(), token)
    }

    fun deleteModel(model: StdfModel) {
        viewModelScope.launch {
            model.deleteModel(getApplication())
            reloadModels()
        }
    }
}