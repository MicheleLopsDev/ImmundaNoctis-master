package io.github.luposolitario.immundanoctis.data

/**
 * Rappresenta l'intero stato salvabile di una sessione di gioco.
 * Contiene il nome della campagna, la data dell'ultimo aggiornamento
 * e la lista completa di tutti i personaggi.
 */
data class SessionData(
    val sessionName: String,
    val lastUpdate: Long,
    val characters: List<GameCharacter>
)
