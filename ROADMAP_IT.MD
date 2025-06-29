# Piano di Implementazione Definitivo: Motore di Gioco "Lupo Solitario"

Questo documento descrive il piano d'azione completo per evolvere il progetto **Immunda Noctis** in un'avventura dinamica basata sulle regole e sui contenuti dell'universo di **Lupo Solitario**. Le attività sono strutturate in fasi progressive per garantire uno sviluppo robusto, modulare e manutenibile.

## Fase 1: Rifondazione Architetturale e del Modello Dati
* **Obiettivo:** Creare fondamenta solide e flessibili, disaccoppiando le regole di gioco e adattando i dati alle meccaniche di Lupo Solitario.
* **Peso Complessivo:** `BASSO-MEDIO`
* **Impatto:** Modifiche interne non visibili, ma essenziali per la scalabilità e la pulizia del codice.

| Attività | Descrizione dettagliata | File Principali Coinvolti |
| :--- | :--- | :--- |
| **1.1: Creare il Motore di Regole** | Definire un'interfaccia `GameRulesEngine.kt` che stabilisca un contratto per qualsiasi sistema di regole. Creare la classe concreta `LoneWolfRules.kt` che implementi questa interfaccia. Il `MainViewModel` verrà modificato per utilizzare questa interfaccia, delegando ad essa tutte le operazioni di logica di gioco. | `engine/GameRulesEngine.kt` (Nuovo), `engine/rules/LoneWolfRules.kt` (Nuovo), `view/MainViewModel.kt` |
| **1.2: Adattare il `GameCharacter`** | Modificare `data/GameData.kt`. Sostituire la `data class CharacterStats` con una nuova `LoneWolfStats` contenente **Combattività** e **Resistenza**. Aggiungere al `GameCharacter` il campo `kaiDisciplines: List<String>`. | `data/GameData.kt` |
| **1.3: Strutturare per la Localizzazione** | In `data/GameData.kt`, creare la `data class LocalizedText(val en: String, val it: String)`. Aggiornare le classi dati per le scene (`Scene`, `NarrativeChoice`, `SceneImage`) affinché usino `LocalizedText` per tutti i campi testuali. | `data/GameData.kt` |
| **1.4: Strutturare per la Geolocalizzazione** | In `data/GameData.kt`, creare la `data class LocationInfo(areaName: LocalizedText, coordinates: String?, isMajorLocation: Boolean)` e aggiungerla come campo opzionale `location: LocationInfo?` alla `data class Scene`. | `data/GameData.kt` |
| **1.5: Aggiornare `config.json`** | Modificare la `data class TagConfig` in `GameData.kt` per cambiare il campo `replacement` da `String?` a `Map<String, String>?` per permettere sostituzioni di testo localizzate. | `data/GameData.kt` |

---

## Fase 2: Conversione dei Contenuti e Adattamento UI
* **Obiettivo:** Popolare il gioco con i contenuti del libro e adattare le schermate di base per riflettere le nuove regole.
* **Peso Complessivo:** `ALTO`
* **Impatto:** L'app inizierà ad avere l'aspetto e i contenuti di Lupo Solitario.

| Attività | Descrizione dettagliata | File Principali Coinvolti |
| :--- | :--- | :--- |
| **2.1: Conversione del Libro-Gioco** | Analizzare `Flight from the Dark.html` paragrafo per paragrafo. Per ognuno: **1) Applicare le correzioni dall'Errata**. **2) Estrarre** testo, scelte, discipline, immagini e **informazioni di localizzazione**. **3) Tradurre** tutti i testi in italiano. **4) Popolare** il file `assets/scenes.json` con gli oggetti `Scene` completi. | `assets/scenes.json` (Nuovo), `Flight from the Dark.html` |
| **2.2: Riprogettare la Creazione del PG** | Modificare `SetupActivity.kt` per seguire il flusso di creazione di Lupo Solitario: determinazione di Combattività/Resistenza e selezione delle Discipline Kai. | `SetupActivity.kt` |
| **2.3: Riprogettare la Scheda Personaggio** | Ridisegnare `CharacterSheetActivity.kt` per visualizzare i nuovi dati e l'inventario specifico di Lupo Solitario. | `CharacterSheetActivity.kt` |
| **2.4: Revisione di `config.json`** | Aprire `assets/config.json`: **1) Eliminare** i tag non più necessari. **2) Mantenere** i tag infrastrutturali utili. **3) Adattare** il campo `replacement` per usare l'oggetto multilingua. | `assets/config.json` |

---

## Fase 3: Implementazione delle Meccaniche di Gioco
* **Obiettivo:** Rendere il combattimento funzionante e implementare la logica di gioco tramite tag.
* **Peso Complessivo:** `MEDIO`
* **Impatto:** Il ciclo di gioco principale diventa interattivo e funzionante.

| Attività | Descrizione dettagliata | File Principali Coinvolti |
| :--- | :--- | :--- |
| **3.1: Implementare la Tabella di Combattimento** | In `LoneWolfRules.kt`, tradurre la "Combat Results Table" in una struttura dati Kotlin (es. una `Map`). | `engine/rules/LoneWolfRules.kt` |
| **3.2: Completare la Logica del Round** | Scrivere il codice completo per il metodo `resolveCombatRound()` in `LoneWolfRules.kt` per calcolare il Rapporto di Forza, generare il numero casuale e determinare i danni. | `engine/rules/LoneWolfRules.kt` |
| **3.3: Creare l'Interfaccia di Combattimento** | In `AdventureActivity.kt`, sviluppare una UI specifica per la "modalità combattimento", basata sul "Diario di Combattimento". | `AdventureActivity.kt`, `ui/adventure/` |
| **3.4: Aggiungere i Tag di Meccanica** | In `config.json`, aggiungere i nuovi tag (`[BATTLE:id]`, `[MOD_STAT:...]`, ecc.) e potenziare il `StringTagParser.kt` per inviare i comandi al `ViewModel`. | `assets/config.json`, `util/StringTagParser.kt` |

---

## Fase 4: Esperienza Immersiva e Mappa Dinamica
* **Obiettivo:** Trasformare l'LLM in un narratore dinamico e creare un'interfaccia utente finale ricca, contestuale e visivamente immersiva.
* **Peso Complessivo:** `ALTO`
* **Impatto:** L'applicazione raggiunge il suo pieno potenziale.

| Attività | Descrizione dettagliata | File Principali Coinvolti |
| :--- | :--- | :--- |
| **4.1: Implementare il Prompt Nascosto Dinamico** | Modificare il `MainViewModel`. Ad ogni turno del DM, dovrà costruire e inviare all'LLM un prompt di contesto completo (scena precedente, attuale, scelte future) per guidare la narrazione. | `view/MainViewModel.kt` |
| **4.2: Creare la Barra delle Discipline Dinamica** | Rifattorizzare la `PlayerActionsBar` per mostrare i pulsanti delle Discipline Kai. Lo stato `enabled` sarà controllato dal `ViewModel` in base alla scena corrente. | `ui/adventure/PlayerActionBar.kt`, `view/MainViewModel.kt` |
| **4.3: Implementare la Mappa di Gioco** | Creare una nuova `MapActivity`. **1) Generazione Sfondo:** Usare Stable Diffusion (`stdf`) per generare un'immagine di base per la mappa del mondo (es. pergamena antica). **2) UI Mappa:** Creare una UI che mostri lo sfondo e vi sovrapponga dinamicamente dei "nodi" (icone e etichette) per ogni luogo maggiore (`isMajorLocation=true`) visitato dal giocatore, tracciando il percorso. | `MapActivity.kt` (Nuovo), `ViewModel` associato (Nuovo), `util/GameStateManager.kt` |
| **4.4: Sfondi per Località (Opzionale)** | Potenziare il sistema per usare Stable Diffusion anche per generare sfondi specifici per le singole località maggiori. Questi sfondi potranno essere mostrati nell'header della `AdventureActivity` quando il giocatore si trova in quel luogo, aumentando l'immersione. | `AdventureActivity.kt`, `view/MainViewModel.kt` |
