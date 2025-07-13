package io.github.luposolitario.immundanoctis.engine.rules

import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.LocalizedText
import io.github.luposolitario.immundanoctis.data.Scene
import io.github.luposolitario.immundanoctis.engine.CombatRoundResult
import io.github.luposolitario.immundanoctis.engine.GameRulesEngine
import kotlin.random.Random

/**
 * Implementazione concreta di [GameRulesEngine] per le regole specifiche
 * dell'universo di Lupo Solitario.
 */
class LoneWolfRules : GameRulesEngine {

    // Inserisci questo companion object all'interno della classe LoneWolfRules

    companion object {
        private const val KILL_DAMAGE = 999 // Valore per rappresentare un'uccisione istantanea

        // Tabella dei Risultati di Combattimento di Lupo Solitario
        // Mappa<Rapporto di Forza, Lista<Pair<Danno Giocatore, Danno Nemico>>>
        // L'indice della lista corrisponde al tiro del dado da 0 a 10 (il tiro '0' della tabella è l'indice 9 qui).
        val COMBAT_RESULTS_CHART: Map<Int, List<Pair<Int, Int>>> = mapOf(
            -10 to listOf(Pair(9, 0), Pair(8, 0), Pair(7, 0), Pair(6, 0), Pair(5, 0), Pair(4, 0), Pair(3, 0), Pair(2, 0), Pair(1, 0), Pair(0, 0)),
            -9 to listOf(Pair(8, 0), Pair(7, 0), Pair(6, 0), Pair(5, 0), Pair(4, 0), Pair(3, 0), Pair(2, 0), Pair(1, 0), Pair(0, 0), Pair(0, 0)),
            -8 to listOf(Pair(7, 0), Pair(6, 0), Pair(5, 0), Pair(4, 0), Pair(3, 0), Pair(2, 0), Pair(1, 0), Pair(0, 0), Pair(0, 0), Pair(0, 0)),
            -7 to listOf(Pair(6, 0), Pair(5, 0), Pair(4, 0), Pair(3, 0), Pair(2, 0), Pair(1, 0), Pair(0, 0), Pair(0, 0), Pair(0, 0), Pair(0, 0)),
            -6 to listOf(Pair(6, 1), Pair(5, 2), Pair(4, 3), Pair(3, 4), Pair(2, 5), Pair(1, 6), Pair(0, 7), Pair(0, 8), Pair(0, 9), Pair(0, 10)),
            -5 to listOf(Pair(5, 2), Pair(4, 3), Pair(3, 4), Pair(2, 5), Pair(1, 6), Pair(0, 7), Pair(0, 8), Pair(0, 9), Pair(0, 10), Pair(0, 11)),
            -4 to listOf(Pair(5, 3), Pair(4, 4), Pair(3, 5), Pair(2, 6), Pair(1, 7), Pair(0, 8), Pair(0, 9), Pair(0, 10), Pair(0, 11), Pair(0, 12)),
            -3 to listOf(Pair(4, 3), Pair(3, 4), Pair(2, 5), Pair(1, 6), Pair(0, 7), Pair(0, 8), Pair(0, 9), Pair(0, 10), Pair(0, 11), Pair(0, 12)),
            -2 to listOf(Pair(4, 4), Pair(3, 5), Pair(2, 6), Pair(1, 7), Pair(0, 8), Pair(0, 9), Pair(0, 10), Pair(0, 11), Pair(0, 12), Pair(0, 14)),
            -1 to listOf(Pair(3, 4), Pair(2, 5), Pair(1, 6), Pair(0, 7), Pair(0, 8), Pair(0, 9), Pair(0, 10), Pair(0, 11), Pair(0, 12), Pair(0, 14)),
            0 to listOf(Pair(3, 5), Pair(2, 6), Pair(1, 7), Pair(0, 8), Pair(0, 9), Pair(0, 10), Pair(0, 11), Pair(0, 12), Pair(0, 14), Pair(0, 16)),
            1 to listOf(Pair(4, 5), Pair(3, 6), Pair(2, 7), Pair(1, 8), Pair(0, 9), Pair(0, 10), Pair(0, 11), Pair(0, 12), Pair(0, 14), Pair(0, 16)),
            2 to listOf(Pair(4, 5), Pair(3, 6), Pair(2, 7), Pair(1, 8), Pair(0, 9), Pair(0, 10), Pair(0, 11), Pair(0, 12), Pair(0, 14), Pair(0, 16)),
            3 to listOf(Pair(5, 6), Pair(4, 7), Pair(3, 8), Pair(2, 9), Pair(1, 10), Pair(0, 11), Pair(0, 12), Pair(0, 14), Pair(0, 16), Pair(0, 18)),
            4 to listOf(Pair(5, 6), Pair(4, 7), Pair(3, 8), Pair(2, 9), Pair(1, 10), Pair(0, 11), Pair(0, 12), Pair(0, 14), Pair(0, 16), Pair(0, 18)),
            5 to listOf(Pair(6, 7), Pair(5, 8), Pair(4, 9), Pair(3, 10), Pair(2, 11), Pair(1, 12), Pair(0, 14), Pair(0, 16), Pair(0, 18), Pair(0, KILL_DAMAGE)),
            6 to listOf(Pair(6, 8), Pair(5, 9), Pair(4, 10), Pair(3, 11), Pair(2, 12), Pair(1, 14), Pair(0, 16), Pair(0, 18), Pair(0, KILL_DAMAGE), Pair(0, KILL_DAMAGE)),
            7 to listOf(Pair(7, 9), Pair(6, 10), Pair(5, 11), Pair(4, 12), Pair(3, 14), Pair(2, 16), Pair(1, 18), Pair(0, KILL_DAMAGE), Pair(0, KILL_DAMAGE), Pair(0, KILL_DAMAGE)),
            8 to listOf(Pair(8, 10), Pair(7, 11), Pair(6, 12), Pair(5, 14), Pair(4, 16), Pair(3, 18), Pair(0, KILL_DAMAGE), Pair(0, KILL_DAMAGE), Pair(0, KILL_DAMAGE), Pair(0, KILL_DAMAGE)),
            9 to listOf(Pair(9, 11), Pair(8, 12), Pair(7, 14), Pair(6, 16), Pair(5, 18), Pair(0, KILL_DAMAGE), Pair(0, KILL_DAMAGE), Pair(0, KILL_DAMAGE), Pair(0, KILL_DAMAGE), Pair(0, KILL_DAMAGE)),
            10 to listOf(Pair(10, 12), Pair(9, 14), Pair(8, 16), Pair(7, 18), Pair(0, KILL_DAMAGE), Pair(0, KILL_DAMAGE), Pair(0, KILL_DAMAGE), Pair(0, KILL_DAMAGE), Pair(0, KILL_DAMAGE), Pair(0, KILL_DAMAGE))
        )
    }


    override fun resolveCombatRound(player: GameCharacter, enemy: GameCharacter): CombatRoundResult {
        // 1. Calcola la Combattività Effettiva (CS)
        val playerCS = (player.stats?.combattivita ?: 0) + (player.details?.activeModifiers?.filter { it.statName == "COMBATTIVITA" }?.sumOf { it.amount } ?: 0)
        val enemyCS = (enemy.stats?.combattivita ?: 0) + (enemy.details?.activeModifiers?.filter { it.statName == "COMBATTIVITA" }?.sumOf { it.amount } ?: 0)

        // 2. Calcola il Rapporto di Forza
        val combatRatio = playerCS - enemyCS

        // 3. Esegui il tiro casuale (0-9)
        val roll = Random.nextInt(0, 10)

        // 4. Trova la riga corretta nella tabella, gestendo valori estremi
        val ratioKey = combatRatio.coerceIn(COMBAT_RESULTS_CHART.keys.minOrNull() ?: 0, COMBAT_RESULTS_CHART.keys.maxOrNull() ?: 0)
        val resultsRow = COMBAT_RESULTS_CHART[ratioKey]

        if (resultsRow == null || roll >= resultsRow.size) {
            return CombatRoundResult(0, 0, LocalizedText("Errore nella tabella di combattimento", "Combat chart error"))
        }

        // 5. Determina i danni in base al tiro
        // L'indice del tiro corrisponde direttamente all'indice della lista. Il tiro '0' della tabella è l'ultimo elemento.
        val tableRollIndex = if (roll == 0) 9 else roll - 1
        val (playerDamage, enemyDamage) = resultsRow[tableRollIndex]

        // 6. Restituisci il risultato
        val logMessage = LocalizedText(
            english = "Combat Ratio: $combatRatio. Roll: $roll. Player loses $playerDamage EP, Enemy loses $enemyDamage EP.",
            italian = "Rapporto Forza: $combatRatio. Tiro: $roll. Perdi $playerDamage RES, Nemico perde $enemyDamage RES."
        )

        return CombatRoundResult(playerDamage, enemyDamage, logMessage)
    }

    override fun canUseDiscipline(player: GameCharacter, disciplineId: String, scene: Scene): Boolean {
        // Il giocatore deve possedere la disciplina
        val playerHasDiscipline = player.kaiDisciplines.contains(disciplineId)
        if (!playerHasDiscipline) {
            return false
        }

        // La scena deve permettere l'uso di quella disciplina
        val sceneAllowsDiscipline = scene.disciplineChoices?.any { it.discipline == disciplineId } ?: false

        return sceneAllowsDiscipline
    }

    override fun getKaiRank(disciplineCount: Int): String {
        return when (disciplineCount) {
            in 0..4 -> "Novizio Kai"
            5 -> "Iniziato Kai"
            6 -> "Discepolo Kai"
            7 -> "Viandante Kai"
            8 -> "Guerriero Kai"
            9 -> "Maestro Kai"
            10 -> "Gran Maestro Kai"
            else -> "Gran Maestro Kai Supremo"
        }
    }
}