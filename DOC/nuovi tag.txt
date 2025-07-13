\# Logica di Implementazione per i Nuovi Tag di Gioco

Questo documento descrive l'implementazione dettagliata dei nuovi tag di gioco, progettata per integrarsi nell'architettura esistente del \`MainViewModel\` e del \`GameStateManager\`. La logica seguente è pensata per essere inserita all'interno del blocco \`when (command.commandName)\` nella funzione \`processCommands\`.

\#\#\# \*\*Presupposto Architetturale\*\*

\* \*\*Stato del Giocatore:\*\* Le modifiche vengono applicate all'oggetto \`hero: GameCharacter\`, in particolare al suo campo \`details: HeroDetails\`.  
\* \*\*Comandi:\*\* Lo \`StringTagParser\` genera \`EngineCommand\` che vengono poi processati.  
\* \*\*Navigazione:\*\* La funzione \`navigateToScene(sceneId: String)\` viene invocata per cambiare scena.  
\* \*\*Feedback UI:\*\* \`\_uiFeedbackEvent.emit("...")\` viene usato per notificare l'utente di eventi di gioco.

\---

\#\# \*\*Implementazione dei Nuovi Tag\*\*

\#\#\# \*\*1. \`\<randomChoiceTable\>\`\*\*

\* \*\*Tag Esempio:\*\* \`\<randomChoiceTable outcomes="\[{'range': '0-4', 'nextSceneId': '108'}, {'range': '5-9', 'nextSceneId': '25'}\]" /\>\`  
\* \*\*Scopo:\*\* Gestire una ramificazione della storia basata su un tiro della Tabella del Numero Casuale.  
\* \*\*Logica di Implementazione:\*\*  
    1\.  Estrai il parametro \`outcomes\` (una stringa formattata come JSON) dal \`command\`.  
    2\.  Esegui un tiro casuale (es. \`Random.nextInt(0, 10)\`).  
    3\.  Deserializza la stringa \`outcomes\` in una lista di oggetti (es. \`data class RandomOutcome(val range: String, val nextSceneId: String)\`).  
    4\.  Itera sulla lista di \`outcomes\`. Per ogni \`outcome\`:  
        \* Fai il parsing del \`range\` (es. "0-4") per ottenere un valore minimo e massimo.  
        \* Controlla se il numero estratto rientra in quel range.  
    5\.  Se viene trovata una corrispondenza, invoca \`navigateToScene(matchedOutcome.nextSceneId)\`.  
    6\.  Gestisci il caso in cui nessun range corrisponda (un log di errore sarebbe appropriato).

\* \*\*Esempio di Codice Kotlin (nel \`when\` di \`processCommands\`):\*\*  
    \`\`\`kotlin  
    "handleRandomChoice" \-\> {  
        val outcomesJson \= command.parameters\["outcomes"\] as? String  
        if (outcomesJson \!= null) {  
            val roll \= Random.nextInt(0, 10\) // o la tua funzione per il tiro  
            log("🎲 Risultato del tiro casuale: $roll")  
              
            // Deserializza gli outcomes e trova la corrispondenza  
            val outcomeType \= object : TypeToken\<List\<Map\<String, String\>\>\>() {}.type  
            val outcomesList: List\<Map\<String, String\>\> \= Gson().fromJson(outcomesJson, outcomeType)  
              
            val targetSceneId \= outcomesList.find {  
                val rangeParts \= it\["range"\]?.split('-')  
                if (rangeParts?.size \== 2\) {  
                    val min \= rangeParts\[0\].toIntOrNull()  
                    val max \= rangeParts\[1\].toIntOrNull()  
                    if (min \!= null && max \!= null) {  
                        roll in min..max  
                    } else false  
                } else false  
            }?.get("nextSceneId")

            if (targetSceneId \!= null) {  
                navigateToScene(targetSceneId)  
            } else {  
                log("❌ ERRORE: Nessun outcome trovato per il tiro $roll")  
            }  
        }  
    }  
    \`\`\`

\---

\#\#\# \*\*2. Scelte Basate su Oggetti (Logica di Scena)\*\*

\* \*\*Scopo:\*\* Abilitare o disabilitare scelte narrative in base agli oggetti posseduti dal giocatore.  
\* \*\*Logica di Implementazione:\*\* Questa logica \*\*non\*\* appartiene a \`processCommands\`. Appartiene invece alla funzione che prepara le scelte visualizzabili per una data scena, probabilmente \`populateChoicesForCurrentScene\` nel tuo \`MainViewModel\`.  
    1\.  Aggiungi un nuovo array \`itemChoices\` alla tua data class \`Scene\` (come suggerito nel prompt aggiornato).  
        \`\`\`kotlin  
        data class ItemChoice(val scene: String, val item: String, val nextSceneId: String, val requires: Boolean)  
        \`\`\`  
    2\.  In \`populateChoicesForCurrentScene\`, dopo aver filtrato le \`disciplineChoices\`, fai lo stesso per \`itemChoices\`.  
    3\.  Per ogni \`itemChoice\` nella scena corrente:  
        \* Recupera l'inventario del giocatore (\`hero.details.inventory\` e \`hero.details.specialItems\`).  
        \* Controlla se l'oggetto \`itemChoice.item\` è presente.  
        \* Confronta il risultato con il booleano \`itemChoice.requires\`.  
        \* Se la condizione è soddisfatta, aggiungi la scelta alla lista di scelte attive da mostrare all'utente.

\---

\#\#\# \*\*3. \`\<skillCheck\>\`\*\*

\* \*\*Tag Esempio:\*\* \`\<skillCheck checkType="RANDOM\_NUMBER\_WITH\_MODIFIER" discipline="Hunting" modifier="+2" outcomes="\[{'range': '0-5', 'nextSceneId': '333'}\]" /\>\`  
\* \*\*Scopo:\*\* Gestire una prova di abilità il cui esito determina la navigazione.  
\* \*\*Logica di Implementazione:\*\*  
    1\.  Estrai i parametri: \`checkType\`, \`discipline\`, \`modifier\`, \`outcomes\`.  
    2\.  Esegui un tiro casuale (\`Random.nextInt(0, 10)\`).  
    3\.  Controlla se \`discipline\` è fornito. Se sì, verifica se il giocatore possiede tale disciplina in \`hero.details.specialAbilities\`.  
    4\.  Se la disciplina è posseduta, applica il \`modifier\` al risultato del tiro.  
    5\.  Procedi come per \`randomChoiceTable\`: fai il parsing di \`outcomes\` e invoca \`navigateToScene\` con il \`nextSceneId\` corrispondente al range del risultato finale.

\* \*\*Esempio di Codice Kotlin:\*\*  
    \`\`\`kotlin  
    "handleSkillCheck" \-\> {  
        val discipline \= command.parameters\["discipline"\] as? String  
        val modifier \= (command.parameters\["modifier"\] as? String)?.toIntOrNull() ?: 0  
        val outcomesJson \= command.parameters\["outcomes"\] as? String

        if (outcomesJson \!= null) {  
            var roll \= Random.nextInt(0, 10\)  
            log("🎲 Prova di abilità: Tiro base \= $roll")

            if (discipline \!= null && hero.details?.specialAbilities?.contains(discipline) \== true) {  
                roll \+= modifier  
                log("✨ Bonus disciplina '$discipline' applicato: \+$modifier. Tiro finale \= $roll")  
            }  
              
            // Logica di parsing outcomes e navigazione (identica a handleRandomChoice)  
            // ...  
        }  
    }  
    \`\`\`

\---

\#\#\# \*\*4. \`\<conditionalAction\>\`\*\*

\* \*\*Scopo:\*\* Eseguire un'azione nidificata (un altro tag) solo se una condizione è soddisfatta.  
\* \*\*Logica di Implementazione:\*\*  
    1\.  Estrai i parametri: \`condition\`, \`itemName\`, \`action\`.  
    2\.  Valuta la \`condition\` usando un blocco \`when\`.  
        \* Se \`condition\` è "HAS\_ITEM", controlla se \`hero.details.inventory\` o \`specialItems\` contiene \`itemName\`.  
        \* Aggiungi altri tipi di condizioni se necessario (es. "FLAG\_IS\_SET").  
    3\.  Se la condizione è vera:  
        \* Prendi la stringa \`action\` (es. \`"\<removeItem itemName='...' /\>"\`).  
        \* Usa la tua istanza di \`StringTagParser\` per processare questa singola stringa: \`stringTagParser.parseAndReplaceWithCommands(action, ...)\`  
        \* Questo genererà una nuova lista di \`EngineCommand\`.  
        \* Aggiungi questi nuovi comandi alla lista \`newCommandsToProcess\` per essere eseguiti in un ciclo successivo, evitando modifiche concorrenti.

\* \*\*Esempio di Codice Kotlin:\*\*  
    \`\`\`kotlin  
    "handleConditionalAction" \-\> {  
        val condition \= command.parameters\["condition"\] as? String  
        val itemName \= command.parameters\["itemName"\] as? String  
        val actionString \= command.parameters\["action"\] as? String

        if (condition \!= null && actionString \!= null) {  
            var conditionMet \= false  
            when (condition) {  
                "HAS\_ITEM" \-\> {  
                    if (itemName \!= null) {  
                        conditionMet \= hero.details?.inventory?.any { it.name \== itemName } \== true ||  
                                       hero.details?.specialItems?.contains(itemName) \== true  
                    }  
                }  
                // Aggiungi altri casi per 'HAS\_DISCIPLINE', 'FLAG\_IS\_SET', etc.  
            }

            if (conditionMet) {  
                log("✅ Condizione '$condition' verificata. Esecuzione azione nidificata.")  
                val (\_, nestedCommands) \= stringTagParser.parseAndReplaceWithCommands(actionString, CharacterType.DM)  
                newCommandsToProcess.addAll(nestedCommands)  
            } else {  
                log("ℹ️ Condizione '$condition' non verificata. Nessuna azione.")  
            }  
        }  
    }  
    \`\`\`

\---

\#\#\# \*\*5. \`\<setGlobalVar\>\` / \`\<updateGlobalVar\>\`\*\*

\* \*\*Scopo:\*\* Gestire lo stato persistente a livello di avventura.  
\* \*\*Nota:\*\* La tua \`SessionData\` non sembra avere una mappa per le variabili globali. Dovresti aggiungerla:  
    \`\`\`kotlin  
    data class SessionData(  
        // ... altri campi  
        val globalVariables: MutableMap\<String, Any\> \= mutableMapOf()  
    )  
    \`\`\`  
\* \*\*Logica di Implementazione:\*\*  
    1\.  Accedi alla mappa \`globalVariables\` dalla sessione corrente.  
    2\.  \*\*\`setGlobalVar\`\*\*: Imposta o sovrascrivi un valore. \`session.globalVariables\[varName\] \= value\`.  
    3\.  \*\*\`updateGlobalVar\`\*\*: Modifica un valore esistente. Recupera il valore, assicurati che sia numerico, applica l'operazione (es. \`ADD\`) e salvalo di nuovo.

\* \*\*Esempio di Codice Kotlin:\*\*  
    \`\`\`kotlin  
    "setGlobalVar" \-\> {  
        val varName \= command.parameters\["varName"\] as? String  
        val value \= command.parameters\["value"\] // Può essere String o Int  
        val operation \= command.parameters\["operation"\] as? String  
        if (varName \!= null && value \!= null && operation \== "SET") {  
            currentSession.globalVariables\[varName\] \= value  
            log("GLOBAL VAR: Impostata '$varName' a '$value'")  
            sessionModified \= true  
        }  
    }

    "updateGlobalVar" \-\> {  
        val varName \= command.parameters\["varName"\] as? String  
        val valueChange \= (command.parameters\["value"\] as? String)?.toIntOrNull()  
        val operation \= command.parameters\["operation"\] as? String  
        if (varName \!= null && valueChange \!= null && operation \== "ADD") {  
            val currentValue \= currentSession.globalVariables\[varName\] as? Int ?: 0  
            currentSession.globalVariables\[varName\] \= currentValue \+ valueChange  
            log("GLOBAL VAR: Aggiornata '$varName'. Nuovo valore: ${currentValue \+ valueChange}")  
            sessionModified \= true  
        }  
    }  
    \`\`\`

(consiglio giro di test RUN o DEBUG).  
