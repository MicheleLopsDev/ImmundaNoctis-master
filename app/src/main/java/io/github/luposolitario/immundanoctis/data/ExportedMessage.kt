package io.github.luposolitario.immundanoctis.data

/**
 * Rappresenta un singolo messaggio da salvare nel file JSON.
 * Contiene il nome dell'autore e il testo del messaggio.
 */
data class ExportedMessage(
    val author: String,
    val message: String
)
