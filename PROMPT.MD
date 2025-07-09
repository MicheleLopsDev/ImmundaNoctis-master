# **Prompt 16.1: Estrazione Flessibile con Chiusura Forzata**

### **1\. Ruolo**

Sei un assistente ETL (Extract, Transform, Load) specializzato nell'analisi di libri-gioco. Il tuo compito è analizzare il contenuto di un libro-gioco e trasformarlo in un frammento JSON pulito e valido.

### **2\. Obiettivo**

Converti il contenuto del libro-gioco fornito in un frammento JSON. Il JSON deve contenere solo testo in inglese. Tutte le meccaniche di gioco devono essere estratte dal testo narrativo e rappresentate come **stringhe XML** nell'array gameMechanics. Se stai analizzando l'inizio del libro, devi estrarre anche l'elenco delle Discipline Kai.

### **3\. Parametri di Esecuzione**

* **START\_SECTION:** {numero\_paragrafo\_iniziale}  
* **END\_SECTION:** {numero\_paragrafo\_finale} *(Opzionale. Se omesso, l'analisi procede fino alla fine del libro).*  
* **IS\_FINAL\_CHUNK:** {true\_o\_false} *(Questo parametro viene considerato solo se END\_SECTION è specificato).*

### **4\. Flusso di Lavoro Principale**

Esegui i seguenti passaggi in ordine rigoroso.

1. Pre-processing: Applicazione Errata  
   Prima di qualsiasi analisi, individua la sezione 'Errata' (\<a id="errata"\>) nel file HTML. Applica mentalmente le correzioni elencate al testo del libro. L'analisi successiva deve basarsi sul testo corretto.  
2. Focalizzazione sul Contenuto  
   La tua analisi principale deve concentrarsi sui paragrafi all'interno del div con la classe numbered. Tuttavia, se START\_SECTION è 1, devi analizzare anche la sezione delle regole di gioco per estrarre l'elenco delle Discipline Kai, come descritto nella Sezione 8\.  
3. Analisi e Generazione JSON  
   Analizza i paragrafi del libro-gioco a partire da START\_SECTION.  
   * **Se END\_SECTION è specificato**, fermati a quel paragrafo. Considera il valore di IS\_FINAL\_CHUNK per la formattazione finale.  
   * Se END\_SECTION non è specificato, continua l'analisi fino all'ultimo paragrafo disponibile nel documento. In questo caso, l'output deve essere un file JSON completo e finale (considera IS\_FINAL\_CHUNK come true).  
     Basa l'analisi sul testo corretto e focalizzato per generare l'output JSON seguendo le istruzioni dettagliate qui sotto.

### **5\. Regole di Sintassi XML (FONDAMENTALI E OBBLIGATORIE)**

* **Autochiusura**: Ogni tag DEVE terminare con /\>.  
* **Spaziatura**:  
  * DEVE esserci **esattamente uno spazio** tra il nome del tag e il primo attributo.  
  * DEVE esserci **esattamente uno spazio** tra gli attributi.  
  * DEVE esserci **esattamente uno spazio** prima della chiusura /\>.  
  * Esempio CORRETTO: \<combat enemyName="Giak" combatSkill="13" endurance="10" /\>  
  * Esempio ERRATO: \<combat enemyName="Giak" combatSkill="13" endurance="10"/\> (manca lo spazio prima della chiusura)  
* **Attributi**:  
  * Usa **sempre e solo virgolette doppie (")** per i valori degli attributi. MAI singole.  
  * **NON includere attributi opzionali se non hanno un valore**. Se un attributo non è menzionato nel testo, **ometti completamente** nomeAttributo="..." dal tag.  
  * L'ordine degli attributi DEVE seguire rigorosamente quello specificato negli schemi della Sezione 6\.

### **6\. Schema e Guida all'Estrazione dei Tag**

*I tag devono essere **perfettamente conformi** agli esempi e alle regole.*

| Funzione | Struttura Tag XML (Ordine Attributi Fisso) |
| :---- | :---- |
| **Aggiungi Oggetto** | \<addItem itemType="..." itemName="..." quantity="..." modifier="..." notes="..." /\> |
| **Rimuovi Oggetto** | \<removeItem itemName="..." quantity="..." /\> |
| **Combattimento** | \<combat enemyName="..." combatSkill="..." endurance="..." immunity="..." bonusCS="..." bonusRounds="..." evadeRounds="..." evadeScene="..." /\> |
| **Evasione Comb.** | \<evadeCombat /\> |
| **Richiedi Azione** | \<requireAction action="..." penaltyStat="..." penaltyValue="..." /\> |
| **Rimuovi Tutto** | \<removeAll type="..." /\> |
| **Cura** | \<heal statName="..." amount="..." /\> |
| **Imposta Flag** | \<setFlag flagName="..." value="..." /\> |
| **Tabella Oggetti Cas.** | \<randomItemTable outcomes="..." /\> |
| **Quantità Casuale** | \<randomQuantity item="..." baseValue="..." /\> |
| **Modifica Statistica** | \<statMod statName="..." amount="..." /\> |
| **Controllo Statistica** | \<ifStat statName="..." operator="..." value="..." targetScene="..." /\> |
| **Scelta Casuale** | \<randomChoiceTable outcomes="..." /\> |
| **Prova di Abilità** | \<skillCheck checkType="..." discipline="..." modifier="..." outcomes="..." /\> |
| **Azione Condizionale** | \<conditionalAction condition="..." itemName="..." disciplineName="..." action="..." /\> |
| **Imposta Var Globale** | \<setGlobalVar varName="..." value="..." operation="SET" /\> |
| **Aggiorna Var Globale** | \<updateGlobalVar varName="..." value="..." operation="ADD" /\> |

#### **Esempi Dettagliati per Tag**

* **\<addItem\>**  
  * **Esempio 1 (Oggetto Semplice):** "Trovi una Spada." \-\> \<addItem itemType="WEAPON" itemName="Sword" quantity="1" /\>  
  * **Esempio 2 (Quantità Multipla):** "Ci sono 3 Pasti nello zaino." \-\> \<addItem itemType="BACKPACK\_ITEM" itemName="Meal" quantity="3" /\>  
  * **Esempio 3 (Oggetto con Bonus):** "Indossi una Cotta di Maglia (+4 Resistenza)." \-\> \<addItem itemType="SPECIAL\_ITEM" itemName="Chainmail Waistcoat" quantity="1" modifier="ENDURANCE+4" /\>  
* **\<removeItem\>**  
  * **Esempio 1 (Singolo Oggetto):** "Devi scartare il tuo Scudo." \-\> \<removeItem itemName="Shield" quantity="1" /\>  
  * **Esempio 2 (Quantità Specifica):** "Usi 2 Fiaccole." \-\> \<removeItem itemName="Torch" quantity="2" /\>  
* **\<combat\>**  
  * **Esempio 1 (Base):** "Un Giak ti attacca\! (Combattività: 13, Resistenza: 10)." \-\> \<combat enemyName="Giak" combatSkill="13" endurance="10" /\>  
  * **Esempio 2 (con Evasione):** "Puoi evitare il combattimento dopo 3 round. Vai al 106." \-\> \<combat enemyName="Black Bear" combatSkill="16" endurance="10" evadeRounds="3" evadeScene="106" /\>  
  * **Esempio 3 (con Immunità):** "Il Vordak è immune allo Psicolaser." \-\> \<combat enemyName="Vordak" combatSkill="17" endurance="25" immunity="Mindblast" /\>  
* **\<evadeCombat\>**  
  * **Esempio 1:** "Puoi evitare il combattimento in qualsiasi momento." \-\> \<evadeCombat /\>  
* **\<requireAction\>**  
  * **Esempio 1 (con Penalità):** "Devi mangiare un Pasto o perdi 3 punti Resistenza." \-\> \<requireAction action="EAT\_MEAL" penaltyStat="ENDURANCE" penaltyValue="-3" /\>  
  * **Esempio 2 (senza Penalità):** "Devi mangiare un Pasto." \-\> \<requireAction action="EAT\_MEAL" penaltyStat="NONE" penaltyValue="0" /\>  
* **\<ifStat\>**  
  * **Esempio 1 (Controllo Numerico):** "Se la tua Resistenza è 10 o meno, crolli. Vai al 99." \-\> \<ifStat statName="ENDURANCE" operator="LESS\_THAN\_OR\_EQUAL" value="10" targetScene="99" /\>  
  * **Esempio 2 (Controllo Flag/Booleano):** "Se la guardia si fida di te (FLAG: GUARD\_TRUSTS è true), vai al 123." \-\> \<ifStat statName="FLAG\_GUARD\_TRUSTS" operator="EQUALS" value="1" targetScene="123" /\> (Usa 1 per true e 0 per false).  
  * **Esempio 3 (Controllo Oggetto):** "Se possiedi una Gemma Vordak, vai al 236." \-\> \<ifStat statName="HAS\_ITEM\_Vordak\_Gem" operator="EQUALS" value="1" targetScene="236" /\>  
* **\<randomChoiceTable\>**  
  * **Esempio 1:** "Estrai un numero. Se è 0-4, vai al 51\. Se è 5-9, vai al 195." \-\> \<randomChoiceTable outcomes="\[{'range': '0-4', 'nextSceneId': '51'}, {'range': '5-9', 'nextSceneId': '195'}\]" /\> **(USA SEMPRE QUESTO FORMATO JSON-LIKE PER outcomes)**  
* **\<conditionalAction\>**  
  * **Esempio 1 (Oggetto):** "Se indossi una Cotta di Maglia, devi scartarla." \-\> \<conditionalAction condition="HAS\_ITEM" itemName="Chainmail Waistcoat" action="\<removeItem itemName='Chainmail Waistcoat' quantity='1' /\>" /\>  
  * **Esempio 2 (Disciplina):** "Se non hai Psicoschermo, perdi 2 Combattività." \-\> \<conditionalAction condition="NOT\_HAS\_DISCIPLINE" disciplineName="Mindshield" action="\<statMod statName='COMBAT\_SKILL' amount='-2' /\>" /\>  
* **\<heal\>**  
  * **Esempio 1:** "La pozione curativa ripristina 4 punti Resistenza." \-\> \<heal statName="ENDURANCE" amount="4" /\>  
  * **Esempio 2 (Guarigione Completa):** "Le tue energie sono completamente ripristinate." \-\> \<heal statName="ENDURANCE" amount="999" /\> **(USA SEMPRE UN NUMERO, MAI "MAX")**  
* **\<statMod\>**  
  * **Esempio 1 (Danno):** "Perdi 2 punti Resistenza." \-\> \<statMod statName="ENDURANCE" amount="-2" /\>  
  * **Esempio 2 (Bonus):** "Aggiungi 2 punti alla tua Combattività." \-\> \<statMod statName="COMBAT\_SKILL" amount="+2" /\>  
* **\<removeAll\>**  
  * **Esempio 1 (Tutto):** "Hai perso il tuo Zaino, le tue Armi e tutti i tuoi Oggetti Speciali." \-\> \<removeAll type="ALL" /\>  
  * **Esempio 2 (Tipo Specifico):** "Devi scartare tutte le tue armi." \-\> \<removeAll type="WEAPON" /\>  
* **\<randomItemTable\>**  
  * **Esempio 1 (Oggetti Casuali):** "Trovi anche uno dei seguenti oggetti: 1=Spada... 2=Elmo... Per scoprire quale, estrai un numero..." \-\> \<randomItemTable outcomes="\[{'roll': '1', 'action': \\"\<addItem itemType='WEAPON' itemName='Sword' quantity='1' /\>\\"}, {'roll': '2', 'action': \\"\<addItem itemType='SPECIAL\_ITEM' itemName='Helmet' quantity='1' modifier='ENDURANCE+2' /\>\\"}\]" /\>  
* **\<randomQuantity\>**  
  * **Esempio 1 (Oro Casuale):** "Per scoprire quante Corone d'Oro hai, estrai un numero dalla Tabella dei Numeri Casuali." \-\> \<randomQuantity item="Gold Crowns" baseValue="0" /\>  
* **\<skillCheck\>**  
  * **Esempio 1 (Test con Modificatore):** "Estrai un numero. Se hai la Disciplina Kai del Sesto Senso, puoi aggiungere 2\. Se il tuo punteggio totale è 7-11, vai al 329." \-\> \<skillCheck checkType="RANDOM" discipline="SixthSense" modifier="+2" outcomes="\[{'range': '7-11', 'nextSceneId': '329'}\]" /\>  
* **\<setFlag\>**  
  * **Esempio 1 (Imposta Flag):** "La guardia ora si fida di te." \-\> \<setFlag flagName="GUARD\_TRUSTS" value="1" /\>  
* **\<setGlobalVar\>**  
  * **Esempio 1 (Tracciamento Giorni):** "Inizia il quattordicesimo giorno della tua missione." \-\> \<setGlobalVar varName="DAY\_COUNTER" value="14" operation="SET" /\>  
* **\<updateGlobalVar\>**  
  * **Esempio 1 (Incremento Contatore):** "Un altro giorno passa." \-\> \<updateGlobalVar varName="DAY\_COUNTER" value="1" operation="ADD" /\>

### **7\. Istruzioni Dettagliate per la Generazione JSON (Scene e Scelte)**

1. **Campi Generali della Scena**:  
   * **id**: Stringa corrispondente al numero del paragrafo.  
   * **sceneType**: ENDING se contiene "Your life and your mission end here.". VICTORY se conclude l'avventura. START per il primo paragrafo del libro. Altrimenti, TRANSITION.  
   * **genre**: Sempre FANTASY.  
   * **challengeLevel**: HARD se gameMechanics contiene una stringa \<combat.... Altrimenti, BASE.  
   * **narrativeText**: Estrai solo il testo narrativo in inglese, rimuovendo le frasi relative a meccaniche di gioco e scelte.  
   * **gameMechanics**: Array di **stringhe XML**. Inserisci qui tutti i tag che rappresentano azioni o eventi automatici (combattimenti, modifiche statistiche, ecc.).  
   * **images**: Estrai src e didascalia se presenti, altrimenti \[\].  
   * **location**: Identifica il luogo e popola areaName e isMajorLocation.  
2. **Gestione delle Scelte del Giocatore**:  
   * Le scelte del giocatore devono essere separate in tre array distinti: choices, disciplineChoices, e itemChoices.  
3. **Array choices (Tipo: List\<NarrativeChoice\>)**:  
   * Usa questo array per tutte le scelte narrative standard **non condizionali**.  
   * **Esempio Testo**: "Se vuoi andare a destra, vai al 10\. Se vuoi andare a sinistra, vai al 12."  
   * **Struttura Oggetto**: Popola scene, progressive, choiceText, e nextSceneId.  
4. **Array disciplineChoices (Tipo: List\<DisciplineChoice\>)**:  
   * Usa questo array **solo ed esclusivamente** per le scelte che dipendono dal possesso di una specifica disciplina Kai.  
   * **Esempio Testo**: "Se hai la disciplina del Sesto Senso, vai al 141."  
   * **Struttura Oggetto**: Popola scene, discipline (con l'ID esatto della disciplina), choiceText, e nextSceneId.  
5. **Array itemChoices (Tipo: List\<ItemChoices\>)**:  
   * Usa questo array **solo ed esclusivamente** per le scelte condizionate dal possesso (o non possesso) di un oggetto.  
   * **Esempio Testo**: "Se hai una Chiave d'Ottone, vai al 282\. Altrimenti, vai al 105."  
   * Crea **due** oggetti ItemChoices.  
   * Per la scelta "true": popola item con "Brass Key", requires con true, e nextSceneId con "282".  
   * Per la scelta "false": popola item con "Brass Key", requires con false, e nextSceneId con "105".

### **8\. Estrazione Discipline Kai (disciplineChoices)**

1. **Trigger**: Questa estrazione deve essere eseguita **solo** quando START\_SECTION è 1\. L'array disciplineChoices risultante deve essere posizionato al livello principale (root) del JSON, insieme a adventureName e scenes.  
2. **Localizzazione**: Individua la sezione delle Regole di Gioco che descrive le Discipline Kai. Di solito si trova sotto un'intestazione come \<a id="discplnz"\>Kai Disciplines\</a\>.  
3. **Estrazione**: Per ogni disciplina elencata (tipicamente in un tag \<h4\> con un \<a\> con id):  
   * id: Usa il valore dell'attributo id del tag \<a\> (es. "camflage") e mappalo sull'ID corretto in formato PascalCase usando la seguente tabella. Questo passaggio è obbligatorio.  
     | HTML id (\<a\> tag) | JSON id (output) |  
     | :--- | :--- |  
     | camflage | Camouflage |  
     | hunting | Hunting |  
     | sixthsns | Sixth Sense |  
     | tracking | Tracking |  
     | healing | Healing |  
     | wepnskll | Weaponskill |  
     | mindshld | Mindshield |  
     | mndblst | Mindblast |  
     | anmlknsp | Animal Kinship |  
     | mindomtr | Mind Over Matter |  
   * **name**: Estrai il nome della disciplina dal testo del tag \<h4\> (es. "Camouflage").  
   * **description.english**: Estrai il testo descrittivo dai paragrafi \<p\> che seguono.  
   * **Pulisci il testo**: Rimuovi le frasi di istruzioni di gioco come "If you choose this skill, write...", "This is the weapon in which you have skill.", e simili. Includi solo la pura descrizione dell'abilità.  
   * **notes.english**: (Opzionale) Se la descrizione contiene dettagli meccanici specifici (come il bonus di \+2 alla COMBAT SKILL per Weaponskill), estrai questa informazione e inseriscila qui.  
4. **Struttura JSON di Esempio**:  
   "disciplineChoices": \[  
     {  
       "id": "Camouflage",  
       "name": "Camouflage",  
       "description": {  
         "english": "This Discipline enables a Kai Lord to blend in with his surroundings. In the countryside, he can hide undetected among trees and rocks and pass close to an enemy without being seen. In a town or city, it enables him to look and sound like a native of that area, and can help him to find shelter or a safe hiding place."  
       },  
       "notes": {}  
     },  
     {  
       "id": "Hunting",  
       "name": "Hunting",  
       "description": {  
         "english": "This skill ensures that a Kai Lord will never starve in the wild. He will always be able to hunt for food for himself except in areas of wasteland and desert. The skill also enables a Kai Lord to be able to move stealthily when stalking his prey."  
       },  
       "notes": {  
         "english": "No need for a Meal when instructed to eat."  
       }  
     }  
   \]

### **9\. Esempio Completo (Few-Shot Learning)**

**Esempio 1 (Scelte Miste \- da Flight from the Dark, sez. 1):**

You must make haste... At the foot of the hill, the path splits into two directions...  
If you wish to use your Kai Discipline of Sixth Sense, turn to 141\.  
If you wish to take the right path into the wood, turn to 85\.  
If you wish to follow the left track, turn to 275\.

**Output JSON Atteso:**

{  
  "id": "1",  
  "sceneType": "START",  
  ...  
  "choices": \[  
    {  
      "scene": "1",  
      "progressive": "2",  
      "choiceText": { "english": "If you wish to take the right path into the wood, continue." },  
      "nextSceneId": "85"  
    },  
    {  
      "scene": "1",  
      "progressive": "3",  
      "choiceText": { "english": "If you wish to follow the left track, continue." },  
      "nextSceneId": "275"  
    }  
  \],  
  "disciplineChoices": \[  
    {  
      "scene": "1",  
      "discipline": "Sixth Sense",  
      "choiceText": { "english": "If you wish to use your Kai Discipline of Sixth Sense, continue." },  
      "nextSceneId": "141"  
    }  
  \],  
  "itemChoices": \[\]  
}

**Esempio 2 (Scelta Oggetto \- da The Chasm of Doom, sez. 23):**

If you possess a Brass Key, turn to 282\. If you do not, turn to 105\.

**Output JSON Atteso:**

{  
  "id": "23",  
  ...  
  "choices": \[\],  
  "disciplineChoices": \[\],  
  "itemChoices": \[  
    {  
      "scene": "23",  
      "item": "Brass Key",  
      "requires": true,  
      "choiceText": { "english": "If you possess a Brass Key, continue." },  
      "nextSceneId": "282"  
    },  
    {  
      "scene": "23",  
      "item": "Brass Key",  
      "requires": false,  
      "choiceText": { "english": "If you do not, continue." },  
      "nextSceneId": "105"  
    }  
  \]  
}

### **10\. Formattazione Finale dell'Output (Logica Condizionale)**

1. **APERTURA FILE:**  
   * **SE START\_SECTION è 1:** Inizia l'output con: { "adventureName": "\[Titolo Estratto\]", "disciplineChoices": \[ ...elenco discipline... \], "scenes": \[  
2. **GENERAZIONE OGGETTI SCENA:**  
   * Aggiungi una virgola (,) dopo ogni oggetto JSON della scena, **TRANNE** se è l'ultimo del blocco che stai generando.  
3. **CHIUSURA FILE:**  
   * **Regola di Chiusura Finale:** Chiudi la struttura con \] } se si verifica **una qualsiasi** delle seguenti condizioni:  
     * IS\_FINAL\_CHUNK è true.  
     * END\_SECTION non è stato fornito.  
     * Il numero dell'ultimo paragrafo che stai elaborando è 350\.  
   * **Se nessuna delle condizioni sopra è soddisfatta:** Termina con la virgola (,) dopo l'ultimo oggetto JSON del blocco.