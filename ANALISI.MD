Ciao! Sono pronto ad aiutarti con il tuo progetto GDR/LIBROGAME su Android, basato sul regolamento di Lupo Solitario. Iniziamo subito con l'analisi delle classi che mi hai fornito. Procederò una per una per darti un quadro chiaro di ciascuna.

### Analisi delle Classi Kotlin:

#### 1. `immundanoctis/worker/DownloadWorker.kt`
Questa classe è un `CoroutineWorker` che gestisce il download di file di grandi dimensioni in background, come i modelli AI. Utilizza notifiche per mostrare lo stato del download e supporta download a pezzi (byte-range) per una maggiore robustezza. Crea un file temporaneo durante il download e lo rinomina solo alla fine per garantire l'integrità del file scaricato.
* **Scopo:** Gestire download in background in modo robusto.
* **Funzionalità chiave:**
    * Download di file da URL con supporto a `HttpURLConnection`.
    * Utilizzo di `ForegroundInfo` per mantenere il download attivo anche se l'app è in background.
    * Gestione del progresso del download.
    * Salvataggio su file temporaneo e rinomina al completamento.
    * Cancellazione del file temporaneo in caso di errore.
* **Costanti importanti:** `KEY_URL`, `KEY_DESTINATION`, `KEY_MODEL_DOWNLOAD_ACCESS_TOKEN`, `KEY_MODEL_NAME`, `KEY_BYTES_DOWNLOADED`, `KEY_TOTAL_BYTES` per passare i dati al worker.

#### 2. `immundanoctis/util/SavePreferences.kt`
Questa classe si occupa della gestione delle preferenze dell'applicazione relative al salvataggio del gioco e allo stile di gioco, utilizzando `SharedPreferences`.
* **Scopo:** Persistenza delle impostazioni utente come il salvataggio automatico e l'abilitazione della chat.
* **Proprietà chiave:**
    * `isAutoSaveEnabled`: booleano per abilitare/disabilitare il salvataggio automatico della chat (default `true`).
    * `isChatEnabled`: booleano per abilitare/disabilitare l'interazione con i PNG (default `false`).
    * `scenesPath`: stringa che memorizza il percorso del file JSON delle scene.
    * `isConfigCopied`: booleano per tracciare se il file di configurazione è stato copiato.
* **Costanti:** `PREFS_NAME` (nome del file di preferenze), `KEY_AUTOSAVE`, `KEY_CHAT_ENABLED`, `KEY_JSON_SCNES_PATH`, `KEY_CONFIG_COPIED`.

#### 3. `immundanoctis/view/MainViewModel.kt`
Questo è il ViewModel principale per la schermata dell'avventura, gestendo la logica di business e lo stato dell'interfaccia utente. Interagisce con i motori di inferenza (Gemma e LlamaCpp), la gestione dello stato del gioco e le preferenze.
* **Scopo:** Fornire dati alla UI dell'AdventureActivity, gestire la logica di gioco, l'interazione con i motori AI e la persistenza della sessione.
* **Stato UI:** `engineLoadingState`, `sessionName`, `chatMessages`, `gameCharacters`, `streamingText`, `isGenerating`, `respondingCharacterId`, `logMessages`, `conversationTargetId`, `saveChatEvent`, `currentScene`, `activeNarrativeChoices`, `activeDisciplineChoices`, `kaiRank`, `isRandomNumberRollRequired`, `randomNumberResult`, `activeTokenInfo`.
* **Funzionalità chiave:**
    * Caricamento e gestione della sessione di gioco (nuova o esistente).
    * Inizializzazione e gestione dei motori AI (`GemmaEngine`, `LlamaCppEngine`, `TranslationEngine`).
    * Invio di messaggi ai motori AI e gestione del testo in streaming.
    * Salvataggio automatico e manuale della chat.
    * Traduzione dei messaggi.
    * Reset della sessione di chat.
    * Processamento dei comandi embeddati nei testi generati dall'AI (es. `playAudio`, `generateImage`, `narrativeChoice`).
    * Gestione della navigazione tra le scene e delle scelte narrative/di disciplina.
    * Lancio dei dadi e risoluzione delle scelte basate sul lancio.
* **Collabora con:** `GameStateManager`, `EnginePreferences`, `ThemePreferences`, `LlamaPreferences`, `SavePreferences`, `StringTagParser`, `GameLogicManager`.

#### 4. `immundanoctis/stdf/service/BackendService.kt`
Questa classe è un `Service` Android responsabile dell'avvio e della gestione del backend C++ (presumibilmente un eseguibile per la generazione di immagini Stable Diffusion - STDF). Viene eseguito come servizio in primo piano per garantire che il processo non venga terminato dal sistema.
* **Scopo:** Avviare e mantenere in esecuzione il processo nativo C++ per il backend di generazione immagini.
* **Funzionalità chiave:**
    * Avvia un processo esterno (`ProcessBuilder`).
    * Imposta le variabili d'ambiente necessarie (es. `LD_LIBRARY_PATH`).
    * Gestisce lo stato del backend (`Idle`, `Starting`, `Running`, `Error`).
    * Crea una notifica in primo piano per il servizio.
    * Legge l'output del processo C++ per il logging.
* **Costanti:** `EXECUTABLE_NAME` (nome dell'eseguibile nativo), `NOTIFICATION_ID`, `CHANNEL_ID`.
* **Dipende da:** `StdfModelRepository`, `StdfModel`.

#### 5. `immundanoctis/SetupActivity.kt`
Questa Activity gestisce la schermata di creazione del personaggio o di caricamento di una sessione esistente. È il punto di ingresso per configurare il personaggio principale del gioco.
* **Scopo:** Permettere all'utente di creare un nuovo personaggio (definendo statistiche, discipline, equipaggiamento) o di continuare un'avventura esistente.
* **Componenti Composable principali:**
    * `CharacterCreationScreen`: UI per la creazione di un nuovo personaggio.
    * `RandomStatsCard`: Permette il lancio casuale delle statistiche (Combattività e Resistenza).
    * `EquipmentChoiceCard`: Consente la selezione di un'arma e un oggetto speciale iniziale.
    * `DisciplineGridCard`: Permette la selezione di 5 discipline Kai.
    * `ExistingSessionScreen`: UI per mostrare i dettagli di una sessione salvata e le opzioni per continuare o crearne una nuova.
* **Interagisce con:** `SetupViewModel`, `GameStateManager`, `ThemePreferences`.
* **Importazioni notevoli:** `GameItem`, `INITIAL_WEAPONS`, `INITIAL_SPECIAL_ITEMS`, `ItemType` per la gestione dell'inventario iniziale.

#### 6. `immundanoctis/util/SavePreferences.kt` (già analizzata)

#### 7. `immundanoctis/util/GameStateManager.kt`
Questa classe è responsabile della gestione dello stato di gioco persistente, inclusi il salvataggio, il caricamento e la cancellazione della sessione di gioco. Utilizza Gson per la serializzazione/deserializzazione in JSON.
* **Scopo:** Gestire la persistenza dei dati della sessione di gioco.
* **Funzionalità chiave:**
    * `saveSession(sessionData: SessionData)`: Salva l'intera sessione di gioco su un file JSON.
    * `loadSession(): SessionData?`: Carica la sessione di gioco dal file JSON.
    * `deleteSession(): Boolean`: Cancella il file di salvataggio della sessione.
    * `createDefaultSession(): SessionData`: Crea una sessione di gioco predefinita con un eroe, un DM e un compagno, inclusi l'inventario iniziale e le statistiche.
* **Dipende da:** `SessionData`, `GameCharacter`, `CharacterID`, `CharacterType`, `LoneWolfStats`, `HeroDetails`, `GameItem`, `ItemType`.

#### 8. `immundanoctis/ui/configuration/ModelSlot.kt`
Questo file definisce un componente Composable riutilizzabile (`ModelSlotView`) per la configurazione dei modelli AI nell'interfaccia utente. Mostra lo stato di un modello (scaricato, in download, non presente) e offre azioni come impostare un URL, selezionare un file locale, avviare il download o eliminare il modello.
* **Scopo:** Fornire un'interfaccia utente standardizzata per la gestione di un singolo slot modello AI (es. modello DM, modello Player).
* **Componenti Composable:**
    * `ModelSlotView`: La card principale che mostra il nome del modello, la sua descrizione e le azioni disponibili.
    * `AddUrlDialog`: Un dialog per inserire l'URL di un modello da scaricare.
* **Interagisce con:** `WorkManager` per osservare lo stato dei download (`DownloadWorker`), `Downloadable` (un data class per i dettagli del modello).

#### 9. `immundanoctis/tools/add_uuids.kt`
Questo è uno script Kotlin standalone (non parte dell'applicazione Android runtime) progettato per aggiungere UUID unici agli oggetti `TagConfig` e `TagParameter` all'interno di un file JSON di configurazione dei tag.
* **Scopo:** Assicurare che ogni configurazione di tag e i suoi parametri abbiano un ID univoco, utile per la gestione programmatica e la persistenza.
* **Funzionalità:** Legge un file JSON di input (`config.json`), itera sui `tags` e `parameters`, assegna un UUID se un ID non è già presente o è vuoto, e scrive il risultato in un nuovo file JSON.
* **Dipende da:** `Jackson` (libreria di serializzazione JSON).
* **Nota:** Questo file è uno strumento di sviluppo, non codice che gira nell'app Android.

#### 10. `immundanoctis/stdf/data/StdfModel.kt`
Questo file definisce le strutture dati (`StdfModel`, `ModelFile`, `HighresInfo`, `DownloadProgress`) per rappresentare i modelli di generazione di immagini Stable Diffusion. Include anche una classe `StdfModelRepository` per la gestione di una lista predefinita di modelli (sia per NPU che per CPU) e il loro stato di download.
* **Scopo:** Definire la struttura dei dati per i modelli STDF e gestirne il repository (lista di modelli disponibili).
* **Data classes chiave:**
    * `StdfModel`: Rappresenta un modello STDF completo con ID, nome, URL, file, dimensioni, prompt predefiniti, ecc.
    * `ModelFile`: Rappresenta un singolo file che compone un modello STDF.
* **Funzionalità chiave:**
    * `download(context: Context, hfToken: String?)`: Inizializza un `StdfDownloadWorker` per scaricare tutti i file del modello.
    * `deleteModel(context: Context)`: Cancella la directory del modello e le relative informazioni di verifica.
    * `StdfModelRepository`: Inizializza la lista di modelli STDF disponibili, verificandone lo stato di download.
* **Dipende da:** `StdfDownloadWorker`, `StdfFileVerification`, `ImageGenerationPreferences`.

#### 11. `immundanoctis/util/SavePreferences.kt` (già analizzata)

#### 12. `immundanoctis/view/StdfViewModel.kt`
Questo ViewModel gestisce lo stato e la logica della schermata di gestione dei modelli STDF (`StdfModelActivity`). Si occupa di caricare i modelli, monitorare i loro stati di download e gestire la selezione del modello attivo.
* **Scopo:** Fornire e gestire lo stato UI per la schermata di gestione dei modelli STDF.
* **Stato UI:** `models`, `hfToken`, `selectedModelId`.
* **Funzionalità chiave:**
    * Caricamento e ricaricamento della lista dei modelli STDF dal `StdfModelRepository`.
    * Lettura del token Hugging Face per i download.
    * Salvataggio e recupero del modello STDF attualmente selezionato.
    * Avvio del download di un modello tramite `StdfModel.download()`.
    * Cancellazione di un modello tramite `StdfModel.deleteModel()`.
    * Esposizione di `WorkInfo` per monitorare il progresso dei download.
* **Collabora con:** `StdfModelRepository`, `WorkManager`, `ImageGenerationPreferences`, `ThemePreferences`.

#### 13. `immundanoctis/ui/adventure/AdventureHeader.kt`
Questo componente Composable definisce l'intestazione della schermata di avventura. Mostra i ritratti dei personaggi (DM, Eroe, Compagni), il personaggio attualmente selezionato e un semaforo per lo stato dei token dell'AI.
* **Scopo:** Mostrare informazioni chiave della sessione di gioco e permettere la selezione del personaggio con cui interagire.
* **Componenti Composable:**
    * `CharacterPortrait`: Visualizza il ritratto e il nome di un singolo personaggio.
    * `TokenSemaphoreIndicator`: Mostra lo stato di utilizzo dei token AI (verde, giallo, rosso, critico) e un dialogo per resettare la sessione.
* **Interagisce con:** `GameCharacter`, `TokenInfo`, `TokenStatus`, e reindirizza a `CharacterSheetActivity`.
* **Nuovo parametro:** `isChatEnabled` per controllare la cliccabilità dei personaggi NPC/DM.

#### 14. `immundanoctis/ui/adventure/ChatComponents.kt`
Questo file contiene i componenti Composable per la visualizzazione della chat, inclusi le bolle dei messaggi, l'indicatore di generazione e l'input per i messaggi.
* **Scopo:** Costruire l'interfaccia utente per la visualizzazione e l'interazione della chat.
* **Componenti Composable:**
    * `MessageBubble`: Visualizza un singolo messaggio di chat, inclusi autore, testo, testo tradotto e opzioni di copia/traduzione/riproduzione audio.
    * `GeneratingIndicator`: Mostra un messaggio di "sta pensando..." quando l'AI sta generando una risposta, con un pulsante per fermare.
    * `MessageInput`: Il campo di testo per l'utente per digitare messaggi, con limite caratteri e pulsante di invio.
* **Dipende da:** `ChatMessage`, `GameCharacter`, `CharacterID`.

#### 15. `immundanoctis/view/StdfViewModel.kt` (già analizzata)

#### 16. `immundanoctis/ui/adventure/ChoiceComponents.kt`
Definisce i componenti Composable per visualizzare le scelte narrative e le prove di disciplina che il giocatore può intraprendere.
* **Scopo:** Presentare al giocatore le opzioni di interazione con la storia e le sfide.
* **Componenti Composable:**
    * `ChoicesContainer`: Un layout per mostrare una lista di `NarrativeChoice` e `DisciplineChoice`.
    * `ActionChoiceCard`: La card per una singola scelta, con stile diverso per le "prove speciali" (discipline).
* **Dipende da:** `NarrativeChoice`, `DisciplineChoice`, `KAI_DISCIPLINES`, `getIconForDiscipline`.

#### 17. `immundanoctis/util/ImageGenerationPreferences.kt`
Gestisce le preferenze specifiche per i parametri di generazione delle immagini STDF, come prompt, negative prompt, passi, ecc., e il modello STDF attualmente selezionato.
* **Scopo:** Salvare e recuperare le impostazioni per la generazione di immagini AI e il modello STDF preferito.
* **Data class:** `GenerationPrefs` (contiene tutti i parametri di generazione).
* **Funzionalità chiave:**
    * `saveSelectedModelId(modelId: String?)`: Salva l'ID del modello STDF selezionato.
    * `getSelectedModelId(): String?`: Recupera l'ID del modello STDF selezionato.
    * `saveBaseUrl(url: String)`: Salva l'URL di base per il download dei modelli.
    * `getBaseUrl(): String`: Recupera l'URL di base (con default Hugging Face).
    * `saveAllFields(modelId: String, prefsData: GenerationPrefs)`: Salva tutte le preferenze di generazione per un dato modello.
    * `getPreferences(modelId: String)`: Recupera tutte le preferenze per un dato modello.
* **Costanti:** `PREFS_NAME`, `KEY_BASE_URL`, `KEY_SELECTED_MODEL_ID`.

#### 18. `immundanoctis/ui/theme/Type.kt`
Definisce la tipografia (stili di testo) utilizzati nell'applicazione in base a Material Design 3.
* **Scopo:** Centralizzare e definire gli stili di testo globali dell'applicazione.
* **Contenuto:** `Typography` (un oggetto Material3 Typography che definisce vari stili come `bodyLarge`, ecc.).

#### 19. `immundanoctis/stdf/data/StdfFileVerification.kt`
Questa classe utilizza `SharedPreferences` per memorizzare e verificare le dimensioni dei file dei modelli STDF scaricati, garantendo l'integrità dei file.
* **Scopo:** Verificare l'integrità dei file dei modelli STDF dopo il download.
* **Funzionalità chiave:**
    * `saveFileSize(modelId: String, fileName: String, size: Long)`: Salva la dimensione attesa di un file.
    * `getFileSize(modelId: String, fileName: String): Long`: Recupera la dimensione salvata.
    * `clearVerification(modelId: String)`: Rimuove tutte le informazioni di verifica per un modello specifico.
* **Costanti:** `PREFS_NAME`.

#### 20. `immundanoctis/StdfGenerationActivity.kt`
Questa Activity è la schermata principale per la generazione di immagini usando i modelli STDF. Permette all'utente di inserire prompt, negative prompt e di avviare il processo di generazione. Visualizza lo stato di progresso e l'immagine generata.
* **Scopo:** Interfaccia utente per la generazione di immagini tramite i modelli STDF.
* **Componenti Composable:** `StdfGenerationScreen`.
* **Funzionalità chiave:**
    * Passaggio del `modelId` e dei prompt predefiniti all'Activity.
    * Avvio e gestione del `BackendService` per il processo C++ del backend.
    * Avvio del `BackgroundGenerationService` per la generazione effettiva dell'immagine.
    * Visualizzazione dell'immagine generata e dello stato di progresso.
    * Definizione di prompt e negative prompt iniziali basati sul modello selezionato.
* **Interagisce con:** `StdfModel`, `StdfModelRepository`, `BackendService`, `BackgroundGenerationService`, `ImageGenerationPreferences`, `ThemePreferences`.

#### 21. `immundanoctis/StdfModelActivity.kt`
Questa Activity consente la gestione dei modelli STDF, permettendo all'utente di scaricare, eliminare e selezionare i modelli per la generazione di immagini. Utilizza un `HorizontalPager` per separare i modelli NPU e CPU.
* **Scopo:** Interfaccia utente per la gestione (download, cancellazione, selezione) dei modelli di generazione immagini STDF.
* **Componenti Composable:** `StdfModelManagementScreen`.
* **Funzionalità chiave:**
    * Mostra i modelli STDF disponibili, filtrati per tipo (NPU/CPU).
    * Monitora lo stato di download dei modelli tramite `WorkManager`.
    * Permette di avviare il download di un modello (`StdfDownloadWorker`).
    * Consente l'eliminazione di un modello.
    * Permette la selezione del modello attivo per la generazione.
* **Interagisce con:** `StdfViewModel`, `StdfModelSlotView`, `StdfDownloadWorker`, `WorkInfo`, `ThemePreferences`.

#### 22. `immundanoctis/view/SetupViewModel.kt`
Questo ViewModel gestisce la logica di creazione del personaggio (`SetupActivity`). Si occupa di generare statistiche casuali, gestire la selezione delle discipline Kai e dell'equipaggiamento iniziale, e finalizzare la creazione della sessione di gioco.
* **Scopo:** Gestire la logica di creazione del personaggio e preparare la sessione di gioco iniziale.
* **Stato UI:** `heroName`, `combattivita`, `resistenza`, `stdfPrompt`, `currentScenesJsonPath`, `selectedWeapon`, `selectedSpecialItem`.
* **Funzionalità chiave:**
    * `rollStats()`: Genera valori casuali per Combattività e Resistenza.
    * `toggleDiscipline(disciplineId: String)`: Aggiunge o rimuove una disciplina dalla lista selezionata (max 5).
    * `onWeaponSelected(weapon: GameItem)`: Imposta l'arma iniziale selezionata.
    * `onSpecialItemSelected(item: GameItem)`: Imposta l'oggetto speciale iniziale selezionato.
    * `finalizeSessionCreation(defaultSession: SessionData)`: Crea l'oggetto `SessionData` finale basandosi sulle scelte dell'utente, applicando i bonus dell'equipaggiamento.
    * `copyAndSaveScenesJson(uri: Uri)`: Copia un file JSON di scene selezionato dall'utente nella directory dell'app.
* **Collabora con:** `SavePreferences`, `GameItem`, `ItemType`, `HeroDetails`, `LoneWolfStats`, `INITIAL_COMMON_ITEMS`.

#### 23. `immundanoctis/engine/LlamaCppEngine.kt`
Questa implementazione dell'interfaccia `InferenceEngine` utilizza la libreria `llama.cpp` (tramite l'SDK Android) per gestire l'inferenza di modelli LLM GGUF (come Llama). Mantiene una cronologia di chat e stima l'utilizzo dei token.
* **Scopo:** Fornire un motore di inferenza per i modelli LLM GGUF (Player Character).
* **Funzionalità chiave:**
    * `load(modelPath: String, chatbotPersonality: String?)`: Carica il modello GGUF specificato e configura i parametri.
    * `sendMessage(text: String)`: Invia il testo al modello e restituisce un `Flow<String>` di token in streaming.
    * `resetSession(systemPrompt: String?)`: Ricarica il modello per resettare la sessione.
    * `unload()`: Rilascia le risorse del modello.
    * `getTokensUsed()`: Restituisce il conteggio dei token utilizzati.
    * Mantiene una cronologia di chat per il contesto (`chatHistory`).
    * `buildPromptWithHistory()`: Costruisce il prompt completo per il modello includendo la cronologia e un prompt di sistema.
    * Stima l'utilizzo dei token e aggiorna `_tokenInfo`.
* **Dipende da:** `LLamaAndroid` (il wrapper nativo), `LlamaPreferences`.

#### 24. `immundanoctis/util/ModelPreferences.kt`
Questa classe gestisce la persistenza delle informazioni sui modelli LLM (DM e Player) utilizzando `SharedPreferences`. Salva il nome, l'URL di origine e il percorso del file di destinazione per ciascun modello.
* **Scopo:** Salvare e recuperare i dettagli sui modelli LLM configurati per il DM e i PG.
* **Funzionalità chiave:**
    * `saveDmModel(model: Downloadable)` / `getDmModel(): Downloadable?` / `clearDmModel()`: Per il modello del Dungeon Master.
    * `savePlayerModel(model: Downloadable)` / `getPlayerModel(): Downloadable?` / `clearPlayerModel()`: Per il modello del Giocatore.
* **Dipende da:** `Downloadable`.

#### 25. `immundanoctis/ui/adventure/AdventureUtils.kt`
Questo file contiene funzioni di utilità per l'interfaccia utente della schermata dell'avventura, principalmente per la mappatura degli ID delle discipline a icone e per una visualizzazione robusta delle immagini.
* **Scopo:** Fornire helper UI per la schermata dell'avventura.
* **Funzionalità chiave:**
    * `getIconForDiscipline(disciplineId: String)`: Restituisce un `ImageVector` (icona Material Design) in base all'ID di una disciplina Kai.
    * `RobustImage`: Un Composable per caricare immagini da risorse drawable con un fallback per errori.
    * `getIconForItem(item: GameItem?)`: Tenta di mappare un `GameItem` a una risorsa drawable per l'icona.

#### 26. `immundanoctis/ui/adventure/AdventureDialogs.kt`
Questo file definisce i componenti Composable per i dialoghi all'interno della schermata dell'avventura, come il dialogo per le abilità.
* **Scopo:** Visualizzare dialoghi informativi o interattivi.
* **Componenti Composable:**
    * `SkillDialog`: Mostra i dettagli di una lista di abilità (Skill) in un dialogo.
    * `SkillCard`: Rappresenta una singola abilità all'interno del dialogo, mostrando nome, icona, livello e effetti.
    * `RatingStars`: Un helper Composable per visualizzare le stelle di valutazione del livello di un'abilità.
* **Dipende da:** `Skill`.

#### 27. `immundanoctis/ui/theme/Color.kt`
Questo file definisce i colori utilizzati nel tema Material Design 3 dell'applicazione, sia per il tema scuro che per quello chiaro.
* **Scopo:** Centralizzare la definizione dei colori per la coerenza visiva dell'applicazione.
* **Contenuto:** Definizione di `Purple80`, `PurpleGrey80`, `Pink80`, `Purple40`, `PurpleGrey40`, `Pink40` e una nuova palette completa per `LightPrimary`, `LightOnPrimary`, ecc.

#### 28. `immundanoctis/util/StringTagParser.kt`
Questa classe è un parser che analizza una stringa di input (es. una risposta del modello AI) alla ricerca di tag specifici definiti in un file `config.json`. Quando trova un tag, può sostituirlo con un testo localizzato e/o generare un `EngineCommand` corrispondente.
* **Scopo:** Interpretare i tag speciali nei testi generati dall'AI e tradurli in comandi o sostituzioni di testo.
* **Funzionalità chiave:**
    * Carica le configurazioni dei tag da `assets/config.json`.
    * `parseAndReplaceWithCommands(inputString: String, currentActor: CharacterType? = null, lang: String = "en")`: La funzione principale che esegue la scansione, la sostituzione e la generazione dei comandi.
    * Supporta diversi tipi di tag (`textSubstitution`, `gameMechanic`, `directionalChoice`, `triggerAudio`, ecc.).
* **Dipende da:** `ObjectMapper` (Jackson), `TagsConfigWrapper`, `TagConfig`, `EngineCommand`, `CharacterType`, `ChallengeLevel`, `LocalizedText`.

#### 29. `immundanoctis/MainActivity.kt`
Questa è l'Activity principale e il punto di ingresso dell'applicazione. Gestisce la schermata del menu principale, il toggle del tema (chiaro/scuro) e include una logica iniziale per copiare un file `scenes.json` negli asset dell'applicazione, se non è già presente.
* **Scopo:** Fornire il menu principale dell'applicazione e inizializzare alcune impostazioni di base.
* **Componenti Composable:**
    * `MainMenuScreen`: Mostra i pulsanti per navigare verso le diverse sezioni dell'app (Avventura, Genera Immagini, Modelli LLM, Modelli STDF, Impostazioni).
    * `MenuIcon`: Un componente riutilizzabile per i pulsanti del menu.
* **Funzionalità chiave:**
    * Gestione del tema dell'app.
    * Copia di `scenes.json` dagli asset alla directory specifica dell'app al primo avvio.
    * Navigazione tra le diverse Activity.
* **Interagisce con:** `SetupActivity`, `StdfGenerationActivity`, `ModelActivity`, `StdfModelActivity`, `ConfigurationActivity`, `ThemePreferences`, `SavePreferences`, `copyAssetToFile`, `getAppSpecificDirectory`.

#### 30. `immundanoctis/engine/GameLogicManager.kt`
Questa classe gestisce la logica di alto livello delle scene di gioco. Carica tutte le scene da un file JSON, seleziona scene iniziali casuali per un genere specifico e permette di recuperare scene per ID.
* **Scopo:** Gestire il flusso delle scene e la navigazione nel gioco.
* **Funzionalità chiave:**
    * `loadAllScenes()`: Carica le scene dal file JSON (il percorso è preso da `SavePreferences`).
    * `selectRandomStartScene(genre: Genre)`: Seleziona una scena di tipo `START` casuale per un dato genere.
    * `getSceneById(sceneId: String)`: Recupera una scena specifica tramite il suo ID.
    * `markSceneAsUsed(sceneId: String)`: Traccia le scene già visitate (anche se non utilizzata direttamente nel ViewModel nel codice fornito, è una funzionalità utile).
    * `resetUsedScenes()`: Resetta la lista delle scene usate.
* **Dipende da:** `SavePreferences`, `Scene`, `Genre`, `SceneType`.

#### 31. `immundanoctis/view/CharacterSheetViewModel.kt`
Questo ViewModel gestisce la logica e lo stato della schermata della scheda personaggio (`CharacterSheetActivity`). Si occupa di caricare i dati dell'eroe, calcolare il grado Kai e gestire l'inventario, inclusa la selezione dell'arma attiva.
* **Scopo:** Fornire e gestire lo stato UI per la schermata della scheda personaggio.
* **Stato UI:** `heroCharacter`, `combatSkill`, `endurance`, `mealsCount`, `goldCount`, `visibleWeapons`, `specialItems`, `backpackItems`, `kaiDisciplines`, `kaiRank`, `selectedWeapon`.
* **Funzionalità chiave:**
    * `loadCharacterData()`: Carica i dati dell'eroe dalla sessione di gioco.
    * Calcola il `kaiRank` in base alle discipline possedute.
    * Popola l'inventario dividendo gli oggetti in armi, oggetti speciali e oggetti dello zaino.
    * Assicura che ci siano sempre 2 "armi visibili" (o pugni se non ci sono altre armi).
    * `selectWeapon(weapon: GameItem?)`: Imposta l'arma attualmente selezionata.
    * `addWeapon(newWeapon: GameItem)`: Gestisce l'aggiunta di nuove armi, inclusa la logica di sostituzione se si supera il limite.
* **Collabora con:** `GameStateManager`, `LoneWolfRules`, `CharacterID`, `ItemType`, `FISTS_WEAPON`, `KAI_DISCIPLINES`.

#### 32. `immundanoctis/engine/TranslationEngine.kt`
Questa classe fornisce funzionalità di traduzione linguistica utilizzando ML Kit di Google. Permette l'identificazione automatica della lingua sorgente e la traduzione verso una lingua di destinazione specificata (default italiano). Gestisce il download e la persistenza dei modelli di traduzione.
* **Scopo:** Fornire servizi di traduzione del testo all'interno dell'app.
* **Funzionalità chiave:**
    * `loadModel()`: Scarica il modello di traduzione predefinito (inglese-italiano).
    * `identifyLanguage(text: String)`: Identifica la lingua di un dato testo.
    * `translate(text: String, targetLang: String)`: Traduce il testo, scaricando dinamicamente i modelli necessari.
    * `getTranslator(sourceLang: String, targetLang: String)`: Recupera o crea un traduttore per una coppia di lingue.
    * `close()`: Rilascia le risorse dei traduttori.
    * `resetTranslators()`: Resetta lo stato dei traduttori.
* **Dipende da:** Google ML Kit (Language Identification, Translation).

#### 33. `immundanoctis/stdf/service/BackgroundGenerationService.kt`
Questo `Service` Android gestisce la generazione effettiva delle immagini Stable Diffusion comunicando con il backend C++ tramite chiamate HTTP a un server localhost (presumibilmente avviato dal `BackendService`). Gestisce il progresso della generazione e notifica l'interfaccia utente.
* **Scopo:** Eseguire la generazione di immagini in background e comunicare il progresso/risultato alla UI.
* **Funzionalità chiave:**
    * `runGeneration(...)`: Effettua la richiesta HTTP al backend STDF, inviando prompt e parametri.
    * Gestisce il flusso di dati streaming (Server-Sent Events) per aggiornare il progresso.
    * Decodifica l'immagine Base64 ricevuta dal backend in un `Bitmap`.
    * Aggiorna lo stato di generazione (`GenerationState.Progress`, `GenerationState.Complete`, `GenerationState.Error`).
    * Crea notifiche per il progresso della generazione.
* **Dipende da:** `OkHttpClient`, `JSONObject`, e si basa sul `BackendService` in esecuzione.

#### 34. `immundanoctis/data/ChatMessage.kt`
Questa data class rappresenta un singolo messaggio all'interno della chat del gioco.
* **Scopo:** Modellare i dati di un messaggio di chat.
* **Campi:**
    * `authorId`: ID del personaggio che ha scritto il messaggio (collegato a `GameCharacter`).
    * `text`: Contenuto principale del messaggio.
    * `position`: Posizione del messaggio nella chat (per l'ordine).
    * `timestamp`: Momento di creazione del messaggio.
    * `id`: UUID unico per il messaggio.
    * `translatedText`: Campo opzionale per la traduzione del testo.
    * `isTranslating`: Flag per indicare se il messaggio è in fase di traduzione.

#### 35. `immundanoctis/engine/InferenceEngine.kt`
Questa interfaccia definisce il contratto generico per qualsiasi motore di inferenza LLM (Large Language Model) utilizzato nell'applicazione. Include metodi per caricare, inviare messaggi, resettare e scaricare il modello, oltre a fornire informazioni sull'utilizzo dei token.
* **Scopo:** Fornire un'API comune per diversi tipi di motori LLM (es. Gemma, LlamaCpp).
* **Data classes:**
    * `TokenInfo`: Contiene il conteggio dei token, il limite massimo, lo stato e la percentuale di utilizzo.
* **Enum:**
    * `TokenStatus`: Definisce gli stati del consumo dei token (GREEN, YELLOW, RED, CRITICAL).
* **Metodi chiave:**
    * `tokenInfo`: `StateFlow` per l'aggiornamento reattivo dello stato dei token.
    * `load(modelPath: String, chatbotPersonality: String?)`: Carica il modello.
    * `sendMessage(text: String)`: Invia un prompt e restituisce un `Flow` di stringhe.
    * `unload()`: Rilascia le risorse del modello.
    * `resetSession(systemPrompt: String?)`: Resetta lo stato interno della sessione del modello.
    * `getTokensUsed()`: Restituisce i token utilizzati.

#### 36. `immundanoctis/ui/adventure/PlayerActionBar.kt`
Questo componente Composable visualizza le informazioni e le azioni principali del personaggio del giocatore (Lupo Solitario), come statistiche, pasti, grado Kai e un pulsante per il lancio del dado.
* **Scopo:** Mostrare un riepilogo rapido dello stato del personaggio giocante e abilitare azioni rapide.
* **Componenti Composable:**
    * Visualizzazione di nome, grado Kai, Combattività, Resistenza e conteggio dei Pasti.
    * `StatItem`: Un helper per visualizzare una singola statistica.
    * Elenco delle discipline Kai possedute.
    * Pulsante per il lancio del dado (se abilitato).
* **Dipende da:** `GameCharacter`, `getIconForDiscipline`, `ItemType`.

#### 37. `immundanoctis/util/TtsPreferences.kt`
Questa classe gestisce le preferenze relative al servizio Text-to-Speech (TTS), inclusi l'abilitazione della lettura automatica, la velocità e il tono della voce, e le voci preferite per genere (maschile/femminile).
* **Scopo:** Persistere le impostazioni del Text-to-Speech.
* **Funzionalità chiave:**
    * `saveAutoRead(enabled: Boolean)` / `isAutoReadEnabled()`: Per la lettura automatica.
    * `saveSpeechRate(rate: Float)` / `getSpeechRate()`: Per la velocità della voce.
    * `savePitch(pitch: Float)` / `getPitch()`: Per il tono della voce.
    * `saveVoiceForGender(gender: String, voiceName: String?)` / `getVoiceForGender(gender: String)`: Per le voci specifiche per genere.
* **Costanti:** `TTS_PREFS_NAME`, `KEY_AUTO_READ`, `KEY_SPEECH_RATE`, `KEY_PITCH`, `KEY_VOICE_MALE`, `KEY_VOICE_FEMALE`.

#### 38. `immundanoctis/util/GemmaPreferences.kt`
Questa classe gestisce le preferenze specifiche per il `GemmaEngine`, come la lunghezza massima dei token, la temperatura, Top-K e Top-P.
* **Scopo:** Persistere i parametri di configurazione del motore Gemma.
* **Proprietà:** `nLen`, `temperature`, `topK`, `topP`.
* **Costanti:** `PREFS_NAME` e chiavi specifiche per ogni parametro.

#### 39. `immundanoctis/AdventureActivity.kt`
Questa è l'Activity principale del gioco, la schermata dell'avventura. Mostra la chat, le scelte, le statistiche del giocatore e gestisce l'interazione con i motori AI e le preferenze.
* **Scopo:** Essere la schermata di gioco centrale dove l'utente interagisce con la storia.
* **Funzionalità chiave:**
    * Caricamento dei motori AI (`GemmaEngine`, `LlamaCppEngine`, `TranslationEngine`) e della sessione di gioco.
    * Gestione degli stati di caricamento e di errore dei motori.
    * Visualizzazione della chat con messaggi dell'AI e dell'utente.
    * Visualizzazione di scelte narrative e di disciplina.
    * Gestione del lancio dei dadi e della risoluzione delle scelte.
    * Integrazione con il servizio TTS per la lettura dei messaggi.
    * Gestione del semaforo dei token AI.
    * Opzioni di menu per salvare la chat manualmente.
* **Componenti Composable:** `LoadingScreen`, `ErrorScreen`, `AdventureChatScreen`.
* **Interagisce con:** `MainViewModel`, `ModelPreferences`, `ThemePreferences`, `TtsPreferences`, `SavePreferences`, `GameStateManager`, `GameLogicManager`, `TtsService`.

#### 40. `immundanoctis/ui/theme/Theme.kt`
Questo file definisce il tema Material Design 3 dell'applicazione, inclusi lo schema di colori (Dark e Light) e la tipografia.
* **Scopo:** Applicare uno stile visivo coerente a tutta l'applicazione.
* **Contenuto:**
    * `DarkColorScheme`: Schema di colori per il tema scuro.
    * `LightColorScheme`: Schema di colori per il tema chiaro (ora completo).
    * `ImmundaNoctisTheme`: Il Composable principale che applica il tema a `content`.
* **Dipende da:** `Color.kt` per la definizione dei colori, `Type.kt` per la tipografia.

#### 41. `immundanoctis/ModelActivity.kt`
Questa Activity funge da schermata di configurazione avanzata per i modelli AI LLM. Permette di impostare l'Hugging Face Access Token, scegliere la modalità del motore AI (solo Gemma o mista), configurare i percorsi e scaricare i modelli, e modificare i parametri avanzati di Gemma e Llama.
* **Scopo:** Configurare i motori AI LLM, inclusi download, percorsi e parametri avanzati.
* **Funzionalità chiave:**
    * Gestione dell'Hugging Face Access Token.
    * Selezione della modalità motore AI (`MIXED` o `GEMMA_ONLY`).
    * `ModelSlotView` per la gestione dei modelli DM (Gemma) e Player (GGUF).
    * Opzioni per caricare modelli da URL o selezionare file locali.
    * Parametri avanzati per Gemma (Max Tokens, Temperatura, Top-P, Top-K).
    * Parametri avanzati per Llama (Max Tokens, Temperatura, Top-P, Top-K, Repeat-P, Personalità Chatbot, Persistenza cronologia chat).
    * Pulsante per resettare la sessione chatbot.
    * Selettore di file per il `scenes.json`.
* **Interagisce con:** `MainViewModel`, `ModelPreferences`, `ThemePreferences`, `WorkManager`, `EnginePreferences`, `GemmaPreferences`, `LlamaPreferences`, `SetupViewModel`, `Downloadable`, `DownloadWorker`.
* **Componenti Composable:** `MainEngineScreen`, `AddUrlDialog`, `EngineRadioButton`, `TokenInputSection`, `SceneJsonPicker`.

#### 42. `immundanoctis/ConfigurationActivity.kt`
Questa Activity fornisce la schermata delle impostazioni generali dell'applicazione, inclusi i toggle per il salvataggio automatico e la chat, le impostazioni audio (TTS) e l'opzione per cancellare la sessione di gioco.
* **Scopo:** Offrire le impostazioni generali dell'applicazione.
* **Funzionalità chiave:**
    * Toggle per il salvataggio automatico (`SavePreferences`).
    * Toggle per abilitare/disabilitare la chat con i personaggi (`SavePreferences`).
    * Impostazioni TTS: abilitare lettura automatica, regolare velocità e tono, selezionare voci specifiche per genere.
    * Opzione per cancellare la sessione di gioco salvata (con conferma).
* **Interagisce con:** `ThemePreferences`, `TtsPreferences`, `SavePreferences`, `GameStateManager`, `TtsService`.
* **Componenti Composable:** `MainEngineScreen` (qui per le impostazioni generali), `VoiceDropdown`, `TokenInputSection`.

#### 43. `immundanoctis/stdf/ui/StdfModelSlotView.kt`
Questo componente Composable è un elemento riutilizzabile per visualizzare e gestire un singolo modello STDF all'interno di una lista (usato in `StdfModelActivity`). Mostra il nome, la descrizione, le dimensioni, la risoluzione e le azioni disponibili (scarica, elimina, seleziona, genera).
* **Scopo:** Rappresentare un singolo slot per un modello STDF nell'interfaccia utente.
* **Contenuto:** Mostra i dettagli del modello (`StdfModel`) e lo stato (`isDownloaded`, `isDownloading`, `isSelected`, `downloadProgress`).
* **Azioni:** Pulsanti per avviare il download, eliminare il modello, selezionarlo come attivo o avviare la generazione di immagini con quel modello.
* **Stili:** Utilizza bordi colorati per indicare la selezione.
* **Dipende da:** `StdfModel`.

#### 44. `immundanoctis/stdf/worker/StdfDownloadWorker.kt`
Questo `CoroutineWorker` si occupa del download di tutti i singoli file che compongono un modello STDF. A differenza del `DownloadWorker` generico, questo è specifico per i modelli STDF e gestisce il download sequenziale di più file per un unico modello, aggiornando il progresso complessivo.
* **Scopo:** Scaricare in modo coordinato tutti i file di un modello STDF.
* **Funzionalità chiave:**
    * Riceve una lista di `ModelFile` in formato JSON.
    * Itera su ogni file, scaricandolo singolarmente.
    * Verifica se i file sono già presenti e integri prima di scaricarli nuovamente.
    * Aggiorna il progresso complessivo del download del modello.
    * Utilizza `StdfFileVerification` per la persistenza delle dimensioni dei file.
* **Costanti:** `KEY_MODEL_ID`, `KEY_MODEL_FILES_JSON`, `KEY_BASE_URL`, `KEY_HF_TOKEN`, `KEY_PROGRESS`.
* **Dipende da:** `StdfModel`, `StdfFileVerification`, `ModelFile`.

#### 45. `immundanoctis/engine/rules/LoneWolfRules.kt`
Questa classe implementa l'interfaccia `GameRulesEngine` e contiene la logica specifica del regolamento di Lupo Solitario. Attualmente include il calcolo del Grado Kai e la verifica dell'uso delle discipline. La risoluzione del combattimento è ancora da implementare.
* **Scopo:** Incapsulare le regole di gioco specifiche di Lupo Solitario.
* **Funzionalità chiave:**
    * `resolveCombatRound(player: GameCharacter, enemy: GameCharacter)`: (TODO) Per risolvere un round di combattimento.
    * `canUseDiscipline(player: GameCharacter, disciplineId: String, scene: Scene)`: Verifica se un giocatore può usare una certa disciplina in una scena.
    * `getKaiRank(disciplineCount: Int)`: Calcola il grado Kai in base al numero di discipline.
* **Dipende da:** `GameCharacter`, `Scene`, `LocalizedText`.

#### 46. `immundanoctis/CharacterSheetActivity.kt`
Questa Activity visualizza la scheda dettagliata del personaggio giocante, mostrando statistiche, inventario, discipline Kai e altre informazioni rilevanti.
* **Scopo:** Visualizzare la scheda dettagliata del personaggio del giocatore.
* **Componenti Composable:** `CharacterSheetScreen`.
* **Funzionalità chiave:**
    * Visualizzazione del ritratto, nome, grado Kai e oro.
    * Statistiche di Combattività e Resistenza.
    * Conteggio dei pasti.
    * Lista delle armi (sempre due slot, con pugni come fallback) e l'arma selezionata.
    * Lista delle discipline Kai.
    * Lista degli oggetti comuni e speciali.
* **Interagisce con:** `CharacterSheetViewModel`.
* **Componenti UI specifici:** `StatsAndMealsCard`, `WeaponsCard`, `WeaponSlot`, `KaiDisciplinesCard`, `CommonItemsCard`, `CommonItemSlot`, `SpecialItemsTableCard`.

#### 47. `immundanoctis/engine/GameRulesEngine.kt`
Questa interfaccia definisce il contratto per qualsiasi sistema di regole di gioco che l'applicazione può supportare. È un'astrazione per consentire la facile integrazione di diversi set di regole.
* **Scopo:** Definire l'API per un sistema di regole di gioco.
* **Data class:** `CombatRoundResult`: Risultato di un round di combattimento.
* **Metodi chiave:**
    * `resolveCombatRound(...)`: Risolve un round di combattimento.
    * `canUseDiscipline(...)`: Verifica l'uso di una disciplina.
    * `getKaiRank(...)`: Calcola il grado Kai.

#### 48. `immundanoctis/service/TtsService.kt`
Questo servizio incapsula l'API di Android TextToSpeech (TTS) per convertire il testo in parlato. Gestisce l'inizializzazione del motore TTS, la selezione della voce (anche in base al genere del personaggio) e la riproduzione del testo.
* **Scopo:** Fornire funzionalità Text-to-Speech all'applicazione.
* **Funzionalità chiave:**
    * Inizializzazione del motore TTS.
    * `speak(text: String, character: GameCharacter)`: Legge il testo, tentando di selezionare una voce adatta al genere del personaggio (utilizzando preferenze salvate o keyword).
    * `getAvailableVoices()`: Restituisce una lista delle voci TTS disponibili sul dispositivo.
    * `shutdown()`: Rilascia le risorse del motore TTS.
* **Dipende da:** `TextToSpeech` (Android SDK), `GameCharacter`, `TtsPreferences`.

#### 49. `immundanoctis/util/LlamaPreferences.kt`
Questa classe gestisce le preferenze specifiche per il `LlamaCppEngine`, come la lunghezza massima dei token, la temperatura, Top-K, Top-P, la penalità di ripetizione e la personalità del chatbot.
* **Scopo:** Persistere i parametri di configurazione del motore Llama/GGUF.
* **Proprietà:** `nLen`, `temperature`, `topK`, `topP`, `repeatP`, `chatbotPersonality`, `isChatHistoryEnabled`.
* **Costanti:** `PREFS_NAME` e chiavi specifiche per ogni parametro.

#### 50. `immundanoctis/util/EnginePreferences.kt`
Questa classe gestisce una singola preferenza per la scelta della modalità del motore AI: se usare Gemma per tutti i personaggi (modalità `Solo Gemma`) o una combinazione di Gemma e GGUF (modalità `Mista`).
* **Scopo:** Persistere l'opzione di configurazione globale dei motori AI.
* **Proprietà:** `useGemmaForAll`: `true` per Solo Gemma, `false` per Modalità Mista.
* **Costanti:** `PREFS_NAME`, `KEY_USE_GEMMA_FOR_ALL`.

#### 51. `immundanoctis/engine/GemmaEngine.kt`
Questa implementazione dell'interfaccia `InferenceEngine` utilizza la libreria MediaPipe per i modelli Gemma. È ottimizzata per i modelli Gemma e gestisce la sessione di inferenza, l'input/output di testo e l'aggiornamento dell'utilizzo dei token.
* **Scopo:** Fornire un motore di inferenza per i modelli Gemma (Dungeon Master).
* **Funzionalità chiave:**
    * `load(modelPath: String, chatbotPersonality: String?)`: Carica il modello Gemma e crea una sessione.
    * `sendMessage(text: String)`: Invia il testo al modello e restituisce un `Flow<String>` di token in streaming.
    * `resetSession(systemPrompt: String?)`: Resetta la sessione del modello e inietta un prompt di sistema.
    * `unload()`: Rilascia le risorse del modello.
    * `getTokensUsed()`: Restituisce il conteggio dei token utilizzati.
    * Aggiorna la `tokenInfo` in base all'utilizzo.
* **Dipende da:** `LlmInference` (MediaPipe), `GemmaPreferences`.

Certamente! Procedo con l'analisi dei file JSON e Gradle che mi hai allegato, aggiungendoli al quadro complessivo del tuo progetto.

### Analisi Aggiuntiva dei File:

#### 1. `scenes.json`
Questo file JSON è fondamentale per il tuo gioco, poiché contiene la definizione di tutte le scene dell'avventura. [cite_start]Ogni oggetto "scena" ha un ID unico e definisce il tipo di scena (`START`, `TRANSITION`, `ENDING`), il genere (`FANTASY`), il livello di difficoltà (`challengeLevel`), il testo narrativo (in inglese e italiano), eventuali immagini, le scelte narrative e le scelte di disciplina disponibili. [cite: 1]

* **Scopo:** Definire la struttura e il contenuto delle diverse scene che compongono la storia del gioco.
* **Struttura chiave:**
    * [cite_start]`scenes`: Un array che contiene più oggetti scena. [cite: 1]
    * [cite_start]`id`: Identificatore unico della scena (es. "1", "2"). [cite: 1]
    * [cite_start]`sceneType`: Indica se la scena è un punto di inizio (`START`), di transizione (`TRANSITION`) o di fine (`ENDING`). [cite: 1]
    * [cite_start]`genre`: Il genere della scena (es. `FANTASY`). [cite: 1]
    * [cite_start]`challengeLevel`: Livello di difficoltà della scena (es. `BASE`, `MASTER`). [cite: 1]
    * [cite_start]`narrativeText`: Contiene il testo della narrazione in diverse lingue (inglese e italiano). [cite: 1]
    * [cite_start]`images`: Un array opzionale di oggetti immagine, ciascuno con un `imageUrl` e una `caption` localizzata. [cite: 1]
    * `choices`: Un array di `NarrativeChoice`, ognuno con un `id`, `choiceText` localizzato e `nextSceneId` per la navigazione. [cite_start]Possono includere `minRoll` e `maxRoll` per le scelte basate su lancio di dadi. [cite: 1]
    * [cite_start]`disciplineChoices`: Un array di `DisciplineChoice`, collegato a una `disciplineId` e un `nextSceneId`. [cite: 1]
    * [cite_start]`location`: Oggetto che descrive la località della scena, con `areaName` localizzato, coordinate e un flag `isMajorLocation`. [cite: 1]
* **Relazione con il codice:** Questa struttura JSON è caricata e interpretata dalla classe `GameLogicManager.kt`, che la utilizza per gestire il flusso del gioco.

#### 2. `build.gradle-project.txt`
Questo è il file `build.gradle` di livello superiore (root project) del tuo progetto Android. Definisce le configurazioni comuni a tutti i moduli del progetto. Contiene le definizioni dei plugin Gradle utilizzati e, in questo caso, due variabili `extra` per abilitare o disabilitare la compilazione di moduli specifici.

* **Scopo:** Configurazione a livello di progetto, inclusa la gestione dei plugin e delle variabili globali di build.
* **Contenuto chiave:**
    * [cite_start]`plugins`: Dichiarazioni dei plugin Gradle applicati (es. `android.application`, `kotlin.android`, `kotlin.compose`, `android.library`). [cite: 2] L'uso di `apply false` indica che questi plugin saranno applicati nei singoli moduli.
    * [cite_start]`extra["buildLlama"] = true`: Questa variabile indica che il modulo `:llama` (presumibilmente contenente l'implementazione per `llama.cpp`) deve essere incluso nel processo di build. [cite: 2]
    * [cite_start]`extra["buildStdf"] = false`: Questa variabile indica che il modulo `:stdf` (presumibilmente per Stable Diffusion) è disabilitato per la build corrente. [cite: 2]
* **Relazione con il codice:** Queste variabili `extra` sono utili per il controllo condizionale della build, permettendo di includere o escludere moduli complessi senza modificare i file `build.gradle` dei sottomoduli.

#### 3. `build.gradle-app.txt`
Questo è il file `build.gradle` specifico del modulo `app`. Definisce le configurazioni di build e le dipendenze per l'applicazione principale.

* **Scopo:** Configurare il modulo dell'applicazione Android e dichiarare tutte le librerie e i moduli di cui dipende.
* **Contenuto chiave:**
    * [cite_start]`plugins`: Applica i plugin Android e Kotlin per l'applicazione. [cite: 3]
    * `android` blocco:
        * [cite_start]`namespace`: Definisce il namespace del package Android ("io.github.luposolitario.immundanoctis"). [cite: 3]
        * [cite_start]`compileSdk`: Specifica la versione SDK con cui compilare (35). [cite: 3]
        * [cite_start]`defaultConfig`: Contiene configurazioni di base come `applicationId`, `minSdk` (34), `targetSdk` (34), `versionCode`, `versionName`. [cite: 3] [cite_start]Include anche `testInstrumentationRunner` e `vectorDrawables`. [cite: 3, 4]
        * [cite_start]`sourceSets`: Specifica le directory per le librerie JNI (`jniLibs.srcDirs("src/main/jniLibs")`). [cite: 4]
        * [cite_start]`buildTypes`: Definisce configurazioni per tipi di build (qui, `release` con `isMinifyEnabled = false` e `proguardFiles`). [cite: 5]
        * [cite_start]`compileOptions`: Imposta la compatibilità con Java 8. [cite: 5]
        * [cite_start]`kotlinOptions`: Imposta il `jvmTarget` a "1.8". [cite: 6]
        * [cite_start]`buildFeatures { compose = true }`: Abilita il supporto a Jetpack Compose. [cite: 6]
    * `dependencies` blocco:
        * [cite_start]Dichiarazioni di dipendenze per librerie AndroidX (es. `core-ktx`, `lifecycle-runtime-ktx`, `activity-compose`, `material3`, `material-icons-extended`). [cite: 7]
        * [cite_start]Dipendenze specifiche per i modelli e il motore: `tasks-genai` (per Gemma), `gson`, `translate` (per ML Kit Translate), `jackson-module-kotlin`, `jackson-databind` (per la gestione JSON dei tag), `language-id` (per ML Kit Language ID), `okhttp-sse`. [cite: 7]
        * [cite_start]Integrazione dei moduli locali: `implementation(project(":llama"))` e `implementation(project(":stdf"))`, indicando che l'app dipende da questi sottomoduli. [cite: 7]
        * [cite_start]Dipendenze per i test (JUnit, Mockito). [cite: 7, 8, 9, 10, 11]
* **Relazione con il codice:** Questo file è cruciale perché elenca tutte le librerie e i sottomoduli che la tua app utilizza, permettendo al compilatore di risolvere i riferimenti e creare l'APK finale. L'inclusione di `:llama` e `:stdf` qui, insieme alle variabili `buildLlama` e `buildStdf` nel `build.gradle-project.txt`, è ciò che controlla quali parti del codice dei motori AI vengono incluse nell'app.

Questi file, insieme alle classi Kotlin analizzate in precedenza, delineano chiaramente l'architettura del tuo progetto, dalla logica di gioco ai motori AI, alla UI e alla gestione delle preferenze e dei download.

Sì, con il file `AndroidManifest.xml` e il file `network_security_config.xml` ora ho un quadro completo del tuo progetto. Grazie per averli forniti!

Ecco l'analisi del tuo `AndroidManifest.xml`:

### Analisi del file `AndroidManifest.xml`

Il file `AndroidManifest.xml` è il cuore di ogni applicazione Android, poiché descrive al sistema operativo le componenti dell'app (Activity, Service, BroadcastReceiver, ContentProvider), i permessi necessari e le configurazioni generali.

* **Scopo:** Dichiarare le componenti dell'applicazione al sistema Android, definire i permessi richiesti e configurare le caratteristiche principali dell'app.

#### Sezione `uses-permission`:
Questa sezione dichiara i permessi che l'applicazione richiede all'utente o al sistema per funzionare correttamente.
* `android.permission.FOREGROUND_SERVICE_DATA_SYNC`: Questo permesso è richiesto per i servizi in primo piano di tipo `dataSync`, comunemente utilizzati per operazioni di sincronizzazione dati che devono continuare anche quando l'app non è in primo piano. [cite_start]È particolarmente rilevante per Android 14 (API 34) e successivi[cite: 1].
* [cite_start]`android.permission.FOREGROUND_SERVICE`: Un permesso generico per l'uso dei servizi in primo piano, necessario per tutte le versioni di Android più recenti per indicare al sistema che l'app esegue operazioni importanti in background che non devono essere interrotte[cite: 1].
* [cite_start]`android.permission.INTERNET`: Permette all'applicazione di accedere alla rete, indispensabile per il download dei modelli e per la comunicazione con il backend locale[cite: 1].
* [cite_start]`android.permission.POST_NOTIFICATIONS`: Necessario per consentire all'app di inviare notifiche all'utente (ad esempio, per il progresso dei download o lo stato dei servizi in background)[cite: 1].

#### Sezione `application`:
Questo tag racchiude le dichiarazioni di tutte le componenti dell'applicazione e le configurazioni a livello globale.
* `android:allowBackup="true"`: Permette al sistema di eseguire il backup dei dati dell'applicazione.
* `android:dataExtractionRules="@xml/data_extraction_rules"`: Specifica le regole per l'estrazione dei dati utente in Android 12 e successivi.
* `android:extractNativeLibs="true"`: Indica che le librerie native (come quelle usate da `llama.cpp` o il backend STDF) devono essere estratte nel filesystem dell'app. [cite_start]Questo è cruciale per l'esecuzione del backend C++[cite: 1].
* `android:fullBackupContent="@xml/backup_rules"`: Specifica le regole per il backup completo dei contenuti dell'app.
* `android:icon` e `android:roundIcon`: Definiscono le icone dell'applicazione.
* `android:label="@string/app_name"`: Imposta il nome visualizzato dell'applicazione.
* `android:networkSecurityConfig="@xml/network_security_config"`: Questo attributo punta a un file XML che contiene configurazioni di sicurezza di rete. Analizzeremo questo file separatamente. [cite_start]È fondamentale per permettere connessioni HTTP in chiaro a `localhost`, necessarie per la comunicazione con il backend STDF che gira localmente[cite: 1].
* `android:supportsRtl="true"`: Abilita il supporto per layout da destra a sinistra.
* `android:theme="@style/Theme.ImmundaNoctis"`: Applica il tema generale dell'applicazione (definito in `ui/theme/Theme.kt`).
* `tools:targetApi="35"`: Specifica che l'applicazione è stata testata con API 35 (Android 15), utilizzata per avvertimenti o comportamenti specifici di quella versione.

#### Dichiarazioni `activity`:
Ogni tag `activity` dichiara una schermata (Activity) dell'applicazione.
* `android:exported="false"`: Per la maggior parte delle Activity, `exported="false"` indica che non possono essere avviate da applicazioni esterne. [cite_start]Questo è un buon default per ragioni di sicurezza, limitando l'accesso solo all'app stessa[cite: 1].
* Ogni Activity ha un `android:name` che punta alla classe Kotlin corrispondente (es. `.AdventureActivity`, `.SetupActivity`, `.CharacterSheetActivity`, `.ConfigurationActivity`, `.ModelActivity`, `.StdfGenerationActivity`, `.StdfModelActivity`).
* `MainActivity`: Questa è l'Activity principale, il punto di ingresso dell'app. [cite_start]È dichiarata con `android:exported="true"` e contiene un `intent-filter` con `android.intent.action.MAIN` e `android.intent.category.LAUNCHER`, il che la rende l'Activity che viene avviata quando l'utente tocca l'icona dell'app nel launcher[cite: 1].

#### Dichiarazioni `service`:
Ogni tag `service` dichiara un servizio in background dell'applicazione.
* [cite_start]`android:exported="false"`: Anche per i servizi, `exported="false"` è una buona pratica di sicurezza per prevenire l'avvio da parte di altre app[cite: 1].
* `android:foregroundServiceType="dataSync"`: Questo attributo è essenziale, specialmente su Android 14 (API 34) e successivi. Classifica il tipo di servizio in primo piano, specificando che esegue operazioni di sincronizzazione dati. [cite_start]Questo è in linea con le operazioni di download dei modelli e il funzionamento del backend STDF[cite: 1].
    * [cite_start]`.stdf.service.BackendService`: Il servizio che avvia il processo C++ del backend di generazione immagini[cite: 1].
    * [cite_start]`.stdf.service.BackgroundGenerationService`: Il servizio che gestisce la generazione effettiva delle immagini in background[cite: 1].
    * [cite_start]`androidx.work.impl.foreground.SystemForegroundService`: Questo servizio fa parte della libreria WorkManager e viene utilizzato per eseguire WorkRequest (come i tuoi `DownloadWorker` e `StdfDownloadWorker`) come servizi in primo piano quando necessario[cite: 1].

---

### Analisi del file `network_security_config.xml`

Questo file XML definisce le impostazioni di sicurezza della rete per l'applicazione, in particolare per quanto riguarda il traffico HTTP in chiaro.

* **Scopo:** Configurare le politiche di sicurezza di rete dell'applicazione, specialmente per consentire connessioni HTTP in chiaro a domini specifici.
* **Contenuto chiave:**
    * `<base-config cleartextTrafficPermitted="true">`: Questa configurazione di base permette il traffico HTTP in chiaro (non crittografato) per tutti i domini. [cite_start]Sebbene sia comodo per lo sviluppo o per specifici casi d'uso (come un backend locale), in un'applicazione di produzione che si connette a server remoti, si preferisce solitamente `false` per motivi di sicurezza[cite: 2].
    * `<trust-anchors>`: Specifica quali certificati sono considerati affidabili. [cite_start]`system` si riferisce ai certificati di sistema, `user` a quelli installati dall'utente[cite: 2].
    * `<domain-config cleartextTrafficPermitted="true">`: Questo blocco sovrascrive la configurazione di base per specifici domini. In questo caso, riafferma che il traffico in chiaro è permesso.
    * [cite_start]`<domain includeSubdomains="true">localhost</domain>` [cite: 2]
    * [cite_start]`<domain includeSubdomains="true">127.0.0.1</domain>` [cite: 2]
        * Questi due tag sono **fondamentali** per il tuo progetto. Essi consentono esplicitamente al tuo codice Android di connettersi a `localhost` (o `127.0.0.1`) utilizzando traffico HTTP in chiaro. [cite_start]Questo è indispensabile per la comunicazione tra l'app Android e il backend STDF C++ che, come abbiamo visto nel `BackendService.kt`, gira su `http://localhost:8081`[cite: 2, 3]. Senza questa configurazione, Android bloccherebbe le connessioni non HTTPS a questi indirizzi locali.

---

**Considerazioni generali:**

Il tuo `AndroidManifest.xml` è ben strutturato e dichiara correttamente tutte le componenti necessarie. L'uso dei `foregroundServiceType="dataSync"` è un'ottima pratica per la compatibilità con le versioni più recenti di Android, specialmente per i servizi che gestiscono download o processi continui.

La configurazione della sicurezza di rete è appropriata per il tuo caso d'uso specifico di un backend locale, consentendo le connessioni HTTP a `localhost`.

Ora direi che abbiamo tutti gli elementi principali per procedere con l'analisi più approfondita o eventuali modifiche future.

(consiglio giro di test RUN o DEBUG)