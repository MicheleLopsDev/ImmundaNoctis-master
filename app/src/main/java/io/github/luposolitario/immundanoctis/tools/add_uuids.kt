package io.github.luposolitario.immundanoctis.tools

// add_uuids.kt (file Kotlin standalone)

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.databind.SerializationFeature
import java.io.File
import java.util.UUID

// Copia qui le classi dati rilevanti da GameData.kt per questo script
// In un ambiente reale, faresti in modo che questo script potesse importarle direttamente
// o le copieresti in un modulo separato che dipenda da GameData.
enum class CharacterType { DM, PLAYER, NPC }
enum class ChallengeLevel { BASE, MEDIUM, ADVANCED, MASTER;
    companion object { fun fromString(level: String): ChallengeLevel? { return entries.firstOrNull { it.name.equals(level, ignoreCase = true) } } }
}

// Minimal data classes needed for parsing the JSON, including the UUIDs
data class LocalizedText(
    @JsonProperty("en") val en: String?,
    @JsonProperty("it") val it: String?
)

data class TagParameter(
    // Aggiungiamo un ID opzionale qui se vogliamo processarlo nel JSON
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("name") val name: String,
    @JsonProperty("description") val description: String?, // Assuming string for now
    @JsonProperty("default_value") val defaultValue: Any?,
    @JsonProperty("value") val value: Any? // Assuming string for now
)

data class TagConfig(
    @JsonProperty("id") val id: String, // Questo ID è già obbligatorio nel tuo JSON
    @JsonProperty("type") val type: String,
    @JsonProperty("regex") val regex: String,
    @JsonProperty("replacement") val replacement: String?,
    @JsonProperty("parameters") val parameters: List<TagParameter>?,
    @JsonProperty("actor") val actor: String,
    @JsonProperty("command") val command: String?,
    @JsonProperty("replace") val replace: Boolean
)

data class TagsConfigWrapper(
    @JsonProperty("tags") val tags: List<TagConfig>
)

fun main() {
    val inputFilePath = "C:\\DEV\\ImmundaNoctis-master\\app\\src\\main\\assets\\config.json" // Percorso del tuo file config.json
    val outputFilePath = "C:\\DEV\\ImmundaNoctis-master\\app\\src\\main\\assets\\config_with_uuids.json" // File di output

    val objectMapper = ObjectMapper()
        .registerModule(KotlinModule())
        .enable(SerializationFeature.INDENT_OUTPUT) // Per una formattazione leggibile
    // Potresti dover abilitare i commenti se il file originale li ha per evitare un errore di lettura iniziale
    // .configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS, true)


    try {
        val inputFile = File(inputFilePath)
        val wrapper: TagsConfigWrapper = objectMapper.readValue(inputFile)

        val updatedTags = wrapper.tags.map { tagConfig ->
            // Assicurati che l'ID del TagConfig esista sempre, altrimenti generane uno
            val updatedTagId = tagConfig.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

            val updatedParameters = tagConfig.parameters?.map { param ->
                // Aggiungi un UUID a ogni TagParameter se non ne ha già uno
                val updatedParamId = param.id.takeIf { !it.isNullOrBlank() } ?: UUID.randomUUID().toString()
                param.copy(id = updatedParamId)
            }

            tagConfig.copy(
                id = updatedTagId,
                parameters = updatedParameters
            )
        }

        val updatedWrapper = TagsConfigWrapper(tags = updatedTags)
        objectMapper.writeValue(File(outputFilePath), updatedWrapper)

        println("UUIDs aggiunti con successo a: $outputFilePath")
    } catch (e: Exception) {
        println("Errore durante l'aggiunta degli UUID: ${e.message}")
        e.printStackTrace()
    }
}