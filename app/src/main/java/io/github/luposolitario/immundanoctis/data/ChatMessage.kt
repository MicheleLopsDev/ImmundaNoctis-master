package io.github.luposolitario.immundanoctis.data

import java.util.UUID
/**
 * La classe che rappresenta un singolo messaggio nella chat.
 * È stata aggiornata per usare l'ID del personaggio.
 * @param authorId L'ID del personaggio che ha scritto il messaggio. Questo campo collega il messaggio a un GameCharacter.
 * @param text Il contenuto effettivo del messaggio.
 */
 data class ChatMessage(
    val authorId: String,
    val text: String,
    val position: Long,
    val timestamp: Long = System.currentTimeMillis(),
    // Aggiunto un ID univoco per trovare e aggiornare il messaggio
    val id: String = UUID.randomUUID().toString(),
    // Campo per contenere il testo tradotto
    val translatedText: String? = null,
    // Flag per mostrare un indicatore di caricamento durante la traduzione
    val isTranslating: Boolean = false
)
