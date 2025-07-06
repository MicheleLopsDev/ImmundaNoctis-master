// in file: java/io/github/luposolitario/immundanoctis/util/StringTagParser.kt

package io.github.luposolitario.immundanoctis.util

// ... (tutti gli import restano uguali) ...
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.luposolitario.immundanoctis.data.CharacterType
import io.github.luposolitario.immundanoctis.data.EngineCommand
import io.github.luposolitario.immundanoctis.data.TagConfig
import io.github.luposolitario.immundanoctis.data.TagsConfigWrapper
import java.io.InputStream
import java.util.Collections


class StringTagParser(context: android.content.Context) {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule())
    private var tagConfigurations: List<TagConfig> = emptyList()

    init {
        try {
            context.assets.open("config.json").use { inputStream ->
                val wrapper: TagsConfigWrapper = objectMapper.readValue(inputStream)
                tagConfigurations = wrapper.tags
            }
        } catch (e: Exception) {
            System.err.println("Errore nel caricamento di config.json: ${e.message}")
        }
    }

    /**
     * Metodo originale per la chat. Rimane invariato per non rompere la compatibilità.
     */
    fun parseAndReplaceWithCommands(
        inputString: String,
        currentActor: CharacterType? = null,
        lang: String = "en"
    ): Pair<String, List<EngineCommand>> {
        var processedString = inputString
        val foundCommands = mutableListOf<EngineCommand>()

        tagConfigurations.forEach { tagConfig ->
            if (currentActor != null && tagConfig.actor != "ANY" && tagConfig.actor != currentActor.name) {
                // Salta questo tag se non è per l'attore corrente
            } else {
                // **MODIFICA 1: Aggiunta opzione IGNORE_CASE per la regex**
                val regex = Regex(tagConfig.regex, RegexOption.IGNORE_CASE)
                val matches = regex.findAll(processedString).toList()

//                if (matches.isEmpty()){
//                    Log.d("StringTagParser", "Parsing tag ${tagConfig.id} failed: $processedString regEx: $regex  ")
//                }

                matches.forEach { matchResult ->
                    if (tagConfig.command != null) {
                        val commandParams = mutableMapOf<String, Any?>()
                        tagConfig.parameters?.forEach { paramConfig ->
                            var paramValueTemplate = paramConfig.value?.toString()
                            var finalParamValue: Any? = paramValueTemplate

                            if (paramValueTemplate != null && paramValueTemplate.contains("captured_value_from_regex")) {
                                // **MODIFICA 2: Corretta la regex per il placeholder**
                                val placeholderRegex = Regex("\\{captured_value_from_regex_(\\d+)\\}")
                                val placeholderMatch = placeholderRegex.find(paramValueTemplate)

                                if (placeholderMatch != null) {
                                    val groupIndex = placeholderMatch.groupValues[1].toInt()
                                    if (groupIndex < matchResult.groupValues.size) {
                                        finalParamValue = matchResult.groupValues[groupIndex]
                                    }
                                }
                            }
                            commandParams[paramConfig.name] = finalParamValue
                        }
                        foundCommands.add(EngineCommand(tagConfig.command, commandParams))
                    }
                }

                if (tagConfig.replace) {
                    processedString = regex.replace(processedString, "")
                }
            }
        }
        return Pair(processedString.trim(), foundCommands)
    }
    /**
     * NUOVO METODO: Specifico per la narrazione del librogame.
     * Pulisce il testo da tag e spazi e restituisce i comandi.
     */
    fun parseNarrationAndCreateCommands(rawResponse: String): Pair<String, List<EngineCommand>> {
        // Separa la narrazione dai tag delle scelte
        val narrativePart = rawResponse.split("--- TAGS ---").getOrElse(0) { "" }
        val choicesPart = rawResponse.split("--- TAGS ---").getOrElse(1) { "" }

        // Estrai i comandi da entrambe le parti
        val (_, gameCommands) = parseAndReplaceWithCommands(narrativePart)
        val (_, choiceCommands) = parseAndReplaceWithCommands(choicesPart)

        val allCommands = gameCommands + choiceCommands

        // Pulisci il testo narrativo da tutti i tag e gli spazi extra
        var cleanNarrative = narrativePart
        tagConfigurations.forEach { tagConfig ->
            if (tagConfig.replace) {
                cleanNarrative = cleanNarrative.replace(Regex(tagConfig.regex), "")
            }
        }

        // Rimuove i blocchi di codice e normalizza gli spazi
        cleanNarrative = cleanNarrative.replace("```xml", "").replace("```", "")
        cleanNarrative = cleanNarrative.lines().joinToString("\n") { it.trim() }.trim()

        return Pair(cleanNarrative, allCommands)
    }
}