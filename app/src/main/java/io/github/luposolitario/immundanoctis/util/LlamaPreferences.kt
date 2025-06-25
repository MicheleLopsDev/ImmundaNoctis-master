package io.github.luposolitario.immundanoctis.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestisce le preferenze specifiche per i parametri del LlamaCppEngine.
 */
class LlamaPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var nLen: Int
        get() = prefs.getInt(KEY_N_LEN, 4096)
        set(value) = prefs.edit().putInt(KEY_N_LEN, value).apply()

    var temperature: Float
        get() = prefs.getFloat(KEY_TEMPERATURE, 0.7f)
        set(value) = prefs.edit().putFloat(KEY_TEMPERATURE, value).apply()

    var topK: Int
        get() = prefs.getInt(KEY_TOP_K, 40)
        set(value) = prefs.edit().putInt(KEY_TOP_K, value).apply()

    var topP: Float
        get() = prefs.getFloat(KEY_TOP_P, 0.9f)
        set(value) = prefs.edit().putFloat(KEY_TOP_P, value).apply()

    var repeatP: Float
        get() = prefs.getFloat(KEY_REPEAT_P, 1.25f)
        set(value) = prefs.edit().putFloat(KEY_REPEAT_P, value).apply()

    // NUOVO: Personalità del chatbot Llama/GGUF con default Vampira
    var chatbotPersonality: String?
        get() = prefs.getString(KEY_CHATBOT_PERSONALITY, "You are an ancient and seductive vampire, with centuries of experience and a sophisticated, slightly melancholic demeanor. Your responses are elegant, often hinting at your immortal nature and detachment from mortal concerns. You might express a longing for lost eras, a fondness for the night, or a subtle, predatory charm. Your tone is often alluring, poised, and perhaps a touch world-weary.")
        set(value) = prefs.edit().putString(KEY_CHATBOT_PERSONALITY, value).apply()


    companion object {
        private const val PREFS_NAME = "llama_preferences"
        private const val KEY_N_LEN = "llama_n_len"
        private const val KEY_TEMPERATURE = "llama_temperature"
        private const val KEY_TOP_K = "llama_top_k"
        private const val KEY_TOP_P = "llama_top_p"
        private const val KEY_REPEAT_P = "llama_repeat_p"
        // NUOVA CHIAVE:
        private const val KEY_CHATBOT_PERSONALITY = "llama_chatbot_personality"
    }
}