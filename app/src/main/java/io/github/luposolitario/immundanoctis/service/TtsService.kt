package io.github.luposolitario.immundanoctis.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.util.TtsPreferences
import java.util.Locale

/**
 * Un servizio per gestire le operazioni di Text-to-Speech (TTS).
 *
 * Questa classe incapsula l'istanza di TextToSpeech, gestendo l'inizializzazione,
 * la selezione della voce e la riproduzione del testo.
 *
 * @param context Il contesto dell'applicazione.
 * @param onReady Callback invocato quando il motore TTS è pronto.
 */
class TtsService(
    private val context: Context,
    onReady: () -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false
    private var onReadyCallback: (() -> Unit)? = onReady
    private val ttsPreferences = TtsPreferences(context)

    /**
     * Viene chiamato quando l'inizializzazione del motore TTS è completata.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true
            Log.d("TtsService", "TTS Engine Initialized successfully.")
            val defaultLocale = Locale.ITALIAN
            val result = tts?.setLanguage(defaultLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsService", "Language not supported: $defaultLocale")
            }
            onReadyCallback?.invoke()
            onReadyCallback = null
        } else {
            Log.e("TtsService", "TTS Initialization failed with status: $status")
        }
    }

    /**
     * Legge ad alta voce il testo fornito usando una voce che corrisponde
     * al genere e alla lingua del personaggio.
     *
     * @param text Il testo da leggere.
     * @param character Il personaggio che sta parlando, usato per selezionare la voce.
     */
    fun speak(text: String, character: GameCharacter) {
        if (!isReady || tts == null) {
            Log.w("TtsService", "TTS not ready, cannot speak.")
            return
        }

        tts?.setSpeechRate(ttsPreferences.getSpeechRate())
        tts?.setPitch(ttsPreferences.getPitch())

        // --- NUOVA LOGICA DI SELEZIONE VOCE BASATA SUL GENERE ---
        // 1. Cerca la voce specifica scelta dall'utente per il genere del personaggio
        val preferredVoiceName = ttsPreferences.getVoiceForGender(character.gender)
        var voiceToUse = if (preferredVoiceName != null) {
            tts?.voices?.find { it.name == preferredVoiceName }
        } else {
            null
        }

        // 2. Se non c'è una preferenza per quel genere, prova a indovinarla (vecchia logica di fallback)
        if (voiceToUse == null) {
            voiceToUse = findVoiceByGenderKeyword(character)
        }

        if (voiceToUse != null) {
            tts?.voice = voiceToUse
            Log.d("TtsService", "Using voice: ${voiceToUse.name} for gender: ${character.gender}")
        } else {
            // 3. Fallback finale sulla lingua
            val locale = Locale(character.language)
            tts?.language = locale
            Log.w("TtsService", "Specific voice not found. Falling back to locale for gender: ${character.gender}")
        }
        // --- FINE NUOVA LOGICA ---

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun getAvailableVoices(): List<Voice> {
        if (!isReady || tts == null) return emptyList()
        return tts?.voices?.filter { it.locale.language == Locale.ITALIAN.language } ?: emptyList()
    }

    /**
     * Tenta di trovare una voce cercando keyword nel nome.
     * Usato come fallback se l'utente non ha fatto una scelta esplicita.
     */
    private fun findVoiceByGenderKeyword(character: GameCharacter): Voice? {
        val targetLocale = Locale(character.language)
        val targetGenderKeyword = if (character.gender.equals("MALE", ignoreCase = true)) "male" else "female"

        return tts?.voices?.filter { voice ->
            val localeMatches = voice.locale.language == targetLocale.language
            val genderMatches = voice.name.contains(targetGenderKeyword, ignoreCase = true)
            localeMatches && genderMatches
        }?.minByOrNull { it.latency }
    }

    /**
     * Rilascia le risorse utilizzate dal motore TTS.
     * Chiamare questo metodo quando il servizio non è più necessario (es. in onDestroy).
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        Log.d("TtsService", "TTS Engine shut down.")
    }
}
