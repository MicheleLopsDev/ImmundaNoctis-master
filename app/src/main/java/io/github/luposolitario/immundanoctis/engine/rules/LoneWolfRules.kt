package io.github.luposolitario.immundanoctis.engine.rules

import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.LocalizedText
import io.github.luposolitario.immundanoctis.data.Scene
import io.github.luposolitario.immundanoctis.engine.CombatRoundResult
import io.github.luposolitario.immundanoctis.engine.GameRulesEngine

/**
 * Implementazione concreta di [GameRulesEngine] per le regole specifiche
 * dell'universo di Lupo Solitario.
 */
class LoneWolfRules : GameRulesEngine {

    override fun resolveCombatRound(player: GameCharacter, enemy: GameCharacter): CombatRoundResult {
        // Logica da implementare nella Fase 3
        TODO("Implementare il calcolo del Rapporto di Forza, il tiro casuale e la consultazione della Tabella di Combattimento.")

        // Esempio di valore di ritorno temporaneo
        return CombatRoundResult(
            playerDamage = 0,
            enemyDamage = 0,
            logMessage = LocalizedText(english = "Round result pending", italian = "Risultato del round in attesa")
        )
    }

    override fun canUseDiscipline(player: GameCharacter, disciplineId: String, scene: Scene): Boolean {
        // Il giocatore deve possedere la disciplina
        val playerHasDiscipline = player.kaiDisciplines.contains(disciplineId)
        if (!playerHasDiscipline) {
            return false
        }

        // La scena deve permettere l'uso di quella disciplina
        val sceneAllowsDiscipline = scene.disciplineChoices?.any { it.disciplineId == disciplineId } ?: false

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