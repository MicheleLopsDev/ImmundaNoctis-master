# **Guida Completa ai Tag di Gioco per Lupo Solitario**

Questo documento riassume tutti i tag di gioco \<.../\> che abbiamo definito per trasformare i libri-gioco di Lupo Solitario in un formato JSON strutturato. Ogni tag è progettato per essere inserito nell'array gameMechanics di una scena e serve a comunicare una regola specifica all'applicazione.

### **Tag per la Gestione di Oggetti e Statistiche**

| Meccanica di Gioco | Tag XML | Scopo del Tag | Esempio dal Libro |
| :---- | :---- | :---- | :---- |
| **Aggiungere un Oggetto** | \<ADD\_ITEM TYPE="..." NAME="..." QUANTITY="..." /\> | Aggiunge uno o più oggetti all'inventario del giocatore. TYPE può essere WEAPON, SPECIAL\_ITEM, BACKPACK\_ITEM, GOLD, MEAL. | **Testo:** "Trovate anche una Spada, 12 Corone d'Oro e cibo sufficiente per 2 Pasti." (da *The Chasm of Doom*, Sez. 2)\<br\>**Tag Generati:**\<br\> \<ADD\_ITEM TYPE="WEAPON" NAME="Sword" QUANTITY="1" /\>\<br\> \<ADD\_ITEM TYPE="GOLD" NAME="Gold Crown" QUANTITY="12" /\>\<br\> \<ADD\_ITEM TYPE="MEAL" NAME="Meal" QUANTITY="2" /\> |
| **Rimuovere un Oggetto** | \<REMOVE\_ITEM NAME="..." QUANTITY="..." /\> | Rimuove un oggetto specifico dall'inventario. QUANTITY può essere un numero o ALL. | **Testo:** "Devi scartare un oggetto prima di entrare in città. Se vuoi scartare il tuo Scudo, vai al 169." (da *The Chasm of Doom*, Sez. 8, ipotizzando che il giocatore scelga di farlo)\<br\>**Tag Generato:** \<REMOVE\_ITEM NAME="Shield" QUANTITY="1" /\> |
| **Rimuovere Tutto** | \<REMOVE\_ALL TYPE="..." /\> | Rimuove tutti gli oggetti di un certo tipo, come WEAPONS\_AND\_BACKPACK. | **Testo:** "Devi lasciare tutte le tue Armi e gli Oggetti dello Zaino..." (da *Fire on the Water*, Sez. 205)\<br\>**Tag Generato:** \<REMOVE\_ALL TYPE="WEAPONS\_AND\_BACKPACK" /\> |
| **Modificare Statistica** | \<STAT\_MOD STAT="..." VALUE="..." /\> | Modifica una statistica del giocatore. STAT può essere ENDURANCE o COMBAT SKILL. VALUE può essere un numero (-3, \+4) o FULL per un ripristino completo. | **Testo:** "Perdi 3 punti di RESISTENZA." (da *Flight from the Dark*, Sez. 144)\<br\>**Tag Generato:** \<STAT\_MOD STAT="ENDURANCE" VALUE="-3" /\> |
| **Curare** | \<HEAL STAT="..." VALUE="..." /\> | Ripristina punti a una statistica, tipicamente RESISTENZA, tramite pozioni o abilità. | **Testo:** "Questa pozione ripristina 4 punti di RESISTENZA." (da *The Caverns of Kalte*, Equipaggiamento)\<br\>**Tag Generato:** \<HEAL STAT="ENDURANCE" VALUE="4" /\> |

### **Tag per il Combattimento e Azioni**

| Meccanica di Gioco | Tag XML | Scopo del Tag | Esempio dal Libro |
| :---- | :---- | :---- | :---- |
| **Iniziare Combattimento** | \<COMBAT ENEMY="..." CS="..." EP="..." IMMUNITY="..." /\> | Inizia un combattimento. ENEMY è il nome del nemico, CS la sua Combattività, EP la sua Resistenza. IMMUNITY è opzionale. | **Testo:** "Vordak: COMBATTIVITÀ 17, RESISTENZA 25\. È immune al Mindblast." (da *Flight from the Dark*, Sez. 29)\<br\>**Tag Generato:** \<COMBAT ENEMY="Vordak" CS="17" EP="25" IMMUNITY="Mindblast" /\> |
| **Evasione dal Combattimento** | \<EVADE\_COMBAT /\> | Segnala che una scelta permette di fuggire dal combattimento, applicando la penalità di un round di danno solo al giocatore. | **Testo:** "Puoi evadere il combattimento in qualsiasi momento..." (da *The Chasm of Doom*, Sez. 14)\<br\>**Tag Generato (in una choice):** \<EVADE\_COMBAT /\> |
| **Azione Obbligatoria** | \<REQUIRE\_ACTION ACTION="..." PENALTY\_STAT="..." PENALTY\_VALUE="..." /\> | Impone un'azione (es. mangiare) per evitare una penalità. | **Testo:** "Devi mangiare un Pasto o perdere 3 punti di RESISTENZA." (da *The Chasm of Doom*, Sez. 19)\<br\>**Tag Generato:** \<REQUIRE\_ACTION ACTION="EAT\_MEAL" PENALTY\_STAT="ENDURANCE" PENALTY\_VALUE="-3" /\> |

### **Tag per la Logica di Gioco e la Memoria**

| Meccanica di Gioco | Tag XML | Scopo del Tag | Esempio dal Libro |
| :---- | :---- | :---- | :---- |
| **Impostare un Flag** | \<SET\_FLAG NAME="..." VALUE="..." /\> | Registra che un evento è accaduto o un'informazione è stata appresa. | **Testo:** "Il suo nome è Banedon. \[...\] Ti consegna un Ciondolo con la Stella di Cristallo." (da *Flight from the Dark*, Sez. 349)\<br\>**Tag Generato:** \<SET\_FLAG NAME="MET\_BANEDON" VALUE="true" /\> |
| **Controllo e Salto Condizionale** | \<IF\_STAT STAT="..." OPERATOR="..." VALUE="..." TARGET\_SCENE="..." /\> | Controlla una statistica e, se la condizione è vera, salta a una scena specifica (spesso una scena di morte). | **Testo:** "Se in qualsiasi momento i tuoi punti RESISTENZA scendono a zero o meno, sei morto." (Regola generale)\<br\>**Tag Generato:** \<IF\_STAT STAT="ENDURANCE" OPERATOR="LESS\_THAN\_OR\_EQUAL" VALUE="0" TARGET\_SCENE="DEATH" /\> |
| **Acquisizione Random (Lista)** | \<RANDOM\_ITEM\_TABLE OUTCOMES="..." /\> | Gestisce l'acquisizione di un oggetto casuale da una lista basata su un tiro di dado. | **Testo:** "Trovi anche uno dei seguenti oggetti: 1 \= Spada... Prendi un numero..." (da *Flight from the Dark*, Equipaggiamento)\<br\>**Tag Generato:** \`\<RANDOM\_ITEM\_TABLE OUTCOMES="0:WEAPON:Broadsword:1 |
| **Acquisizione Random (Quantità)** | \<RANDOM\_QUANTITY ITEM="..." BASE\_VALUE="..." /\> | Gestisce l'acquisizione di una quantità casuale di un oggetto (di solito oro). | **Testo:** "Per scoprire quante Corone d'Oro ci sono... prendi un numero dalla Tabella del Destino e aggiungi 10." (da *The Caverns of Kalte*, Equipaggiamento)\<br\>**Tag Generato:** \<RANDOM\_QUANTITY ITEM="Gold Crown" BASE\_VALUE="10" /\> |

### **Attributi Condizionali per le Scelte (choices)**

Questi non sono tag, ma proprietà speciali all'interno degli oggetti choice che la tua app dovrà interpretare per mostrare o nascondere una scelta.

| Condizione | Proprietà JSON | Scopo | Esempio dal Libro |
| :---- | :---- | :---- | :---- |
| **Possesso di Oggetto** | requiredItem | La scelta è disponibile solo se il giocatore possiede l'oggetto specificato. | **Testo:** "Se hai una Chiave d'Oro..."\<br\>**JSON:** "requiredItem": "Golden Key" |
| **Test di Statistica** | choiceCondition | La scelta è disponibile solo se la statistica del giocatore soddisfa una condizione. | **Testo:** "Se la tua RESISTENZA è 12 o più..."\<br\>**JSON:** "choiceCondition": { "stat": "ENDURANCE", "operator": "GREATER\_THAN\_OR\_EQUAL", "value": "12" } |
| **Evento Passato** | requiredFlag | La scelta è disponibile solo se un evento precedente ha impostato un determinato flag. | **Testo:** "Se conosci la parola d'ordine..."\<br\>**JSON:** "requiredFlag": { "name": "LEARNED\_PASSWORD", "value": "true" } |
| **Tiro di Dado** | minRoll, maxRoll | Il giocatore deve scegliere un range di numeri prima di vedere il risultato del tiro. | **Testo:** "Se scegli un numero da 0 a 4..."\<br\>**JSON:** "minRoll": 0, "maxRoll": 4 |

(consiglio giro di test RUN o DEBUG).