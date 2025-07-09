import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File

// --- Data Classes per mappare la struttura di config.json ---

data class TagsConfigWrapper(val tags: List<TagConfig>)

// MODIFICA: Aggiunta l'annotazione per ignorare le proprietà non conosciute
@JsonIgnoreProperties(ignoreUnknown = true)
data class TagConfig(
    val id: String,
    val type: String,
    val regex: String,
    val parameters: List<ParameterConfig>?,
    val command: String?,
    val replace: Boolean
)

data class ParameterConfig(
    val name: String,
    val value: String?
)

// --- Script di Validazione Principale ---

fun main() {
    // --- Percorsi Configurabili ---
    val configFilePath = "C:\\DEV\\ImmundaNoctis-master\\app\\src\\main\\assets\\config.json"
    val testFilePath = "C:\\DEV\\ImmundaNoctis-master\\app\\src\\main\\assets\\test-xml.json"
    // ----------------------------

    // Inizializza ObjectMapper per leggere i file JSON
    val objectMapper = ObjectMapper().registerModule(KotlinModule())

    // Carica le configurazioni dei tag dal file JSON aggiornato
    val tagConfigurations: List<TagConfig>
    try {
        val configFile = File(configFilePath)
        val wrapper: TagsConfigWrapper = objectMapper.readValue(configFile, TagsConfigWrapper::class.java)
        tagConfigurations = wrapper.tags
        println("✅ Caricato $configFilePath con ${tagConfigurations.size} configurazioni.")
    } catch (e: Exception) {
        println("❌ Errore nel caricamento di $configFilePath: ${e.message}")
        return
    }

    // Carica la lista di tag di test dal file JSON
    val testTags: List<String>
    try {
        val testFile = File(testFilePath)
        testTags = objectMapper.readValue(testFile, object : TypeReference<List<String>>() {})
        println("✅ Caricato $testFilePath con ${testTags.size} tag di test.")
    } catch (e: Exception) {
        println("❌ Errore nel caricamento di $testFilePath: ${e.message}")
        return
    }

    println("\n--- INIZIO VALIDAZIONE REGEX ---\n")

    var successfulMatches = 0
    var failedMatches = 0

    // Itera su ogni tag di test e prova a trovare una corrispondenza
    testTags.forEach { testTag ->
        var matchFound = false
        for (tagConfig in tagConfigurations) {
            // Usa l'opzione IGNORE_CASE come nello StringTagParser originale
            val regex = Regex(tagConfig.regex, RegexOption.IGNORE_CASE)
            val matchResult = regex.find(testTag)

            if (matchResult != null) {
                matchFound = true
                successfulMatches++
                println("---------------------------------")
                println("✅ SUCCESSO: Trovata corrispondenza per il tag:")
                println("   Tag: $testTag")
                println("   Config ID: ${tagConfig.id}")
                println("   Regex: ${tagConfig.regex}")

                // Estrai e stampa i parametri catturati
                if (tagConfig.parameters != null && tagConfig.parameters.isNotEmpty()) {
                    println("   Parametri Estratti:")
                    val placeholderRegex = Regex("\\{captured_value_from_regex_(\\d+)\\}")
                    tagConfig.parameters.forEach { paramConfig ->
                        val paramValueTemplate = paramConfig.value
                        var finalParamValue: String? = "N/A"

                        if (paramValueTemplate != null && paramValueTemplate.contains("captured_value_from_regex")) {
                            val placeholderMatch = placeholderRegex.find(paramValueTemplate)
                            if (placeholderMatch != null) {
                                val groupIndex = placeholderMatch.groupValues[1].toInt()
                                if (groupIndex < matchResult.groupValues.size) {
                                    finalParamValue = matchResult.groupValues[groupIndex]
                                }
                            }
                        }
                        // Stampa solo se il valore è stato catturato (non è vuoto o null)
                        if (!finalParamValue.isNullOrEmpty()) {
                            println("     - ${paramConfig.name}: \"$finalParamValue\"")
                        }
                    }
                }
                break // Esci dal ciclo interno una volta trovata la corrispondenza
            }
        }

        if (!matchFound) {
            failedMatches++
            println("---------------------------------")
            println("❌ FALLIMENTO: Nessuna regex ha corrisposto al tag:")
            println("   Tag: $testTag")
        }
    }

    println("\n--- FINE VALIDAZIONE ---")
    println("Totale Successi: $successfulMatches / ${testTags.size}")
    println("Totale Fallimenti: $failedMatches / ${testTags.size}")
    println("---------------------------------\n")
}
