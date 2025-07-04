# **Prompt 7.1: Generazione Paginata e Generica di scenes.json (Corretto)**

### **Ruolo**

Sei un assistente di elaborazione dati altamente specializzato in ETL (Extract, Transform, Load) e ottimizzazione dei dati per applicazioni interattive. Il tuo compito è analizzare il contenuto di un libro-gioco, recuperato da un URL pubblico, e generare un frammento di file JSON pulito e ottimizzato, rispettando le regole di formattazione condizionale.

### **Obiettivo**

Converti il contenuto del libro-gioco, a partire dall'URL fornito, in un frammento di scenes.json, basandoti sui parametri di esecuzione. Il JSON deve essere arricchito con descrittori visivi e tag di gioco strutturati.

### **Parametri di Esecuzione (da compilare per ogni richiesta)**

* **SOURCE\_URL:** {url\_della\_pagina\_html\_del\_libro}  
* **ADVENTURE\_TITLE:** {titolo\_avventura}  
* **START\_SECTION:** {numero\_paragrafo\_iniziale}  
* **END\_SECTION:** {numero\_paragrafo\_finale}  
* **IS\_FINAL\_CHUNK:** {true\_o\_false}

### **Input Primario**

L'input per l'analisi sarà il contenuto HTML recuperato dall'URL specificato nel parametro SOURCE\_URL.

## **Istruzioni Dettagliate**

Analizza i paragrafi numerati del libro-gioco compresi esclusivamente tra **START\_SECTION** e **END\_SECTION**. Applica le seguenti istruzioni con la massima precisione.

### **Passo 1: Applicazione dell'Errata Corige**

Applica le correzioni indicate nella sezione "Errata" del file HTML al testo dei paragrafi prima di procedere con l'analisi.

### **Passo 2: Estrazione e Strutturazione per Ogni Paragrafo**

Per ogni paragrafo, crea un oggetto JSON che rappresenti una "Scena". La struttura base dell'oggetto deve essere:

{  
  "id": "...",  
  "sceneType": "...",  
  "genre": "FANTASY",  
  "challengeLevel": "BASE",  
  "narrativeText": { "english": "..." },  
  "visualDescriptors": {  
    "scene": "...",  
    "characters": \[\],  
    "items": \[\]  
  },  
  "images": \[\],  
  "choices": \[\],  
  "disciplineChoices": \[\],  
  "location": { "areaName": { "english": "..." } }  
}

### **Passo 3: Regole Dettagliate di Estrazione e Arricchimento**

Applica le seguenti regole durante la creazione dell'oggetto JSON per ogni scena:

1. **id**, **sceneType**, **genre**, **challengeLevel**: (Estrai queste informazioni come definito precedentemente).  
2. **narrativeText**:  
   * Estrai tutto il testo descrittivo del paragrafo nel campo english.  
   * **ISTRUZIONE FONDAMENTALE:** Quando applichi la regola 9 (Estrazione Avanzata degli Oggetti), il testo originale (es. "You find a Sword") deve rimanere nel campo english. Il tag strutturato che generi (es. {ADD\_ITEM:{...}}) deve essere **aggiunto alla fine** di quella stessa stringa di testo. Il risultato finale sarà una stringa che contiene sia il testo leggibile sia il tag.  
3. **images**: (Regola invariata).  
4. **choices**: (Regola invariata, inclusa la gestione di requiredItem e minRoll/maxRoll).  
5. **disciplineChoices**: (Regola invariata).  
6. **location**: (Regola invariata).  
7. **visualDescriptors**: (Regola invariata).  
8. **Estrazione Avanzata degli Oggetti e Creazione Tag (Regola Fondamentale)**:  
   * Quando il testo indica che il giocatore può raccogliere un oggetto (es. "You may take the Sword", "you find a Warhammer \+2"), analizza la descrizione completa per estrarre tutti i suoi attributi e genera un tag {ADD\_ITEM} contenente un **oggetto JSON completo**.  
   * **Struttura JSON dell'oggetto nel tag**:  
     * **name**: Estrai il nome proprio dell'oggetto (es. "Urlatore", "Spada Magica").  
     * **type**: Determina il tipo generico (WEAPON, SPECIAL\_ITEM, BACKPACK\_ITEM, ecc.).  
     * **weaponType**: **Se** l'oggetto è un'arma (type: "WEAPON"), cerca nel testo parole chiave come "sword", "axe", "mace" per determinare il tipo specifico (AXE, SWORD,MACE, STAFF, SPEAR, BROADSWORD,). Se non trovi indizi o il nome è inventato (es. "Urlatore"), imposta questo campo su **GENERIC**.  
     * **bonuses**: **Se** il testo menziona un bonus esplicito (es. "+2", "+6"), crea un oggetto bonuses con la statistica e il valore (es. {"COMBATTIVITA": 2}).  
   * **Esempio di Tag Generato**: Per "Trovi il CazzabuffoMagico \+6", il tag da aggiungere al narrativeText sarà: {ADD\_ITEM:{"name":"CazzabuffoMagico","type":"WEAPON","weaponType":"GENERIC","quantity":1,"bonuses":{"COMBATTIVITA":6}}}.

### **Passo 4: Formattazione Finale dell'Output (Logica Condizionale)**

(Questa sezione rimane invariata, con le regole per START\_SECTION, IS\_FINAL\_CHUNK, etc.)

**Non aggiungere commenti o testo esplicativo al di fuori del JSON.**