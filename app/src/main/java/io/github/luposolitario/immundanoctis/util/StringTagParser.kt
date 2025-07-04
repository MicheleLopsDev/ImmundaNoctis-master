package io.github.luposolitario.immundanoctis.util

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.luposolitario.immundanoctis.data.CharacterType
import io.github.luposolitario.immundanoctis.data.EngineCommand
import io.github.luposolitario.immundanoctis.data.TagConfig
import io.github.luposolitario.immundanoctis.data.TagsConfigWrapper
import java.io.InputStream
import java.util.Collections

class StringTagParser(private val context: Context) {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule())
    private var tagConfigurations: List<TagConfig> = emptyList()
    private val failedTranslatorModels: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf())

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

    fun parseAndReplaceWithCommands(
        inputString: String,
        currentActor: CharacterType? = null,
        lang: String = "en"
    ): Pair<String, List<EngineCommand>> {
        var processedString = inputString
        val foundCommands = mutableListOf<EngineCommand>()

        tagConfigurations.forEach { tagConfig ->
            // Applica il filtro per l'attore, se specificato
            if (currentActor != null && tagConfig.actor != "ANY" && tagConfig.actor != currentActor.name) {
                // Salta questo tag se non è per l'attore corrente
            } else {
                val regex = Regex(tagConfig.regex)
                val matches = regex.findAll(processedString).toList()

                // *** INIZIO LOGICA CORRETTA ***
                // Itera su OGNI corrispondenza trovata per questo tag
                matches.forEach { matchResult ->
                    if (tagConfig.command != null) {
                        val commandParams = mutableMapOf<String, Any?>()

                        // Popola i parametri basandosi sui gruppi catturati dalla regex
                        tagConfig.parameters?.forEach { paramConfig ->
                            var paramValue: Any? = paramConfig.value?.toString()
                            if (paramValue is String) {
                                val placeholderRegex = Regex("\\{captured_value_from_regex_(\\d+)\\}")
                                placeholderRegex.findAll(paramValue).forEach { placeholderMatch ->
                                    val groupIndex = placeholderMatch.groupValues[1].toInt()
                                    if (groupIndex < matchResult.groupValues.size) {
                                        paramValue = matchResult.groupValues[groupIndex]
                                    }
                                }
                            }
                            commandParams[paramConfig.name] = paramValue
                        }

                        // Aggiungi il comando alla lista, uno per ogni match
                        foundCommands.add(EngineCommand(tagConfig.command, commandParams))
                    }
                }
                // *** FINE LOGICA CORRETTA ***

                // Se il tag deve essere rimosso dal testo, fallo ora
                if (tagConfig.replace) {
                    processedString = regex.replace(processedString, "")
                }
            }
        }

        // Restituisci il testo pulito e la lista completa dei comandi
        return Pair(processedString.trim(), foundCommands)
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