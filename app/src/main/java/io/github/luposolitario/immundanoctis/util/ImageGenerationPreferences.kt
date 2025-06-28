package io.github.luposolitario.immundanoctis.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Contiene le preferenze di generazione salvate per un modello specifico.
 * Questa classe è un contenitore di dati semplice.
 */
data class GenerationPrefs(
    val prompt: String = "",
    val negativePrompt: String = "",
    val steps: Float = 20f,
    val cfg: Float = 7f,
    val seed: String = "",
    val size: Int = 512,
    val denoiseStrength: Float = 0.6f
)

/**
 * Gestisce le preferenze specifiche per i parametri di generazione delle immagini,
 * adattando la logica di 'GenerationPreferences' di localdream per usare SharedPreferences.
 */
class ImageGenerationPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- Chiavi dinamiche, come nell'originale ---
    private fun getPromptKey(modelId: String) = "${modelId}_prompt"
    private fun getNegativePromptKey(modelId: String) = "${modelId}_negative_prompt"
    private fun getStepsKey(modelId: String) = "${modelId}_steps"
    private fun getCfgKey(modelId: String) = "${modelId}_cfg"
    private fun getSeedKey(modelId: String) = "${modelId}_seed"
    private fun getSizeKey(modelId: String) = "${modelId}_size"
    private fun getDenoiseStrengthKey(modelId: String) = "${modelId}_denoise_strength"


    // --- NUOVI METODI PER IL MODELLO SELEZIONATO ---
    /**
     * Salva l'ID del modello STDF selezionato come predefinito per la generazione.
     */
    fun saveSelectedModelId(modelId: String?) {
        prefs.edit().putString(KEY_SELECTED_MODEL_ID, modelId).apply()
    }

    /**
     * Recupera l'ID del modello STDF selezionato.
     */
    fun getSelectedModelId(): String? {
        return prefs.getString(KEY_SELECTED_MODEL_ID, null)
    }

    /**
     * Salva l'URL di base per il download dei modelli.
     */
    fun saveBaseUrl(url: String) {
        prefs.edit().putString(KEY_BASE_URL, url).apply()
    }

    /**
     * Recupera l'URL di base per il download.
     * @return L'URL salvato o un URL di default di Hugging Face.
     */
    fun getBaseUrl(): String {
        return prefs.getString(KEY_BASE_URL, "https://huggingface.co/") ?: "https://huggingface.co/"
    }

    /**
     * Salva tutte le preferenze di generazione per un modello specifico in una sola volta.
     */
    fun saveAllFields(modelId: String, prefsData: GenerationPrefs) {
        with(prefs.edit()) {
            putString(getPromptKey(modelId), prefsData.prompt)
            putString(getNegativePromptKey(modelId), prefsData.negativePrompt)
            putFloat(getStepsKey(modelId), prefsData.steps)
            putFloat(getCfgKey(modelId), prefsData.cfg)
            putString(getSeedKey(modelId), prefsData.seed)
            putInt(getSizeKey(modelId), prefsData.size)
            putFloat(getDenoiseStrengthKey(modelId), prefsData.denoiseStrength)
            apply()
        }
    }

    /**
     * Recupera tutte le preferenze per un dato modelId.
     * @param modelId L'ID del modello di cui recuperare le preferenze.
     * @return Un oggetto GenerationPrefs con i valori salvati o i valori di default.
     */
    fun getPreferences(modelId: String): GenerationPrefs {
        return GenerationPrefs(
            prompt = prefs.getString(getPromptKey(modelId), "") ?: "",
            negativePrompt = prefs.getString(getNegativePromptKey(modelId), "") ?: "",
            steps = prefs.getFloat(getStepsKey(modelId), 20f),
            cfg = prefs.getFloat(getCfgKey(modelId), 7f),
            seed = prefs.getString(getSeedKey(modelId), "") ?: "",
            size = prefs.getInt(getSizeKey(modelId), 512),
            denoiseStrength = prefs.getFloat(getDenoiseStrengthKey(modelId), 0.6f)
        )
    }

    companion object {
        private const val PREFS_NAME = "image_generation_prefs"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_SELECTED_MODEL_ID = "selected_stdf_model_id"
    }
}