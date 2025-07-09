# **Manuale di Progettazione delle Meccaniche di Gioco**

## **1\. Introduzione e Architettura di Riferimento**

Questo documento serve come guida tecnica per l'analisi, la revisione e l'implementazione delle meccaniche di gioco della serie Lupo Solitario. La logica descritta è basata sull'architettura esistente, che prevede:

* **GameData.kt**: Definisce le strutture dati fondamentali che rappresentano lo stato del gioco (GameCharacter, GameItem, StatModifier, SessionData, ecc.).  
* GameStateManager.kt: Un singleton che gestisce la persistenza (caricamento/salvataggio) della SessionData, implementando una strategia di caching per l'efficienza.  
* GameLogicManager.kt: Un singleton che gestisce il caricamento e l'accesso alle scene dell'avventura da un file JSON, fungendo da "content provider".  
* **StringTagParser.kt**: Analizza il testo delle scene, identifica i tag XML tramite espressioni regolari e genera una lista di EngineCommand.  
* **MainViewModel.kt**: Funge da orchestratore centrale. Riceve ed esegue gli EngineCommand attraverso un blocco when nella funzione processCommands, modificando lo stato del gioco.  
* CharacterSheetViewModel.kt: Gestisce la logica di visualizzazione e interazione con la scheda del personaggio, incluso il calcolo delle statistiche effettive basate sui StatModifier.  
* GameRulesEngine.kt **/** LoneWolfRules.kt: Definisce un'interfaccia per le regole di gioco (es. combattimento, utilizzo discipline), mantenendo la logica di gioco separata dalla UI.

L'obiettivo è garantire che ogni meccanica di gioco sia mappata in modo coerente e robusto all'interno di questa architettura.

## **2\. Analisi dei Tag Esistenti e Raffinamenti**

Questa sezione analizza i tag la cui logica è già parzialmente o completamente implementata, suggerendo possibili raffinamenti.

### **Gestione Inventario**

#### **\<addItem\> /** \<removeItem\> **/** \<removeAll\>

* **Scopo:** Aggiungere o rimuovere oggetti dall'inventario.  
* **Analisi Logica:** La tua implementazione è molto solida. MainViewModel gestisce l'aggiunta/rimozione, mentre CharacterSheetViewModel si occupa di aggiornare la UI e i modificatori associati (come visto in discardItem). La gestione dell'inventario pieno (\_inventoryFullState) è una soluzione eccellente.  
* **Considerazioni e Raffinamenti:**  
  * **Rimozione Condizionale:** La meccanica "Devi scartare lo Scudo per arrampicarti" non è gestita. La soluzione migliore non è modificare questi tag, ma usare il tag \<conditionalAction\> (descritto nella Sezione 3), che si integra perfettamente con la tua architettura a comandi.

### **Modifica Statistiche e Flag**

#### **\<statMod\> / \<heal\>**

* **Scopo:** Modificare una statistica del giocatore (Combattività, Resistenza).  
* **Analisi Logica:** La logica in MainViewModel che aggiorna direttamente le statistiche del GameCharacter è funzionale. Il tuo sistema di StatModifier in CharacterSheetViewModel è ancora più potente e dovrebbe diventare il meccanismo primario per tutte le modifiche alle statistiche.  
* **Suggerimento di Raffinamento:** Centralizzare tutta la logica di modifica delle statistiche tramite i StatModifier. Il comando \<statMod\> nel MainViewModel non dovrebbe modificare direttamente hero.stats, ma dovrebbe creare e aggiungere un StatModifier alla lista hero.details.activeModifiers. La funzione calculateEffectiveCombatSkill del CharacterSheetViewModel farà il resto. Questo rende gli effetti tracciabili e reversibili (es. per bonus/malus temporanei).  
  * **Esempio:** \<statMod statName="ENDURANCE" amount="-2" /\> dovrebbe creare un StatModifier(statName="RESISTENZA", amount=-2, sourceType=ModifierSourceType.EVENT, duration=ModifierDuration.PERMANENT).

#### **\<setFlag\>**

* **Scopo:** Impostare una variabile booleana (flag) per tracciare eventi della storia.  
* **Analisi Logica:** L'implementazione che modifica la mappa gameFlags in HeroDetails è corretta, scalabile e gestita correttamente dal GameLogicManager.

### **Logica di Navigazione Condizionale**

#### **\<ifStat\>**

* **Scopo:** Indirizzare il giocatore a una scena diversa in base al valore di una statistica.  
* **Analisi Logica:** La logica in checkStatAndJump è corretta per i confronti numerici con le statistiche base.  
* **Considerazioni e Raffinamenti:**  
  * **Confronto con Statistiche Effettive:** Il controllo dovrebbe essere eseguito sulle statistiche *effettive* (computedStats), non su quelle base, per tenere conto di bonus e malus attivi. La funzione checkStatAndJump dovrebbe prima calcolare il valore effettivo della statistica richiesta prima di eseguire il confronto.  
  * **Controllo di Flag:** Propongo di creare un tag dedicato \<ifFlag flagName="GUARD\_TRUSTS" value="true" targetScene="123" /\> per mantenere la logica pulita e separata. L'implementazione sarebbe simile a ifStat, ma controllerebbe hero.details.gameFlags.

### **Azioni Obbligatorie**

#### **\<requireAction\>**

* **Scopo:** Forzare il giocatore a compiere un'azione, con una penalità in caso di fallimento.  
* **Analisi Logica:** Attualmente gestisce bene il caso EAT\_MEAL.  
* **Considerazioni e Raffinamenti:**  
  * **Generalizzazione:** Per renderlo più flessibile, l'attributo action potrebbe diventare un'enumerazione (ActionType.EAT\_MEAL, ActionType.USE\_ITEM, ActionType.DISCARD\_ITEM). Il when in processCommands potrebbe quindi gestire diversi tipi di azioni richieste, ad esempio mostrando una UI specifica che chiede al giocatore di selezionare un oggetto da usare/scartare.

## **3\. Analisi delle Meccaniche Mancanti e Logica di Implementazione**

Questa sezione descrive le meccaniche di gioco non ancora coperte e come implementarle all'interno della tua architettura.

#### **A. Scelte Basate su Tiri Casuali**

* **Meccanica:** Una delle meccaniche di navigazione più comuni. "Estrai un numero dalla Tabella. Se è 0-4, vai a X. Se è 5-9, vai a Y".  
* **Tag Proposto:** \<randomChoiceTable outcomes="\[{'range': '0-4', 'nextSceneId': '108'}, {'range': '5-9', 'nextSceneId': '25'}\]" /\>  
* **Logica di Implementazione (MainViewModel):**  
  1. Aggiungere un case per handleRandomChoice in processCommands.  
  2. La funzione estrae un numero casuale (0-9).  
  3. Deserializza la stringa JSON outcomes in una lista di oggetti Outcome(range: String, nextSceneId: String).  
  4. Trova l'outcome il cui range contiene il numero estratto e invoca navigateToScene() con il nextSceneId corrispondente.

#### **B. Scelte Basate sul Possesso di Oggetti**

* **Meccanica:** Mostrare una scelta solo se il giocatore possiede (o non possiede) un oggetto.  
* **Logica di Implementazione (in** MainViewModel.populateChoicesForCurrentScene**):**  
  1. **Modifica Struttura Dati:** Aggiungi i campi requiredItem: String? e itemCondition: Boolean? (es. true per "deve possedere") alla tua data class NarrativeChoice in GameData.kt.  
  2. **Aggiorna Logica di Filtro:** In populateChoicesForCurrentScene, estendi il filtro filter per controllare anche queste nuove condizioni, verificando la presenza di requiredItem in hero.details.inventory e hero.details.specialItems. Questo approccio è pulito e centralizza la logica di visualizzazione delle scelte.

#### **C. Prove di Abilità (Skill Checks)**

* **Meccanica:** L'esito di un'azione (saltare, schivare) dipende da un tiro casuale, spesso modificato da una Disciplina.  
* **Tag Proposto:** \<skillCheck checkType="RANDOM\_NUMBER\_WITH\_MODIFIER" discipline="Hunting" modifier="+2" outcomes="\[...\]" /\>  
* **Logica di Implementazione (MainViewModel):**  
  1. Aggiungere un case per handleSkillCheck in processCommands.  
  2. La logica è un'estensione di handleRandomChoice: dopo aver estratto il numero casuale, verifica se il giocatore possiede la discipline specificata. Se sì, applica il modifier al tiro.  
  3. Usa il risultato finale per determinare la scena successiva dagli outcomes.

#### **D. Gestione di Variabili Globali (Timer e Contatori)**

* **Meccanica:** Tracciare informazioni che persistono tra le scene (es. i 40 giorni in *Fire on the Water*).  
* **Tag Proposti:** \<setGlobalVar varName="questTimer" value="40" /\> e \<updateGlobalVar varName="questTimer" value="-1" operation="ADD" /\>.  
* **Logica di Implementazione:**  
  1. **Modifica Strutturale:** Aggiungi val globalVariables: MutableMap\<String, Any\> \= mutableMapOf() alla tua data class SessionData.  
  2. **Implementazione in processCommands**: Aggiungi i case per setGlobalVar e updateGlobalVar che modificano la mappa currentSession.globalVariables e poi salvano la sessione tramite gameStateManager.

## **4\. Sistema di Combattimento \- Analisi e Percorso di Progettazione**

Dato che il combattimento è ancora da implementare, ecco un percorso di progettazione che si integra con la tua architettura.

1. **Avvio del Combattimento (**MainViewModel**)**  
   * Quando viene processato un tag \<combat\>, il MainViewModel dovrebbe creare un oggetto di stato per il combattimento (es. data class CombatState(val enemy: GameCharacter, ...)).  
   * Dovrebbe calcolare la **Combattività Effettiva** iniziale per il giocatore e per il nemico, considerando tutti i StatModifier attivi.  
   * Dovrebbe avviare un ciclo di combattimento, probabilmente gestito da una nuova funzione executeCombatRound().  
2. **Logica del Round di Combattimento (**LoneWolfRules.kt**)**  
   * La funzione resolveCombatRound (attualmente TODO) è il posto giusto per la logica pura del round.  
   * **Input:** Riceve il player e l'enemy con le loro statistiche effettive.  
   * Passi:  
     a. Calcola il Rapporto di Forza (player.effectiveCombatSkill \- enemy.effectiveCombatSkill).  
     b. Estrae un numero dalla Tabella del Numero Casuale.  
     c. Consulta la Tabella dei Risultati di Combattimento (che dovrebbe essere una struttura dati statica, magari in LoneWolfRules.kt stesso).  
     d. Determina i danni per entrambi (es. playerDamage, enemyDamage).  
     e. Gestione Effetti Speciali: Qui dentro, verifica se il giocatore ha la Sommerswerd e se il nemico è di tipo UNDEAD per raddoppiare enemyDamage.  
   * **Output:** Ritorna un oggetto CombatRoundResult con i danni calcolati.  
3. **Gestione del Risultato del Round (**MainViewModel**)**  
   * La funzione executeCombatRound() riceve il CombatRoundResult.  
   * Applica i danni a entrambi i personaggi aggiornando la loro Resistenza.  
   * Controlla se uno dei due combattenti ha Resistenza \<= 0.  
   * Se il combattimento continua, attende l'input del giocatore per il round successivo.  
   * Se il combattimento finisce, gestisce l'esito (morte, vittoria, loot).  
4. **Gestione dell'Esito del Combattimento**  
   * **Vittoria:** Per gestire cosa succede dopo una vittoria (es. "Se vinci, vai alla sezione X"), si può usare un tag specifico come \<onCombatWin targetScene="X" /\> o \<onCombatWin action="\<addItem...\>" /\>. Questo comando verrebbe processato solo dopo che il MainViewModel ha determinato la vittoria del giocatore.  
   * **Morte:** Se hero.effectiveEndurance \<= 0, imposta lo stato \_isHeroDead.value \= true.

Questa architettura mantiene una chiara separazione delle responsabilità: il ViewModel gestisce il flusso e lo stato, mentre GameRulesEngine incapsula le regole specifiche del gioco.

(consiglio giro di test RUN o DEBUG).