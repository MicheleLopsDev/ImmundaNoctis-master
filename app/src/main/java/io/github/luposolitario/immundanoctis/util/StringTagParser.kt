// app/src/main/java/io/github/luposolitario.immundanoctis.util/StringTagParser.kt

package io.github.luposolitario.immundanoctis.util

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.luposolitario.immundanoctis.data.* // Importa tutte le classi dati da GameData.kt
import java.io.InputStream

class StringTagParser(private val context: Context) {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule())
    private var tagConfigurations: List<TagConfig> = emptyList()

    init {
        loadConfigurations()
    }

    private fun loadConfigurations() {
        var configStream: InputStream? = null
        try {
            configStream = context.assets.open("config.json") // Ora legge sempre "config.json"
            val wrapper: TagsConfigWrapper = objectMapper.readValue(configStream)
            tagConfigurations = wrapper.tags
        } catch (e: Exception) {
            System.err.println("Errore nel caricamento delle configurazioni dei tag da assets/config.json: ${e.message}")
            e.printStackTrace()
        } finally {
            configStream?.close()
        }
    }

    fun parseAndReplaceWithCommands(inputString: String, currentActor: CharacterType? = null): Pair<String, List<EngineCommand>> {
        var resultString = inputString
        val commands = mutableListOf<EngineCommand>()

        for (tagConfig in tagConfigurations) {
            if (currentActor != null && tagConfig.actor != currentActor.name) {
                continue
            }

            val regex = Regex(tagConfig.regex)
            val matches = regex.findAll(resultString).toList()

            for (matchResult in matches) {
                if (tagConfig.command != null) {
                    val commandParams = mutableMapOf<String, Any?>()

                    tagConfig.parameters?.forEach { param ->
                        // Qui i parametri sono ancora String/Any?, non LocalizedText
                        commandParams[param.name] = param.value ?: param.defaultValue
                    }

                    if (matchResult.groupValues.size > 1) {
                        commandParams["captured_value_from_regex"] = matchResult.groupValues[1]
                    }

                    commands.add(EngineCommand(tagConfig.command, commandParams))
                }
            }

            if (tagConfig.replace) {
                when (tagConfig.type) {
                    "text_substitution", "chatbot_greeting" -> {
                        resultString = resultString.replace(regex, tagConfig.replacement ?: "")
                    }
                    "prompt_description" -> {
                        resultString = regex.replace(resultString) { matchResult ->
                            val indexStr = matchResult.groupValues[1]
                            val index = indexStr.toIntOrNull() ?: 0
                            val paramName = "prompt_value_$index"
                            tagConfig.parameters?.firstOrNull { it.name == paramName }?.value?.toString() ?: ""
                        }
                    }
                    "dnd_environment_description" -> {
                        resultString = regex.replace(resultString) { matchResult ->
                            val environmentTypeRequested = matchResult.groupValues[1]
                            val paramName = "description_${environmentTypeRequested.lowercase()}"
                            tagConfig.parameters?.firstOrNull { it.name == paramName }?.value?.toString() ?: "Un ambiente generico non specificato."
                        }
                    }
                    "game_mechanic" -> {
                        resultString = regex.replace(resultString) { matchResult ->
                            val abilityName = tagConfig.regex.substringAfter("\\{").substringBefore("\\[")
                            val challengeLevelStr = matchResult.groupValues[1]
                            val challengeLevel = ChallengeLevel.fromString(challengeLevelStr)
                            val descriptionParamName = "${abilityName}_${challengeLevelStr.lowercase()}_desc"
                            val description = tagConfig.parameters?.firstOrNull { it.name == descriptionParamName }?.value?.toString()
                            "SFIDA_${abilityName.uppercase()}_${challengeLevel?.name ?: "UNKNOWN"}: ${description ?: "Descrizione della sfida mancante."}"
                        }
                    }
                    "trigger_audio", "generate_image", "trigger_graphic_effect" -> {
                        resultString = regex.replace(resultString, "")
                    }
                    else -> {
                        System.err.println("Tipo di tag sconosciuto: ${tagConfig.type} per ID: ${tagConfig.id}")
                    }
                }
            }
        }
        return Pair(resultString, commands)
    }
}