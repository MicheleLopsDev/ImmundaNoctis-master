## **Guida Rapida al Flusso di Estrazione per Lupo Solitario**

Questo documento riassume il funzionamento dei due prompt specializzati per l'estrazione dei dati dai libri-gioco della serie Lupo Solitario.

### **1\. Prompt per l'Estrazione degli Oggetti (**items.json**)**

Questo è il **primo passo** del processo. Il suo unico scopo è analizzare l'intero libro per creare un database completo e centralizzato di tutti gli oggetti esistenti.

#### **Obiettivo Principale**

* **Creare una Fonte Unica di Verità:** Analizza l'intero libro e genera un singolo file items.json. Ogni oggetto (spada, pozione, oggetto di missione) viene definito una sola volta.  
* **Garantire Coerenza:** Evita che lo stesso oggetto venga definito con proprietà diverse in punti diversi del gioco.

#### **Come Funziona**

1. **Input:** Richiede solo il file HTML del libro-gioco.  
2. **Processo:**  
   * Applica le correzioni dall'Errata Corrige.  
   * Scansiona l'intero documento, con particolare attenzione alle sezioni "Equipment" e ai paragrafi di gioco.  
   * Identifica ogni oggetto, ne estrae le proprietà (itemType, modifier, notes) e crea una voce univoca nel database.  
3. **Output:** Un array JSON completo che rappresenta il database degli oggetti per quell'avventura.

### **2\. Prompt per l'Estrazione delle Scene (**scenes.json**)**

Questo è il **secondo passo**. Utilizza il libro per generare la sequenza delle scene, le narrazioni e le meccaniche di gioco, facendo riferimento agli oggetti già definiti.

#### **Obiettivo Principale**

* **Estrarre la Struttura del Gioco:** Converte i paragrafi numerati in scene JSON, complete di testo, scelte e logica di gioco.  
* **Interpretare la Logica:** Non si limita a estrarre testo, ma interpreta le regole e le condizioni narrative per generare i tag XML corretti (\<combat\>, \<ifCondition\>, \<randomCheck\>, ecc.).

#### **Come Funziona**

1. **Input:** Richiede i seguenti parametri:  
   * START\_SECTION: Il paragrafo da cui iniziare (obbligatorio).  
   * END\_SECTION: Il paragrafo a cui fermarsi (opzionale).  
   * IS\_FINAL\_CHUNK: true o false (usato solo se END\_SECTION è specificato).  
2. **Processo:**  
   * Applica le correzioni dall'Errata Corrige.  
   * Si focalizza **esclusivamente** sul contenuto dei paragrafi numerati.  
   * Analizza l'intervallo di paragrafi richiesto.  
   * Estrae il testo narrativo e lo separa dalle meccaniche di gioco.  
   * Converte le meccaniche di gioco in tag XML, inserendole nell'array gameMechanics.  
3. **Output:** Un array JSON di scene.  
   * Se END\_SECTION non viene specificato, l'output è un file JSON completo e chiuso (IS\_FINAL\_CHUNK è considerato true).  
   * Se END\_SECTION viene specificato, l'output è un frammento che può essere concatenato con altri.

### **Flusso di Lavoro Consigliato**

1. **Esegui il Prompt degli Oggetti:** Passagli il file HTML del libro per generare il file items.json completo.  
2. **Esegui il Prompt delle Scene:** Passagli lo stesso file HTML e i parametri START\_SECTION (ed eventualmente END\_SECTION) per generare il file scenes.json.

Seguendo questo processo in due fasi, otterrai un set di dati pulito, normalizzato e molto più facile da gestire per il tuo motore di gioco.

(consiglio giro di test RUN o DEBUG).