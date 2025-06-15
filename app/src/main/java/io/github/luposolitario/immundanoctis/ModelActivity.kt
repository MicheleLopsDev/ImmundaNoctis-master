package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.*
import io.github.luposolitario.immundanoctis.view.MainViewModel
import io.github.luposolitario.immundanoctis.worker.DownloadWorker
import java.io.File

class ModelActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val modelPreferences by lazy { ModelPreferences(applicationContext) }
    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    private val ttsPreferences by lazy { TtsPreferences(applicationContext) }
    private val workManager by lazy { WorkManager.getInstance(applicationContext) }
    private lateinit var enginePreferences: EnginePreferences
    private lateinit var gameStateManager: GameStateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enginePreferences = EnginePreferences(applicationContext)
        gameStateManager = GameStateManager(applicationContext)
        val dmDirectory = getDownloadDirectory("dm")
        val plDirectory = getDownloadDirectory("pl")

        val dmModelDefault = Downloadable("gemma-2b-it-quant.bin", Uri.parse(""), File(dmDirectory, "gemma-2b-it-quant.bin"))
        val playerModelDefault = Downloadable("llama-3-8b-instruct.Q4_K_M.gguf", Uri.parse(""), File(plDirectory, "llama-3-8b-instruct.Q4_K_M.gguf"))

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
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                        ttsPrefs = ttsPreferences,
                        onDeleteSession = {
                            if (gameStateManager.deleteSession()) {
                                Toast.makeText(this, "Sessione cancellata con successo.", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, MainActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, "Errore durante la cancellazione della sessione.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun getDownloadDirectory(subfolder: String): File {
        val appSpecificDir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val immundaDir = File(appSpecificDir, subfolder)
        if (!immundaDir.exists()) {
            immundaDir.mkdirs()
        }
        return immundaDir
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
    dmDirectory: File,
    plDirectory: File,
    themePrefs: ThemePreferences,
    enginePreferences: EnginePreferences,
    ttsPrefs: TtsPreferences,
    onDeleteSession: () -> Unit
) {
    var dmModelState by remember { mutableStateOf(initialDmModel) }
    var playerModelState by remember { mutableStateOf(initialPlayerModel) }
    var showUrlDialogFor by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var hfToken by remember { mutableStateOf(themePrefs.getToken() ?: "") }
    var selectedEngine by remember {
        mutableStateOf(if (enginePreferences.useGemmaForAll) EngineOption.GEMMA_ONLY else EngineOption.MIXED)
    }
    var autoReadEnabled by remember { mutableStateOf(ttsPrefs.isAutoReadEnabled()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showUrlDialogFor != null) {
        AddUrlDialog(
            onDismiss = { showUrlDialogFor = null },
            onConfirm = { url ->
                val directory = if (showUrlDialogFor == "DM") dmDirectory else plDirectory
                val currentModel = if (showUrlDialogFor == "DM") dmModelState else playerModelState
                currentModel.destination.delete()
                val newUri = Uri.parse(url)
                val fileName = newUri.lastPathSegment?.substringBefore('?') ?: "downloaded_model"
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

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = "Attenzione") },
            title = { Text("Conferma Cancellazione") },
            text = { Text("Stai per cancellare permanentemente la sessione di gioco salvata. L'operazione non è reversibile. Vuoi continuare?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteSession()
                    }
                ) {
                    Text("CANCELLA")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)) {

        // --- SEZIONE IMPOSTAZIONI TTS ---
        Text("Impostazioni Audio", style = MaterialTheme.typography.titleLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Lettura Automatica Messaggi", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Leggi automaticamente i messaggi di DM e PNG.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = autoReadEnabled,
                onCheckedChange = {
                    autoReadEnabled = it
                    ttsPrefs.saveAutoRead(it)
                }
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
        Divider()
        Spacer(Modifier.height(16.dp))

        val isGgufEnabled = selectedEngine == EngineOption.MIXED

        ModelSlotView(
            title = "Motore del Dungeon Master (Gemma)",
            subtitle = "Modello per narrazione e ambiente.Consigliato: Gemma",
            model = dmModelState,
            token = hfToken,
            viewModel = viewModel,
            workManager = workManager,
            onSetUrlClick = { showUrlDialogFor = "DM" },
            onDownloadComplete = { modelPrefs.saveDmModel(it) },
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

        Column(modifier = Modifier.alpha(if (isGgufEnabled) 1f else 0.5f)) {
            ModelSlotView(
                title = "Motore dei Personaggi (GGUF)",
                subtitle = "Modello per le risposte dei PG. Disabilitato in modalità 'Solo Gemma'.",
                model = playerModelState,
                token = hfToken,
                viewModel = viewModel,
                workManager = workManager,
                onSetUrlClick = { if (isGgufEnabled) showUrlDialogFor = "PLAYER" },
                onDeleteClick = {
                    workManager.cancelAllWorkByTag(playerModelState.name)
                    playerModelState.destination.delete()
                    modelPrefs.clearPlayerModel()
                    (context as? Activity)?.recreate()
                },
                onDownloadComplete = { modelPrefs.savePlayerModel(it) },
                enabled = isGgufEnabled
            )
        }

        // --- SEZIONE: Operazioni di Emergenza (CORRETTA) ---
        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))
        Text("Operazioni di Emergenza", style = MaterialTheme.typography.titleLarge)
        Text(
            "Usa queste opzioni solo se l'app non funziona correttamente.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedButton(
            onClick = { showDeleteConfirmDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error) // Corretto
        ) {
            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Cancella Sessione di Gioco")
        }


        Spacer(Modifier.weight(1f, fill = false))
        Spacer(Modifier.height(24.dp))
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

@Composable
fun ModelSlotView(
    title: String,
    subtitle: String,
    model: Downloadable,
    viewModel: MainViewModel,
    workManager: WorkManager,
    onSetUrlClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDownloadComplete: (Downloadable) -> Unit,
    token: String,
    enabled: Boolean
) {
    val workInfoList by workManager.getWorkInfosByTagLiveData(model.name).observeAsState()
    val runningWork = workInfoList?.find { !it.state.isFinished }

    val status by remember(runningWork, model.destination.exists()) {
        derivedStateOf {
            when (runningWork?.state) {
                WorkInfo.State.RUNNING -> {
                    val bytesDownloaded = runningWork.progress.getLong(DownloadWorker.KEY_BYTES_DOWNLOADED, 0L)
                    val totalBytes = runningWork.progress.getLong(DownloadWorker.KEY_TOTAL_BYTES, 0L)
                    Downloadable.Companion.State.Downloading(bytesDownloaded, totalBytes)
                }
                WorkInfo.State.SUCCEEDED -> Downloadable.Companion.State.Downloaded(model)
                WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> Downloadable.Companion.State.Error("Download fallito")
                else -> if (model.destination.exists()) Downloadable.Companion.State.Downloaded(model) else Downloadable.Companion.State.Ready
            }
        }
    }

    LaunchedEffect(status) {
        if (status is Downloadable.Companion.State.Downloaded) {
            onDownloadComplete(model)
        }
    }

    val onClick: () -> Unit = {
        if (status is Downloadable.Companion.State.Downloading) {
            runningWork?.id?.let {
                workManager.cancelWorkById(it)
                viewModel.log("Cancellazione download per ${model.name}")
            }
        } else {
            model.destination.delete()
            val downloadData = workDataOf(
                DownloadWorker.KEY_URL to model.source.toString(),
                DownloadWorker.KEY_DESTINATION to model.destination.absolutePath,
                DownloadWorker.KEY_MODEL_NAME to model.destination.name,
                DownloadWorker.KEY_MODEL_DOWNLOAD_ACCESS_TOKEN to token
            )
            val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(downloadData)
                .addTag(model.name)
                .build()
            workManager.enqueueUniqueWork(model.name, ExistingWorkPolicy.REPLACE, downloadWorkRequest)
            viewModel.log("Download per ${model.name} messo in coda.")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Downloadable.Button(status = status, item = model, onClick = onClick, enabled = enabled)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row {
                if (status is Downloadable.Companion.State.Downloading) {
                    IconButton(onClick = onClick, enabled = enabled) {
                        Icon(Icons.Default.Cancel, "Cancella Download")
                    }
                } else {
                    IconButton(onClick = onSetUrlClick, enabled = enabled) {
                        Icon(Icons.Default.AddLink, "Imposta URL Modello")
                    }
                }
                if (status is Downloadable.Companion.State.Downloaded) {
                    IconButton(onClick = onDeleteClick, enabled = enabled) {
                        Icon(Icons.Default.Delete, "Cancella Modello", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun TokenInputSection(
    token: String,
    onTokenChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Hugging Face Access Token", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = token,
                onValueChange = onTokenChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Il tuo token con permessi 'read'") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    val description = if (passwordVisible) "Nascondi token" else "Mostra token"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onSaveClick, modifier = Modifier.align(Alignment.End)) {
                Text("Salva Token")
            }
        }
    }
}

@Composable
fun AddUrlDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val urlText = remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Imposta URL del Modello") }, text = {
        OutlinedTextField(value = urlText.value, onValueChange = { urlText.value = it }, label = { Text("URL diretto del modello") }, placeholder = { Text("https://huggingface.co/...") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }, confirmButton = {
        TextButton(onClick = { if (urlText.value.isNotBlank()) onConfirm(urlText.value) }) {
            Text("Conferma")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) { Text("Annulla") }
    })
}

fun android.content.ContentResolver.getFileName(uri: Uri): String? {
    var name: String? = null
    val cursor: Cursor? = query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
    }
    return name
}

@Preview(showBackground = true, name = "Gestione Motori (Chiaro)")
@Composable
private fun MainEngineScreenPreview() {
    ImmundaNoctisTheme(darkTheme = false) {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ModelSlotViewPreview(
                    title = "Motore del Dungeon Master",
                    subtitle = "Modello Gemma (caricato localmente)",
                    modelName = "gemma-2b-it-Q4_K_M.gguf",
                    isDownloaded = true
                )
                Divider()
                ModelSlotViewPreview(
                    title = "Motore dei Personaggi",
                    subtitle = "Consigliato: Llama/Mistral (formato GGUF)",
                    modelName = "Llama-3.1-8B-Q6_K.gguf",
                    isDownloaded = false
                )
            }
        }
    }
}

@Composable
private fun ModelSlotViewPreview(
    title: String,
    subtitle: String,
    modelName: String,
    isDownloaded: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = {}, enabled = !isDownloaded) {
                Text(if (isDownloaded) "Load $modelName" else "Download $modelName")
            }
            Row {
                IconButton(onClick = {}) { Icon(Icons.Default.AddLink, contentDescription = "URL") }
                if (isDownloaded) {
                    IconButton(onClick = {}) { Icon(Icons.Default.Delete, "Cancella", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
fun EngineRadioButton(
    text: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
