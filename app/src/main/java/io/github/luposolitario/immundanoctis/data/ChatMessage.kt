package io.github.luposolitario.immundanoctis.data

/**
 * La classe che rappresenta un singolo messaggio nella chat.
 * È stata aggiornata per usare l'ID del personaggio.
 * @param authorId L'ID del personaggio che ha scritto il messaggio. Questo campo collega il messaggio a un GameCharacter.
 * @param text Il contenuto effettivo del messaggio.
 */
data class ChatMessage(
    val authorId: String,
    val text: String
)