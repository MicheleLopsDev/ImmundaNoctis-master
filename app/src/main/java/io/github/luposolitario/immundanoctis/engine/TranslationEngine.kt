package io.github.luposolitario.immundanoctis.engine

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Gestisce tutte le operazioni di traduzione usando ML Kit.
 */
class TranslationEngine {
    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.ITALIAN)
        .build()

    private val englishItalianTranslator: Translator = Translation.getClient(options)
    private val tag = "TranslationEngine"

    init {
        downloadModelIfNeeded()
    }

    private fun downloadModelIfNeeded() {
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        englishItalianTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                Log.d(tag, "Modello di lingua scaricato o già presente.")
            }
            .addOnFailureListener { exception ->
                Log.e(tag, "Errore durante il download del modello di lingua.", exception)
            }
    }

    /**
     * Traduce un testo da inglese a italiano in modo asincrono.
     * @param text Il testo da tradurre.
     * @return Il testo tradotto.
     * @throws Exception in caso di errore di traduzione.
     */
    suspend fun translate(text: String): String = suspendCancellableCoroutine { continuation ->
        englishItalianTranslator.translate(text)
            .addOnSuccessListener { translatedText ->
                continuation.resume(translatedText)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }

    /**
     * Rilascia le risorse del traduttore. Da chiamare quando il ViewModel viene distrutto.
     */
    fun close() {
        englishItalianTranslator.close()
    }
}
