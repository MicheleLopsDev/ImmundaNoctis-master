package io.github.luposolitario.immundanoctis.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import io.github.luposolitario.immundanoctis.data.GameCharacter
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
    context: Context,
    onReady: () -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false
    private var onReadyCallback: (() -> Unit)? = onReady

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

        val voice = findBestVoiceForCharacter(character)
        if (voice != null) {
            tts?.voice = voice
            Log.d("TtsService", "Using voice: ${voice.name} for lang: ${voice.locale}")
        } else {
            val locale = Locale(character.language)
            tts?.language = locale
            Log.w("TtsService", "Specific voice not found. Falling back to locale: $locale")
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    /**
     * Cerca la voce migliore disponibile che corrisponda al genere e alla lingua del personaggio.
     * Questo metodo è stato reso più compatibile controllando il nome della voce invece della
     * proprietà 'gender'.
     *
     * @param character Il personaggio per cui trovare la voce.
     * @return L'oggetto Voice migliore, o null se non viene trovata una corrispondenza.
     */
    private fun findBestVoiceForCharacter(character: GameCharacter): Voice? {
        val targetLocale = Locale(character.language)
        // Usiamo una keyword per cercare il genere nel nome della voce (es. "it-it-x-ita-local#male_1-local")
        val targetGenderKeyword = if (character.gender.equals("MALE", ignoreCase = true)) "male" else "female"

        return tts?.voices?.filter { voice ->
            // Controlla che la lingua corrisponda
            val localeMatches = voice.locale.language == targetLocale.language
            // Controlla che il nome della voce contenga la keyword del genere (male/female)
            val genderMatches = voice.name.contains(targetGenderKeyword, ignoreCase = true)

            localeMatches && genderMatches
        }?.minByOrNull {
            // Criterio di preferenza (es. minore latenza, maggiore qualità)
            it.latency
        }
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
