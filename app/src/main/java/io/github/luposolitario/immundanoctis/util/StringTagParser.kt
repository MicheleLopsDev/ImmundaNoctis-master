package io.github.luposolitario.immundanoctis.util

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.luposolitario.immundanoctis.data.*
import java.io.InputStream
import java.util.Collections

class StringTagParser(private val context: Context) {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule())
    private var tagConfigurations: List<TagConfig> = emptyList()
    private val failedTranslatorModels: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    init {
        loadConfigurations()
    }

    private fun loadConfigurations() {
        var configStream: InputStream? = null
        try {
            configStream = context.assets.open("config.json")
            val wrapper: TagsConfigWrapper = objectMapper.readValue(configStream)
            tagConfigurations = wrapper.tags
        } catch (e: Exception) {
            System.err.println("Errore nel caricamento delle configurazioni dei tag da assets/config.json: ${e.message}")
            e.printStackTrace()
        } finally {
            configStream?.close()
        }
    }

// Dentro StringTagParser.kt

    fun parseAndReplaceWithCommands(inputString: String, currentActor: CharacterType? = null, lang: String = "en"): Pair<String, List<EngineCommand>> {
        var resultString = inputString
        val commands = mutableListOf<EngineCommand>()

        for (tagConfig in tagConfigurations) {
            // Filtro per attore
            if (currentActor != null && tagConfig.actor != "ANY" && tagConfig.actor != currentActor.name) {
                continue
            }

            val regex = Regex(tagConfig.regex)
            val matches = regex.findAll(resultString).toList()
            var tempResultString = resultString

            for (matchResult in matches) {
                // Se il tag deve essere sostituito, fallo subito
                if (tagConfig.replace) {
                    // Per i nostri nuovi tag, la sostituzione è con una stringa vuota
                    tempResultString = tempResultString.replace(matchResult.value, "")
                }

                // Se il tag genera un comando, crealo
                if (tagConfig.command != null) {
                    val commandParams = mutableMapOf<String, Any?>()

                    // Logica specifica per i nuovi comandi di traduzione
                    if ((tagConfig.id == "narrative_choice_translation" || tagConfig.id == "discipline_choice_translation") && matchResult.groupValues.size > 2) {
                        commandParams["id"] = matchResult.groupValues[1] // Cattura l'ID
                        commandParams["italianText"] = matchResult.groupValues[2] // Cattura il testo tradotto
                    }

                    // Aggiungi qui altra logica per altri tipi di comandi se necessario...

                    commands.add(EngineCommand(tagConfig.command, commandParams))
                }
            }
            // Aggiorna la stringa di risultato solo alla fine del ciclo interno
            resultString = tempResultString
        }
        // Rimuovi eventuali spazi bianchi o newline extra lasciati dalle sostituzioni
        return Pair(resultString.trim(), commands)
    }

    /**
     * Recupera una TagConfig specifica tramite il suo ID.
     * @param tagId L'ID del tag da cercare.
     * @return La TagConfig corrispondente, o null se non trovata.
     */
    fun getTagConfigById(tagId: String): TagConfig? {
        return tagConfigurations.find { it.id == tagId }
    }
}