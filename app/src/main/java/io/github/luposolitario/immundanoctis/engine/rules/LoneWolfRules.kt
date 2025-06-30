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
        return CombatRoundResult(0, 0, LocalizedText(en = "Round result pending", it = "Risultato del round in attesa"))
    }

    override fun canUseDiscipline(player: GameCharacter, disciplineId: String, scene: Scene): Boolean {
        // Logica da implementare nella Fase 5
        TODO("Implementare il controllo: il giocatore ha la disciplina E la scena la abilita in 'disciplineChoices'?")

        // Esempio di valore di ritorno temporaneo
        return false
    }
}