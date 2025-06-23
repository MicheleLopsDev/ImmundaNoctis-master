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
import io.github.luposolitario.immundanoctis.util.getAppSpecificDirectory // <-- Assicurati che ci sia questo import
import io.github.luposolitario.immundanoctis.util.GemmaPreferences
import io.github.luposolitario.immundanoctis.util.LlamaPreferences
import androidx.compose.material3.Slider
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import io.github.luposolitario.immundanoctis.worker.DownloadWorker

class ModelActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val modelPreferences by lazy { ModelPreferences(applicationContext) }
    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    private val workManager by lazy { WorkManager.getInstance(applicationContext) }
    private lateinit var enginePreferences: EnginePreferences
    private val gemmaPreferences by lazy { GemmaPreferences(applicationContext) }
    private val llamaPreferences by lazy { LlamaPreferences(applicationContext) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enginePreferences = EnginePreferences(applicationContext)
        val dmDirectory = getAppSpecificDirectory(applicationContext, "dm")
        val plDirectory = getAppSpecificDirectory(applicationContext, "pl")

        val dmModelDefault = Downloadable(
            "gemma-3n-E4B-it-int4",
            Uri.parse("https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task?download=true"),
            File(dmDirectory, "gemma-3n-E4B-it-int4.task")
        )
        val playerModelDefault = Downloadable(
            "Roleplay-9B-lora-800-porn.i1-Q4_K_S",
            Uri.parse("https://huggingface.co/mradermacher/Roleplay-9B-lora-800-porn-i1-GGUF/resolve/main/Roleplay-9B-lora-800-porn.i1-Q4_K_S.gguf"),
            File(plDirectory, "Roleplay-9B-lora-800-porn.i1-Q4_K_S.gguf")
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
                    )
                }
            }
        }
    }

    private enum class EngineOption { MIXED, GEMMA_ONLY }

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
        gemmaPrefs: GemmaPreferences,
        llamaPrefs: LlamaPreferences,
    ) {
        var dmModelState by remember { mutableStateOf(initialDmModel) }
        var playerModelState by remember { mutableStateOf(initialPlayerModel) }
        var showUrlDialogFor by remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current
        var hfToken by remember { mutableStateOf(themePrefs.getToken() ?: "") }
        var selectedEngine by remember {
            mutableStateOf(if (enginePreferences.useGemmaForAll) EngineOption.GEMMA_ONLY else EngineOption.MIXED)
        }



        var nLen by remember { mutableStateOf(gemmaPrefs.nLen.toString()) }
        var temperature by remember { mutableStateOf(gemmaPrefs.temperature) }
        var topP by remember { mutableStateOf(gemmaPrefs.topP) }
        var topK by remember { mutableStateOf(gemmaPrefs.topK.toString()) }


        var nLen_llama by remember { mutableStateOf(llamaPrefs.nLen.toString()) }
        var temperature_llama by remember { mutableStateOf(gemmaPrefs.temperature) }
        var topP_llama by remember { mutableStateOf(llamaPrefs.topP) }
        var topK_llama by remember { mutableStateOf(llamaPrefs.topK.toString()) }
        var repeatP_llama by remember { mutableStateOf(llamaPrefs.repeatP) }



        // --- NUOVO: Gestore per la selezione di file ---
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

        val startDownload = { model: Downloadable ->
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
            modelPrefs.saveDmModel(model)
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
            Column() {
                // --- MODIFICA: Aggiunti i nuovi parametri a ModelSlotView ---
                ModelSlotView(
                    title = "Motore del Dungeon Master (Gemma)",
                    subtitle = "Modello per narrazione e ambiente.Consigliato: Gemma",
                    model = dmModelState,
                    workManager = workManager,
                    onSetUrlClick = { showUrlDialogFor = "DM" },
                    onDownloadClick = { startDownload(dmModelState) }, // <-- AGGIUNGI QUESTO
                    onSelectFileClick = {
                        viewModel.isPickingForDm = true // Flag per sapere per chi stiamo scegliendo
                        filePickerLauncher.launch("*/*") // Avvia il selettore di file
                    },
                    onDeleteClick = {
                        workManager.cancelAllWorkByTag(dmModelState.name)
                        dmModelState.destination.delete()
                        modelPrefs.clearDmModel()
                        (context as? Activity)?.recreate()
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
                // --- MODIFICA: Aggiunti i nuovi parametri a ModelSlotView ---
                ModelSlotView(
                    title = "Motore dei Personaggi (GGUF)",
                    subtitle = "Modello per le risposte dei PG. Disabilitato in modalità 'Solo Gemma'.",
                    model = playerModelState,
                    workManager = workManager,
                    onSetUrlClick = { if (isGgufEnabled) showUrlDialogFor = "PLAYER" },
                    onDownloadClick = { startDownload(playerModelState) }, // <-- AGGIUNGI QUESTO
                    onSelectFileClick = {
                        if (isGgufEnabled) {
                            viewModel.isPickingForDm =
                                false // Flag per sapere per chi stiamo scegliendo
                            filePickerLauncher.launch("*/*") // Avvia il selettore di file
                        }
                    },
                    onDeleteClick = {
                        workManager.cancelAllWorkByTag(playerModelState.name)
                        playerModelState.destination.delete()
                        modelPrefs.clearPlayerModel()
                        (context as? Activity)?.recreate()
                    },
                    enabled = isGgufEnabled
                )
                Text("Impostazioni Avanzate GUFF ", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
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

                Text("Repeat-p (Penalita di ripetizione)", style = MaterialTheme.typography.bodyLarge)
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

            Spacer(Modifier.weight(1f, fill = false))
            TokenInputSection(
                token = hfToken,
                onTokenChange = { hfToken = it },
                onSaveClick = {
                    themePrefs.saveToken(hfToken)
                    viewModel.log("Token Hugging Face salvato.")
                }
            )
        }
    }
}
