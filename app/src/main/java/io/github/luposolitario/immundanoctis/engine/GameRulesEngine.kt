package io.github.luposolitario.immundanoctis.engine

import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.LocalizedText
import io.github.luposolitario.immundanoctis.data.Scene

/**
 * Contenitore per il risultato di un singolo round di combattimento.
 * Contiene i danni inflitti e un messaggio di log da mostrare all'utente.
 */
data class CombatRoundResult(
    val playerDamage: Int,
    val enemyDamage: Int,
    val logMessage: LocalizedText
)

/**
 * Interfaccia che definisce il "contratto" per un sistema di regole di gioco.
 * Qualsiasi set di regole (Lupo Solitario, D&D, ecc.) dovrà implementare questa interfaccia
 * per poter essere utilizzato dal MainViewModel.
 */
interface GameRulesEngine {

    /**
     * Calcola il risultato di un singolo round di combattimento.
     *
     * @param player Il personaggio del giocatore.
     * @param enemy Il personaggio nemico.
     * @return Un oggetto [CombatRoundResult] con l'esito del round.
     */
    fun resolveCombatRound(player: GameCharacter, enemy: GameCharacter): CombatRoundResult

    /**
     * Verifica se una specifica disciplina può essere utilizzata nella scena corrente.
     *
     * @param player Il personaggio del giocatore.
     * @param disciplineId L'ID della disciplina da controllare (es. "SIXTH_SENSE").
     * @param scene La scena attuale.
     * @return `true` se la disciplina è utilizzabile, `false` altrimenti.
     */
    fun canUseDiscipline(player: GameCharacter, disciplineId: String, scene: Scene): Boolean
}