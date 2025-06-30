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

    fun parseAndReplaceWithCommands(inputString: String, currentActor: CharacterType? = null, lang: String = "en"): Pair<String, List<EngineCommand>> {
        var resultString = inputString
        val commands = mutableListOf<EngineCommand>()

        for (tagConfig in tagConfigurations) {
            if (currentActor != null && tagConfig.actor != "ANY" && tagConfig.actor != currentActor.name) {
                continue
            }

            val regex = Regex(tagConfig.regex)
            val matches = regex.findAll(resultString).toList()

            for (matchResult in matches) {
                if (tagConfig.command != null) {
                    val commandParams = mutableMapOf<String, Any?>()

                    tagConfig.parameters?.forEach { param ->
                        commandParams[param.name] = param.value ?: param.defaultValue
                    }

                    if (matchResult.groupValues.size > 1) {
                        commandParams["captured_value_from_regex"] = matchResult.groupValues[1]
                    }

                    when (tagConfig.type) {
                        "gameMechanic" -> { // Convertito a camelCase
                            val abilityName = tagConfig.regex.substringAfter("\\{").substringBefore("\\[")
                            commandParams["abilityType"] = abilityName // Convertito a camelCase
                        }
                        "directionalChoice" -> { // Convertito a camelCase
                            commandParams["nextSceneId"] = matchResult.groupValues[1] // Convertito a camelCase
                        }
                    }

                    commands.add(EngineCommand(tagConfig.command, commandParams))
                }
            }

            if (tagConfig.replace) {
                when (tagConfig.type) {
                    "textSubstitution", "chatBotSaluto" -> { // Convertito a camelCase
                        // Inside your loop, where 'regex' is defined
                        val localizedReplacement = tagConfig.replacement?.get(lang) ?: tagConfig.replacement?.get("en") ?: ""
                        resultString = resultString.replace(regex, localizedReplacement)
                    }
                    "promptDescription" -> { // Convertito a camelCase
                        resultString = regex.replace(resultString) { matchResult ->
                            val indexStr = matchResult.groupValues[1]
                            val index = indexStr.toIntOrNull() ?: 0
                            val paramName = "promptValue$index" // Convertito a camelCase
                            tagConfig.parameters?.firstOrNull { it.name == paramName }?.value?.toString() ?: ""
                        }
                    }
                    "dndEnvironmentDescription" -> { // Convertito a camelCase
                        resultString = regex.replace(resultString) { matchResult ->
                            val environmentTypeRequested = matchResult.groupValues[1]
                            val paramName = "description_${environmentTypeRequested.lowercase()}"
                            tagConfig.parameters?.firstOrNull { it.name == paramName }?.value?.toString() ?: "Un ambiente generico non specificato."
                        }
                    }
                    "gameMechanic" -> { // Convertito a camelCase
                        resultString = regex.replace(resultString) { matchResult ->
                            val abilityName = tagConfig.regex.substringAfter("\\{").substringBefore("\\[")
                            val challengeLevelStr = matchResult.groupValues[1]
                            val challengeLevel = ChallengeLevel.fromString(challengeLevelStr)
                            val descriptionParamName = "${abilityName}BaseDesc" // Esempio, dovrai allinearlo ai nomi nel JSON
                            val description = tagConfig.parameters?.firstOrNull { it.name == descriptionParamName }?.value?.toString()
                            "SFIDA_${abilityName.uppercase()}_${challengeLevel?.name ?: "UNKNOWN"}: ${description ?: "Descrizione della sfida mancante."}"
                        }
                    }
                    "triggerAudio", "generateImage", "triggerGraphicEffect", // Convertito a camelCase
                    "directionalChoice" -> { // Convertito a camelCase
                        resultString = regex.replace(resultString, "")
                    }
                    else -> {
                        System.err.println("Tipo di tag sconosciuto: ${tagConfig.type} per ID: ${tagConfig.id}")
                    }
                }
            } else {
                // Se replace è false, il tag viene lasciato intatto nel testo.
            }
        }
        return Pair(resultString, commands)
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