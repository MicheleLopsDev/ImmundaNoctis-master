package io.github.luposolitario.immundanoctis.util

import android.content.Context
import android.content.SharedPreferences

class TtsPreferences(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(TTS_PREFS_NAME, Context.MODE_PRIVATE)

    // ... metodi isAutoReadEnabled, get/save SpeechRate e Pitch rimangono invariati ...
    fun saveAutoRead(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_READ, enabled).apply()
    }
    fun isAutoReadEnabled(): Boolean {
        return preferences.getBoolean(KEY_AUTO_READ, false)
    }
    fun saveSpeechRate(rate: Float) {
        preferences.edit().putFloat(KEY_SPEECH_RATE, rate).apply()
    }
    fun getSpeechRate(): Float {
        return preferences.getFloat(KEY_SPEECH_RATE, 1.0f)
    }
    fun savePitch(pitch: Float) {
        preferences.edit().putFloat(KEY_PITCH, pitch).apply()
    }
    fun getPitch(): Float {
        return preferences.getFloat(KEY_PITCH, 1.0f)
    }

    // --- METODI MODIFICATI E SPECIALIZZATI PER GENERE ---
    /**
     * Salva il nome della voce TTS preferita per un genere specifico.
     * @param gender "MALE" o "FEMALE".
     * @param voiceName Il nome della voce, o null per resettare.
     */
    fun saveVoiceForGender(gender: String, voiceName: String?) {
        val key = if (gender.equals("MALE", ignoreCase = true)) KEY_VOICE_MALE else KEY_VOICE_FEMALE
        preferences.edit().putString(key, voiceName).apply()
    }

    /**
     * Recupera il nome della voce TTS preferita per un genere specifico.
     * @param gender "MALE" o "FEMALE".
     * @return Il nome della voce salvata, o null.
     */
    fun getVoiceForGender(gender: String): String? {
        val key = if (gender.equals("MALE", ignoreCase = true)) KEY_VOICE_MALE else KEY_VOICE_FEMALE
        return preferences.getString(key, null)
    }
    // --- FINE METODI MODIFICATI ---

    companion object {
        private const val TTS_PREFS_NAME = "tts_preferences"
        private const val KEY_AUTO_READ = "auto_read_enabled"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_PITCH = "pitch"
        // --- NUOVE CHIAVI PER GENERE ---
        private const val KEY_VOICE_MALE = "selected_voice_male"
        private const val KEY_VOICE_FEMALE = "selected_voice_female"
    }
}
