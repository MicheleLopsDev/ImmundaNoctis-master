package io.github.luposolitario.immundanoctis.engine

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Gestisce tutte le operazioni di traduzione usando ML Kit.
 * Ora supporta l'identificazione della lingua e la traduzione dinamica.
 */
class TranslationEngine {
    private val tag = "TranslationEngine"

    private var languageIdentifier: LanguageIdentifier = LanguageIdentification.getClient()
    private val translators = ConcurrentHashMap<String, Translator>()
    private val failedTranslatorModels: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    private val defaultTargetLanguage = TranslateLanguage.ITALIAN
    private val defaultSourceLanguage = TranslateLanguage.ENGLISH // Modello di default EN->IT

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()

    private suspend fun getTranslator(sourceLang: String, targetLang: String): Translator? {
        val key = "${sourceLang}_$targetLang"

        if (failedTranslatorModels.contains(key)) {
            Log.w(tag, "Modello per $sourceLang -> $targetLang precedentemente fallito. Non riprovo.")
            return null
        }

        val translator = translators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()
            Translation.getClient(options)
        }

        try {
            loadSpecificTranslatorModel(translator, sourceLang, targetLang)
            return translator
        } catch (e: Exception) {
            Log.e(tag, "Errore durante il caricamento del modello per $sourceLang -> $targetLang", e)
            translators.remove(key)
            failedTranslatorModels.add(key)
            return null
        }
    }

    private suspend fun loadSpecificTranslatorModel(translator: Translator, sourceLang: String, targetLang: String): Unit = suspendCancellableCoroutine { continuation ->
        Log.d(tag, "loadSpecificTranslatorModel(): Inizio download modello per $sourceLang -> $targetLang.")
        val conditions = DownloadConditions.Builder()
            .build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                Log.d(tag, "loadSpecificTranslatorModel(): SUCCESS! Modello $sourceLang -> $targetLang scaricato o già presente.")
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(tag, "loadSpecificTranslatorModel(): FAILURE! Errore download modello $sourceLang -> $targetLang.", exception)
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }
    }

    suspend fun loadModel(): Unit = suspendCancellableCoroutine { continuation ->
        Log.d(tag, "loadModel(): Inizio funzione. Scaricamento traduttore default.")
        val conditions = DownloadConditions.Builder()
            .build()

        languageIdentifier = LanguageIdentification.getClient()

        val defaultTranslatorOptions = TranslatorOptions.Builder()
            .setSourceLanguage(defaultSourceLanguage)
            .setTargetLanguage(defaultTargetLanguage)
            .build()
        val defaultTranslator = Translation.getClient(defaultTranslatorOptions)

        defaultTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                Log.d(tag, "loadModel(): SUCCESS! Modello traduttore default (${defaultSourceLanguage} -> ${defaultTargetLanguage}) scaricato o già presente.")
                translators[defaultSourceLanguage + "_" + defaultTargetLanguage] = defaultTranslator
                _isModelReady.value = true
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(tag, "loadModel(): FAILURE! Errore download modello traduttore default.", exception)
                _isModelReady.value = false
                failedTranslatorModels.add(defaultSourceLanguage + "_" + defaultTargetLanguage)
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }
    }

    private suspend fun identifyLanguage(text: String): String = suspendCancellableCoroutine { continuation ->
        if (text.isBlank()) {
            continuation.resume(defaultSourceLanguage)
            return@suspendCancellableCoroutine
        }
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                Log.d(tag, "Lingua identificata: $languageCode")
                if (languageCode == "und" || languageCode == "unk") { // Correzione per 'und' e 'unk'
                    Log.w(tag, "Lingua identificata come INDETERMINATA/SCONOSCIUTA. Fallback a ${defaultSourceLanguage}.")
                    continuation.resume(defaultSourceLanguage)
                } else {
                    continuation.resume(languageCode)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(tag, "Errore nell'identificazione della lingua. Fallback a ${defaultSourceLanguage}.", exception)
                continuation.resume(defaultSourceLanguage)
            }
    }

    /**
     * Traduce un testo, riconoscendo automaticamente la lingua di origine.
     * @param text Il testo da tradurre.
     * @param targetLang La lingua di destinazione (es. TranslateLanguage.ITALIAN).
     * @return Il testo tradotto, o il testo originale se la lingua non può essere identificata
     * o il modello specifico per la traduzione fallisce.
     * @throws IllegalStateException se il motore di traduzione non è inizializzato.
     */
    suspend fun translate(text: String, targetLang: String = defaultTargetLanguage): String {
        if (!_isModelReady.value) {
            Log.e(tag, "translate(): Motore di traduzione non inizializzato o modello default non caricato.")
            throw IllegalStateException("Motore di traduzione non inizializzato. Chiama loadModel() all'avvio dell'app.")
        }
        if (text.isBlank()) return ""

        val sourceLang = identifyLanguage(text)
        Log.d(tag, "Lingua di origine dopo fallback: $sourceLang")

        if (sourceLang == targetLang) {
            Log.d(tag, "Lingua di origine uguale a quella di destinazione. Nessuna traduzione necessaria.")
            return text
        }

        // NUOVO: Log per Spagnolo e Francese
        when (sourceLang) {
            TranslateLanguage.CHINESE -> Log.d(tag, "Identificata lingua Cinese (zh). Tentativo di traduzione.")
            TranslateLanguage.SPANISH -> Log.d(tag, "Identificata lingua Spagnola (es). Tentativo di traduzione.")
            TranslateLanguage.FRENCH -> Log.d(tag, "Identificata lingua Francese (fr). Tentativo di traduzione.")
            TranslateLanguage.DUTCH -> Log.d(tag, "Identificata lingua Tedesco (de). Tentativo di traduzione.")
            // Puoi aggiungere altri casi qui se necessario
            else -> Log.d(tag, "Identificata lingua '$sourceLang'. Tentativo di traduzione.")
        }

        val translator = getTranslator(sourceLang, targetLang)
        if (translator == null) {
            Log.e(tag, "Traduttore per $sourceLang -> $targetLang non disponibile. Restituisco testo originale.")
            return text
        }

        return suspendCancellableCoroutine { continuation ->
            translator.translate(text)
                .addOnSuccessListener { translatedText ->
                    Log.d(tag, "Testo tradotto da $sourceLang a $targetLang: $translatedText")
                    continuation.resume(translatedText)
                }
                .addOnFailureListener { exception ->
                    Log.e(tag, "Errore durante la traduzione da $sourceLang a $targetLang. Restituisco testo originale.", exception)
                    continuation.resumeWithException(exception)
                }
        }
    }

    /**
     * Rilascia le risorse di tutti i traduttori e dell'identificatore di lingua.
     * Resetta anche lo stato dei modelli falliti.
     */
    fun close() {
        languageIdentifier.close()
        translators.values.forEach { it.close() }
        translators.clear()
        failedTranslatorModels.clear()
        _isModelReady.value = false
        Log.d(tag, "TranslationEngine e tutti i traduttori chiusi.")
    }

    /**
     * Resetta lo stato di tutti i traduttori, forzando un potenziale nuovo download.
     * Utile in caso di modelli corrotti o fallimenti persistenti.
     */
    fun resetTranslators() {
        Log.i(tag, "Richiesto reset dei traduttori ML Kit.")
        close()
    }
}