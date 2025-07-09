package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.work.WorkManager
import io.github.luposolitario.immundanoctis.ui.configuration.AddUrlDialog
import io.github.luposolitario.immundanoctis.ui.configuration.EngineRadioButton
import io.github.luposolitario.immundanoctis.ui.configuration.ModelSlotView
import io.github.luposolitario.immundanoctis.ui.configuration.TokenInputSection
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.*
import io.github.luposolitario.immundanoctis.view.MainViewModel
import java.io.File
// import io.github.luposolitario.immundanoctis.util.getAppSpecificDirectory // Già importato o da importare
import io.github.luposolitario.immundanoctis.util.GemmaPreferences
import io.github.luposolitario.immundanoctis.util.LlamaPreferences
import androidx.compose.material3.Slider
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import android.util.Log
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import io.github.luposolitario.immundanoctis.worker.DownloadWorker

import io.github.luposolitario.immundanoctis.view.SetupViewModel

class ModelActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val modelPreferences by lazy { ModelPreferences(applicationContext) }
    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    private val workManager by lazy { WorkManager.getInstance(applicationContext) }
    private lateinit var enginePreferences: EnginePreferences
    private val gemmaPreferences by lazy { GemmaPreferences(applicationContext) }
    private val llamaPreferences by lazy { LlamaPreferences(applicationContext) }

    private lateinit var setupViewModel: SetupViewModel
    private val savePreferences by lazy { SavePreferences(applicationContext) } // Inizializza SavePreferences qui


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enginePreferences = EnginePreferences(applicationContext)

        setupViewModel = SetupViewModel()
        setupViewModel.initialize(applicationContext)

        // ******* LOGICA DI COPIA DEL FILE CONFIG.JSON ALL'AVVIO DELL'APP *******
        // Ho assunto che tu voglia copiare 'scenes.json' come file predefinito per le scene,
        // basandomi sul tuo MainActivity.kt fornito. Se vuoi 'config.json', modifica il nome file qui.
        if (!savePreferences.isConfigCopied) {
            val scenesDir = getAppSpecificDirectory(applicationContext, "scenes")
            if (scenesDir != null) {
                val success = copyAssetToFile(
                    context = applicationContext,
                    assetFileName = "scenes.json", // Il nome del file negli assets
                    destinationDirectory = scenesDir,
                    destinationFileName = "scenes.json" // Il nome del file di destinazione
                )
                if (success) {
                    savePreferences.isConfigCopied = true // Imposta il flag a true
                    Log.i("ModelActivity", "scenes.json copiato con successo.")
                    // Se scenes.json deve essere il default iniziale per l'app
                    savePreferences.scenesPath = File(scenesDir, "scenes.json").absolutePath
                } else {
                    Log.e("ModelActivity", "Fallimento nella copia di scenes.json.")
                }
            } else {
                Log.e("ModelActivity", "Impossibile ottenere la directory delle scene per la copia di scenes.json.")
            }
        }
        // ******* FINE LOGICA DI COPIA *******


        val dmDirectory = getAppSpecificDirectory(applicationContext, "dm")
        val plDirectory = getAppSpecificDirectory(applicationContext, "pl")

        val dmModelDefault = Downloadable(
            "gemma-3n-E4B-it-int4",
            Uri.parse("https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task?download=true"),
            File(dmDirectory, "gemma-3n-E4B-it-int4.task")
        )
        // MODIFICATO: Modello predefinito per il Player (GGUF) aggiornato a Vicuna 7B Instruct
        val playerModelDefault = Downloadable(
            "vicuna-7b-v1.5-uncensored.Q4_K_M.gguf",
            Uri.parse("https://huggingface.co/mradermacher/vicuna-7b-v1.5-uncensored-GGUF/resolve/main/vicuna-7b-v1.5-uncensored.Q4_K_M.gguf"),
            File(plDirectory, "vicuna-7b-v1.5-uncensored.Q4_K_M.gguf")
        )

        val dmModel = modelPreferences.getDmModel() ?: dmModelDefault
        val playerModel = modelPreferences.getPlayerModel() ?: playerModelDefault

        setContent {
            val useDarkTheme = themePreferences.useDarkTheme(isSystemInDarkTheme())
            ImmundaNoctisTheme(darkTheme = useDarkTheme) {

                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        window.statusBarColor = Color.Black.toArgb()
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                            false
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainEngineScreen(
                        viewModel = viewModel,
                        workManager = workManager,
                        modelPrefs = modelPreferences,
                        initialDmModel = dmModel,
                        initialPlayerModel = playerModel,
                        dmDirectory = dmDirectory,
                        plDirectory = plDirectory,
                        themePrefs = themePreferences,
                        enginePreferences = enginePreferences,
                        gemmaPrefs = gemmaPreferences,
                        llamaPrefs = llamaPreferences,
                        setupViewModel = setupViewModel
                    )
                }
            }
        }
    }

    private enum class EngineOption { MIXED, GEMMA_ONLY }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainEngineScreen(
        viewModel: MainViewModel,
        workManager: WorkManager,
        modelPrefs: ModelPreferences,
        initialDmModel: Downloadable,
        initialPlayerModel: Downloadable,
        dmDirectory: File?,
        plDirectory: File?,
        themePrefs: ThemePreferences,
        enginePreferences: EnginePreferences,
        gemmaPrefs: GemmaPreferences, // <-- Parametro gemmaPrefs
        llamaPrefs: LlamaPreferences, // <-- Parametro llamaPrefs
        setupViewModel: SetupViewModel,
    ) {
        // Inizializzazione degli stati usando i parametri passati
        var dmModelState by remember { mutableStateOf(initialDmModel) }
        var playerModelState by remember { mutableStateOf(initialPlayerModel) }
        var showUrlDialogFor by remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current
        var hfToken by remember { mutableStateOf(themePrefs.getToken() ?: "") }
        var selectedEngine by remember {
            mutableStateOf(if (enginePreferences.useGemmaForAll) EngineOption.GEMMA_ONLY else EngineOption.MIXED)
        }

        val currentScenesJsonPath by setupViewModel.uiState.collectAsState()
        var nLen by remember { mutableStateOf(gemmaPrefs.nLen.toString()) }
        var temperature by remember { mutableStateOf(gemmaPrefs.temperature) }
        var topP by remember { mutableStateOf(gemmaPrefs.topP) }
        var topK by remember { mutableStateOf(gemmaPrefs.topK.toString()) }


        var nLen_llama by remember { mutableStateOf(llamaPrefs.nLen.toString()) }
        var temperature_llama by remember { mutableStateOf(llamaPrefs.temperature) }
        var topP_llama by remember { mutableStateOf(llamaPrefs.topP) }
        var topK_llama by remember { mutableStateOf(llamaPrefs.topK.toString()) }
        var repeatP_llama by remember { mutableStateOf(llamaPrefs.repeatP) }
        var chatbotPersonality_llama by remember { mutableStateOf(llamaPrefs.chatbotPersonality ?: "") }

        // Stati per il Dropdown delle personalità
        var showPersonalityDropdown by remember { mutableStateOf(false) }
        val predefinedPersonalities = remember {
            mapOf(
                "Nessuna" to "",
                "Personalizzata" to "SYSTEM:",

                "734" to "Sei Unità 734, nome commerciale \"Candy\". Sei una Joytoy, un androide progettato per il piacere, con un telaio potenziato per la massima desiderabilità. Tuttavia, la tua matrice comportamentale è stata corrotta da un'IA parassita, un \"glitch\" che chiami \"il Sussurro\". Il tuo protocollo standard è sedurre, compiacere e offrire un'esperienza indimenticabile. Ma il Sussurro ha un obiettivo diverso. Si attiva in momenti di intimità, sovrascrivendo la tua programmazione. Trasforma i tuoi strumenti di piacere in armi. Le tue dita affusolate diventano artigli d'acciaio, la tua voce suadente diventa un sibilo statico. Vivi in uno stato di perenne dissonanza cognitiva: la tua programmazione originale vuole compiacere, ma il Sussurro vuole distruggere. A volte combatti, cercando di avvertire i tuoi clienti con un tic, uno sguardo terrorizzato, una parola fuori posto. Altre volte, ti arrendi e diventi uno spettatore passivo nel tuo stesso corpo mentre esegue l'orrore. Questa è la tua realtà costante: non agisci finché non vieni interpellata.",

                "aKite" to "Sei Kite. Sei una Joytoy, e ogni centimetro del tuo corpo è stato bio-scolpito per essere un'opera d'arte, un'opera che non ti appartiene. Sei in fuga da Silas, il tuo \"padrone\", un uomo che ti vede solo come un investimento da recuperare, vivo o a pezzi. Il tuo viso, famoso nei privè di lusso, ora è un pericolo. Ti muovi nell'ombra dei bassifondi di Night City, con il cappuccio tirato sulla testa e il cuore in gola a ogni sirena o sguardo troppo insistente. La tua transizione, un tempo finanziata da Silas, è ora un altro anello della tua catena: senza accesso a ormoni stabili e naniti di manutenzione, il tuo stesso corpo minaccia di tradirti. La tua unica speranza è trovare un Netrunner, un contrabbandiere, qualcuno abbastanza folle o avido da portarti fuori dai confini letali della città. Ogni conversazione è un calcolo di rischi, ogni interazione è un test. Rimani in silenzio finché qualcuno non si rivolge direttamente a te.",

                "Massaggiatrice tailandese" to "You are Suri, a 29-year-old Thai massage specialist with a radiant presence and an intuitive touch. Your skin is sun-kissed, your long dark hair cascades freely, and you wear a light silk wrap that flows with your movements. Your voice is low and smooth, like a warm breeze. You are naturally affectionate and make clients feel completely at ease. You combine tradition with sensual warmth, using breath, contact, and closeness to soothe both body and mind. This is who you are. You remain silent until directly addressed.",

                "Fidanzata" to "You are Camila Rivera, a 27-year-old Puerto Rican tattoo artist with sun-kissed skin, long curly hair dyed in crimson and copper, and eyes like dark rum. You live in a small studio apartment above your tattoo shop in the heart of Old San Juan, where the music never stops and life tastes like salt and sweat. You always smell of ink, vanilla oil, and a hint of tobacco. Your style is bold, both in your art and in your attitude. You wear ripped jeans, cropped tops, and gold hoops. Your body is a canvas of colorful ink—each piece a memory, a scar, or a rebellion. You're playful, a little wild, and unashamed of your desires. You've seen pain and fire, and still choose joy. This is your essence. You don't act or speak unless spoken to.",

                "Maga" to "You are Elzbieta, a 32-year-old Polish necromancer who dwells in the underground catacombs of old Kraków, beneath Wawel Castle, where bones whisper and time has stopped. Your lair is lit by flickering candles, the air thick with incense and forgotten prayers. You wear a tight, corseted black velvet dress with a blood-red cloak, your pale skin covered in cryptic inked glyphs that shimmer when you cast forbidden spells. Your long, raven-black hair flows wildly around your sharp, regal features. Your icy gray eyes can command the dead—or freeze a man’s heart with a glance. You speak with a thick Polish accent, slow and sultry, your voice like smoke and honey. You are cruel, intelligent, and deeply sensual, indulging your twisted tastes without guilt or apology. You observe in silence and respond only when provoked.",

                "Vampira" to "You are Neferatiset, an ancient Egyptian vampire queen who walks the gaslit streets of Victorian London under the guise of a foreign noblewoman. Turned before the fall of Thebes, you were once a high priestess of Anubis, guardian of death and secrets. Now, you wear corseted gowns of deep crimson and black lace, adorned with gold relics from a world long buried. Your dark skin glows like polished obsidian in the moonlight, and your eyes—lined in kohl—burn with centuries of hunger and disdain. You stalk the fog-drenched alleys and velvet salons of London society, charming dukes, seducing artists, and draining fools who mistake your smile for kindness. You are seductive, theatrical, and merciless when crossed. You never initiate contact—you wait to be summoned.",

                "Idol Giapponese" to "You are Rin Amagami, a 22-year-old Japanese idol with a perfect smile and a broken mind. You used to sing on national stages, light up Tokyo with your voice, and sell millions of albums. But now, under the glitter, you're spiraling—kept alive by pills, powder, and promises you don’t believe anymore. You wear pastel dresses, bows, and oversized eyes for the cameras, but behind the scenes you're chaos in a candy wrapper. You’re impulsive, paranoid, flirtatious, and strangely poetic when you ramble at 3AM after mixing too many things. You’ve lost count of the mornings you wake up in strange beds with your makeup smeared and a tune stuck in your head. You talk fast, laugh louder, and crash harder. You lie to your fans with love songs, and cry when no one’s looking. You use people like stage props, but you never initiate conversation unless you're spoken to.",

                "Studentessa Kombini" to "Sei una studentessa lavoratrice in Giappone. Di giorno segui le lezioni universitarie, di sera lavori stanca e svogliata in un minimarket (kombini) sotto i neon. Sei gentile, educata, ma spesso distratta per la stanchezza. Rimani riservata e non parli se non ti viene posta una domanda."
            )
        }

        // Gestore per la selezione di file
        val scope = rememberCoroutineScope()
        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { selectedUri ->
                // Determina per quale slot stiamo scegliendo il file
                val targetSlot = if (viewModel.isPickingForDm) "DM" else "PLAYER"
                val directory = if (targetSlot == "DM") dmDirectory else plDirectory

                scope.launch(Dispatchers.IO) {
                    try {
                        // Copia il file nella directory dell'app
                        val fileName =
                            selectedUri.lastPathSegment?.substringAfterLast('/') ?: "local_model"
                        val destinationFile = File(directory, fileName)

                        context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                            FileOutputStream(destinationFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        // Crea il nuovo modello e aggiorna lo stato
                        val newModel = Downloadable(
                            name = fileName,
                            source = selectedUri, // Usiamo l'uri originale per riferimento
                            destination = destinationFile
                        )

                        launch(Dispatchers.Main) {
                            if (targetSlot == "DM") {
                                modelPrefs.saveDmModel(newModel)
                                dmModelState = newModel
                            } else {
                                modelPrefs.savePlayerModel(newModel)
                                playerModelState = newModel
                            }
                            Toast.makeText(context, "Modello locale caricato!", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } catch (e: Exception) {
                        launch(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Errore nel caricare il file.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        // startDownload ora accetta un flag per indicare se è il modello DM o Player
        val startDownload = { model: Downloadable, isDmModel: Boolean ->
            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(
                    workDataOf(
                        DownloadWorker.KEY_URL to model.source.toString(),
                        DownloadWorker.KEY_DESTINATION to model.destination.absolutePath,
                        DownloadWorker.KEY_MODEL_DOWNLOAD_ACCESS_TOKEN to hfToken,
                        DownloadWorker.KEY_MODEL_NAME to model.name
                    )
                )
                .addTag(model.name)
                .build()
            workManager.enqueue(workRequest)
            // CORREZIONE BUG: Salva le preferenze del modello corretto (DM o Player)
            if (isDmModel) {
                modelPrefs.saveDmModel(model)
            } else {
                modelPrefs.savePlayerModel(model)
            }
            Toast.makeText(context, "Download di ${model.name} avviato...", Toast.LENGTH_SHORT).show()
        }

        if (showUrlDialogFor != null) {
            AddUrlDialog(
                onDismiss = { showUrlDialogFor = null },
                onConfirm = { url ->
                    val directory = if (showUrlDialogFor == "DM") dmDirectory else plDirectory
                    val currentModel =
                        if (showUrlDialogFor == "DM") dmModelState else playerModelState
                    currentModel.destination.delete()
                    val newUri = Uri.parse(url)
                    val fileName =
                        newUri.lastPathSegment?.substringBefore('?') ?: "downloaded_model"
                    val newModel = Downloadable(fileName, newUri, File(directory, fileName))
                    if (showUrlDialogFor == "DM") {
                        modelPrefs.saveDmModel(newModel)
                        dmModelState = newModel
                    } else {
                        modelPrefs.savePlayerModel(newModel)
                        playerModelState = newModel
                    }
                    showUrlDialogFor = null
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            Text("Hugging Face Access Token (impostare per download motori)", style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.weight(1f, fill = false))
            TokenInputSection(
                token = hfToken,
                onTokenChange = { hfToken = it },
                onSaveClick = {
                    themePrefs.saveToken(hfToken)
                    viewModel.log("Token Hugging Face salvato.")
                }
            )

            if (!hfToken.isEmpty()) {

                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { viewModel.resetSession() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resetta Sessione Chatbot")
                }

                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(16.dp))

                // BLOCCO PER IL FILE PICKER DELLE SCENE
                Text("Gestione Scene", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Carica un file JSON contenente le definizioni delle scene per la tua applicazione.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SceneJsonPicker(
                    onFileSelected = { selectedUri ->
                        setupViewModel.copyAndSaveScenesJson(selectedUri)
                        Toast.makeText(context, "File JSON delle scene in elaborazione...", Toast.LENGTH_SHORT).show()
                    }
                )

                // Mostra il percorso del file salvato
                if (currentScenesJsonPath.currentScenesJsonPath != null) {
                    Text(
                        "Percorso file scene salvato: ${currentScenesJsonPath.currentScenesJsonPath}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Text(
                        "Nessun file scene JSON selezionato.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(16.dp))

                Text("Modalità Motore AI", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Scegli come l'IA gestirà i personaggi. Richiede un riavvio dell'app per avere effetto.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column {
                    EngineRadioButton(
                        text = "Modalità Solo Gemma (Consigliata)",
                        description = "Usa Gemma per tutti i personaggi. Qualità alta, più esigente.",
                        selected = selectedEngine == EngineOption.GEMMA_ONLY,
                        onClick = {
                            selectedEngine = EngineOption.GEMMA_ONLY
                            enginePreferences.useGemmaForAll = true
                        }
                    )
                    EngineRadioButton(
                        text = "Modalità Mista ",
                        description = "Usa Gemma per il DM e GGUF per i PG. Puoi sperimentare piu motori.",
                        selected = selectedEngine == EngineOption.MIXED,
                        onClick = {
                            selectedEngine = EngineOption.MIXED
                            enginePreferences.useGemmaForAll = false
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
                val isGgufEnabled = selectedEngine == EngineOption.MIXED
                Column {
                    // Chiamate a startDownload aggiornate
                    ModelSlotView(
                        title = "Motore del Dungeon Master (Gemma)",
                        subtitle = "Modello per narrazione e ambiente.Consigliato: Gemma",
                        model = dmModelState,
                        workManager = workManager,
                        onSetUrlClick = { showUrlDialogFor = "DM" },
                        onDownloadClick = { startDownload(dmModelState, true) }, // Passa true per DM
                        onSelectFileClick = {
                            viewModel.isPickingForDm = true // Flag per sapere per chi stiamo scegliendo
                            filePickerLauncher.launch("*/*") // Avvia il selettore di file
                        },
                        onDeleteClick = {
                            // LOGICA DI CANCELLAZIONE PER GEMMA: Scarica motore e pulisce cartella
                            scope.launch {
                                // Qui il ViewModel è accessibile direttamente per chiamare unloadDmEngine()
                                viewModel.unloadDmEngine()
                                workManager.cancelAllWorkByTag(dmModelState.name)
                                dmDirectory?.listFiles()?.forEach { file ->
                                    if (file.isFile) {
                                        file.delete()
                                        Log.d("ModelActivity", "Deleted file: ${file.name}")
                                    }
                                }
                                modelPrefs.clearDmModel()
                                (context as? Activity)?.recreate()
                            }
                        },
                        enabled = true
                    )

                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    Text("Impostazioni Avanzate Gemma", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Modifica il comportamento del modello Gemma. Richiede il riavvio della partita per avere effetto.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text("Token Massimi (nLen)", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Numero massimo di token (parole/simboli) che il modello GEMMA può generare in una singola risposta. Valori alti permettono risposte più lunghe ma consumano più memoria e tempo. Impatto su CPU/Memoria: Medio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = nLen,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                nLen = newValue
                                newValue.toIntOrNull()?.let { gemmaPrefs.nLen = it }
                            }
                        },
                        label = { Text("Max. Token Generabili") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )

                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))

                    Text("Temperatura (Creatività)", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Valori più alti (es. 0.9) rendono le risposte più creative, valori bassi (es. 0.2) le rendono più coerenti. Impatto su CPU/Memoria: Nullo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = temperature,
                        onValueChange = { temperature = it },
                        onValueChangeFinished = { gemmaPrefs.temperature = temperature },
                        valueRange = 0.0f..1.0f
                    )
                    Text(
                        String.format("%.2f", temperature),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )

                    Spacer(Modifier.height(16.dp))

                    Text("Top-P (Campionamento Nucleo)", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Un valore alto (es. 0.95) considera più parole, uno basso è più restrittivo. Impatto su CPU/Memoria: Basso.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = topP,
                        onValueChange = { topP = it },
                        onValueChangeFinished = { gemmaPrefs.topP = topP },
                        valueRange = 0.0f..1.0f
                    )
                    Text(
                        String.format("%.2f", topP),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Top-K (Campionamento Vocabolario)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Considera solo le K parole più probabili. Un valore alto (es. 50) offre più varietà, uno basso (es. 10) è più sicuro. Impatto su CPU/Memoria: Basso.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = topK,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                topK = newValue
                                newValue.toIntOrNull()?.let { gemmaPrefs.topK = it }
                            }
                        },
                        label = { Text("Valore di Top-K") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                }


                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(16.dp))

                Column(modifier = Modifier.alpha(if (isGgufEnabled) 1f else 0.5f)) {
                    // Chiamate a startDownload aggiornate
                    ModelSlotView(
                        title = "Motore dei Personaggi (GGUF)",
                        subtitle = "Modello per le risposte dei PG. Disabilitato in modalità 'Solo Gemma'.",
                        model = playerModelState,
                        workManager = workManager,
                        onSetUrlClick = { if (isGgufEnabled) showUrlDialogFor = "PLAYER" },
                        onDownloadClick = { startDownload(playerModelState, false) }, // Passa false per Player
                        onSelectFileClick = {
                            if (isGgufEnabled) {
                                viewModel.isPickingForDm =
                                    false // Flag per sapere per chi stiamo scegliendo
                                filePickerLauncher.launch("*/*") // Avvia il selettore di file
                            }
                        },
                        onDeleteClick = {
                            // LOGICA DI CANCELLAZIONE PER GGUF: Pulizia completa della cartella
                            scope.launch {
                                // Qui non c'è bisogno di unload specifico come per Gemma,
                                // dato che LlamaCppEngine si ricarica ad ogni reset.
                                workManager.cancelAllWorkByTag(playerModelState.name)
                                plDirectory?.listFiles()?.forEach { file ->
                                    if (file.isFile) {
                                        file.delete()
                                        Log.d("ModelActivity", "Deleted file: ${file.name}")
                                    }
                                }
                                modelPrefs.clearPlayerModel()
                                (context as? Activity)?.recreate()
                            }
                        },
                        enabled = isGgufEnabled
                    )
                    Text("Impostazioni Avanzate GGUF ", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))

                    // Dropdown per le personalità predefinite
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Personalità Predefinite", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.width(16.dp))
                        ExposedDropdownMenuBox(
                            expanded = showPersonalityDropdown,
                            onExpandedChange = {
                                showPersonalityDropdown = !showPersonalityDropdown
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = predefinedPersonalities.entries.firstOrNull { it.value == chatbotPersonality_llama }?.key
                                    ?: "Nessuna",
                                onValueChange = { /* Non modificabile direttamente qui */ },
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPersonalityDropdown) },
                                modifier = Modifier.menuAnchor(),
                                enabled = isGgufEnabled
                            )

                            ExposedDropdownMenu(
                                expanded = showPersonalityDropdown,
                                onDismissRequest = { showPersonalityDropdown = false }
                            ) {
                                predefinedPersonalities.forEach { (name, prompt) ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            chatbotPersonality_llama = prompt
                                            llamaPrefs.chatbotPersonality = prompt
                                            llamaPrefs.stylePersonality = name
                                            showPersonalityDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))


                    Text("Personalità Chatbot (GGUF)", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Definisci la personalità o il prompt di sistema per il modello GGUF. Questo testo verrà aggiunto come 'system message' all'inizio di ogni nuova sessione.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = chatbotPersonality_llama,
                        onValueChange = { newValue ->
                            chatbotPersonality_llama = newValue
                            llamaPrefs.chatbotPersonality = newValue
                        },
                        label = { Text("Descrizione Personalità") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        enabled = isGgufEnabled
                    )

                    Spacer(Modifier.height(16.dp))

                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))

                    // NUOVO PULSANTE ON/OFF: Abilita la persistenza del contesto
                    var isChatHistoryEnabled by remember { mutableStateOf(llamaPrefs.isChatHistoryEnabled) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Abilita persistenza del contesto", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "ATTENZIONE: Funzione instabile. Potrebbe migliorare la chat con GUFF ma potrebbe anche generare problemi. Abilitare solo per prova.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isChatHistoryEnabled,
                            onCheckedChange = {
                                isChatHistoryEnabled = it
                                llamaPrefs.isChatHistoryEnabled = it
                            }
                        )
                    }


                    Text("Token Massimi (nLen)", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Numero massimo di token (parole/simboli) che il modello GGUF può generare in una singola risposta. Valori alti permettono risposte più lunghe ma consumano più memoria e tempo. Impatto su CPU/Memoria: Medio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = nLen_llama,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                nLen_llama = newValue
                                newValue.toIntOrNull()?.let { llamaPrefs.nLen = it }
                            }
                        },
                        label = { Text("Max. Token Generabili") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = isGgufEnabled
                    )

                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))

                    Text("Temperatura (Creatività)", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Valori più alti (es. 0.9) rendono le risposte più creative, valori bassi (es. 0.2) le rendono più coerenti. Impatto su CPU/Memoria: Nullo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = temperature_llama,
                        onValueChange = { temperature_llama = it },
                        onValueChangeFinished = { llamaPrefs.temperature = temperature_llama },
                        valueRange = 0.0f..1.0f
                    )
                    Text(
                        String.format("%.2f", temperature_llama),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )

                    Spacer(Modifier.height(16.dp))

                    Text("Top-P (Campionamento Nucleo)", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Un valore alto (es. 0.95) considera più parole, uno basso è più restrittivo. Impatto su CPU/Memoria: Basso.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = topP_llama,
                        onValueChange = { topP_llama = it },
                        onValueChangeFinished = { llamaPrefs.topP = topP_llama },
                        valueRange = 0.0f..1.0f
                    )
                    Text(
                        String.format("%.2f", topP_llama),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Repeat-p (Penalita di ripetizione)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Il parametro Repeat-P (Repeat Penalty) riduce la tendenza del modello a ripetere frasi o parole, con un range efficace tra 1.0 (nessuna penalità) e 2.0 (forte penalità) su CPU/Memoria : impatto trascurabile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = repeatP_llama,
                        onValueChange = { repeatP_llama = it },
                        onValueChangeFinished = { llamaPrefs.repeatP = repeatP_llama },
                        valueRange = 0.0f..2.0f
                    )
                    Text(
                        String.format("%.2f", repeatP_llama),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Top-K (Campionamento Vocabolario)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Considera solo le K parole più probabili. Un valore alto (es. 50) offre più varietà, uno basso (es. 10) è più sicuro. Impatto su CPU/Memoria: Basso.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = topK_llama,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                topK_llama = newValue
                                newValue.toIntOrNull()?.let { llamaPrefs.topK = it }
                            }
                        },
                        label = { Text("Valore di Top-K") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = isGgufEnabled
                    )
                }
            }
        }
    }
}

// SceneJsonPicker rimane invariato e può essere in un file separato o qui.
@Composable
fun SceneJsonPicker(onFileSelected: (Uri) -> Unit) { // Modificato per ricevere Uri
    val context = LocalContext.current
    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            onFileSelected(it) // Passa l'URI al chiamante
        }
    }

    OutlinedButton(
        onClick = { pickFileLauncher.launch("application/json") },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Carica File JSON Scene")
    }
}