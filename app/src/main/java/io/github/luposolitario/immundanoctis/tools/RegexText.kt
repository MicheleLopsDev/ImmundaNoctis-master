package io.github.luposolitario.immundanoctis.tools

// Non c'è la definizione di TagConfig qui, viene da add_uuids.kt

fun testRegex(processedString: String, tagConfig: TagConfig): List<MatchResult> {
    val regex = Regex(tagConfig.regex)
    val matches = regex.findAll(processedString).toList()
    return matches
}

// Esempio di utilizzo:
fun main() {
    // Esempio 1: TagConfig con tutti i parametri richiesti, incluso 'id'
    val tagConfig = TagConfig(
        id = "some_unique_id_1", // AGGIUNGI QUESTO! Puoi usare UUID.randomUUID().toString() se hai bisogno di un ID unico.
        type = "STAT_MOD",
        regex = "\\<STAT_MOD:([^:]+):([+-]?\\\\d+|[+-]?MAX_RESISTANCE_RESTORE)\\/>",
        replacement = "",
        parameters = listOf(),
        actor = "player",
        command = "modify",
        replace = false
    )
    val processedString = "As you step on a loose flagstone in the center of the corridor, you hear a loud *click*. Before you can react, dozens of poisoned darts shoot out from hidden holes in the walls, piercing your body. A searing pain overwhelms you as darkness claims your vision. <STAT_MOD:RESISTENZA:-100>"

    val matches = testRegex(processedString, tagConfig)

    if (matches.isNotEmpty()) {
        println("Trovati ${matches.size} match:")
        matches.forEach {
            println("Match: ${it.value} (Range: ${it.range})")
        }
    } else {
        println("Nessun match trovato.")
    }

    // Esempio 2: TagConfig per un caso senza match, incluso 'id'
    val tagConfigNoMatch = TagConfig(
        id = "some_unique_id_2", // AGGIUNGI QUESTO!
        type = "NO_MATCH",
        regex = "\\<STAT_MOD:([^:]+):([+-]?\\\\d+|[+-]?MAX_RESISTANCE_RESTORE)\\/>",
        replacement = "",
        parameters = listOf(),
        actor = "none",
        command = "none",
        replace = false
    )
    val processedStringNoMatch = "As you step on a loose flagstone in the center of the corridor, you hear a loud *click*. Before you can react, dozens of poisoned darts shoot out from hidden holes in the walls, piercing your body. A searing pain overwhelms you as darkness claims your vision. <STAT_MOD:RESISTENZA:-100>"
    val matchesNoMatch = testRegex(processedStringNoMatch, tagConfigNoMatch)

    if (matchesNoMatch.isNotEmpty()) {
        println("Trovati ${matchesNoMatch.size} match:")
        matchesNoMatch.forEach {
            println("Match: ${it.value} (Range: ${it.range})")
        }
    } else {
        println("Nessun match trovato per la seconda regex.")
    }
}