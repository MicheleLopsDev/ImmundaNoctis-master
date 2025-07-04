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

// Dentro StringTagParser.kt

// Dentro StringTagParser.kt

// Dentro StringTagParser.kt

    // Dentro StringTagParser.kt

    fun parseAndReplaceWithCommands(inputString: String, currentActor: CharacterType? = null, lang: String = "en"): Pair<String, List<EngineCommand>> {
        var processedString = inputString
        val foundCommands = mutableListOf<EngineCommand>()

        // Itera su ogni REGOLA definita nel tuo config.json
        tagConfigurations.forEach { tagConfig ->
            val regex = Regex(tagConfig.regex)
            val matches = regex.findAll(processedString).toList()

            // Se troviamo delle corrispondenze per questa regola...
            if (matches.isNotEmpty()) {

                // Per ogni corrispondenza trovata, creiamo il comando associato
                matches.forEach { matchResult ->
                    if (tagConfig.command != null) {
                        val commandParams = mutableMapOf<String, Any?>()
                        val commandName = tagConfig.command

                        // Usiamo un "when" per gestire i diversi tipi di comando in modo esplicito
                        when (commandName) {
                            "updateChoiceText", "updateDisciplineChoiceText" -> {
                                if (matchResult.groupValues.size > 2) {
                                    commandParams["id"] = matchResult.groupValues[1]
                                    commandParams["italianText"] = matchResult.groupValues[2]
                                    foundCommands.add(EngineCommand(commandName, commandParams))
                                }
                            }
                            "addItem" -> {
                                if (matchResult.groupValues.size > 3) {
                                    commandParams["itemName"] = matchResult.groupValues[1]
                                    commandParams["itemType"] = matchResult.groupValues[2]
                                    commandParams["quantity"] = matchResult.groupValues[3].toIntOrNull() ?: 1
                                    foundCommands.add(EngineCommand(commandName, commandParams))
                                }
                            }
                            "addGold" -> {
                                if (matchResult.groupValues.size > 1) {
                                    commandParams["amount"] = matchResult.groupValues[1].toIntOrNull() ?: 0
                                    foundCommands.add(EngineCommand(commandName, commandParams))
                                }
                            }
                            // Aggiungeremo qui altri comandi (es. applyStatModifier) in futuro
                        }
                    }
                }

                // Alla fine, se la regola lo prevede, puliamo TUTTE le occorrenze di quel tag dal testo
                if (tagConfig.replace) {
                    processedString = regex.replace(processedString, "")
                }
            }
        }

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