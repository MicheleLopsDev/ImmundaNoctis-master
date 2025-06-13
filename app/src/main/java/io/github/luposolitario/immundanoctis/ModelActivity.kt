package io.github.luposolitario.immundanoctis

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.*
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.Downloadable
import io.github.luposolitario.immundanoctis.util.ModelPreferences
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import io.github.luposolitario.immundanoctis.view.MainViewModel
import io.github.luposolitario.immundanoctis.worker.DownloadWorker
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

class ModelActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val modelPreferences by lazy { ModelPreferences(applicationContext) }
    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    private val workManager by lazy { WorkManager.getInstance(applicationContext) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dmDirectory = getDownloadDirectory("dm")
        val plDirectory = getDownloadDirectory("pl")

        val dmModelDefault = Downloadable("gemma-3n-E4B-it-int4.task", Uri.parse("https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task?download=true"), File(dmDirectory, "gemma-3n-E4B-it-int4.task"))
        val playerModelDefault = Downloadable("Llama-3.1-8B-Q6_K.gguf", Uri.parse("https://huggingface.co/jott1970/Llama-3.1-8B-Instruct-Fei-v1-Uncensored-Q6_K-GGUF/resolve/main/llama-3.1-8b-instruct-fei-v1-uncensored-q6_k.gguf?download=true"), File(plDirectory, "llama-3.1-8b-instruct-q6_k.gguf"))

        val dmModel = modelPreferences.getDmModel() ?: dmModelDefault
        val playerModel = modelPreferences.getPlayerModel() ?: playerModelDefault

        setContent {
            val useDarkTheme = themePreferences.useDarkTheme(isSystemInDarkTheme())
            ImmundaNoctisTheme(darkTheme = useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainEngineScreen(viewModel, workManager, modelPreferences, dmModel, playerModel, dmDirectory, plDirectory,themePreferences)
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

@Composable
fun MainEngineScreen(viewModel: MainViewModel, workManager: WorkManager, modelPrefs: ModelPreferences, initialDmModel: Downloadable, initialPlayerModel: Downloadable, dmDirectory: File, plDirectory: File, themePrefs:ThemePreferences,) {
    var dmModelState by remember { mutableStateOf(initialDmModel) }
    var playerModelState by remember { mutableStateOf(initialPlayerModel) }
    var showUrlDialogFor by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var slotToImport by remember { mutableStateOf<String?>(null) }
    var hfToken by remember { mutableStateOf(themePrefs.getToken() ?: "") }

    val filePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null || slotToImport == null) return@rememberLauncherForActivityResult
        val isDm = slotToImport == "DM"
        val directory = if (isDm) dmDirectory else plDirectory
        val currentModel = if (isDm) dmModelState else playerModelState
        val contentResolver = context.contentResolver
        val fileName = contentResolver.getFileName(uri) ?: "imported_model"
        val destinationFile = File(directory, fileName)
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            viewModel.log("File importato: $fileName")
            currentModel.destination.delete()
            val newModel = Downloadable(fileName, uri, destinationFile)
            if (isDm) {
                modelPrefs.saveDmModel(newModel)
                dmModelState = newModel
            } else {
                modelPrefs.savePlayerModel(newModel)
                playerModelState = newModel
            }
        } catch (e: Exception) {
            viewModel.log("Errore importazione: ${e.message}")
        } finally {
            slotToImport = null
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            filePickerLauncher.launch("*/*")
        } else {
            viewModel.log("Permesso negato.")
        }
    }
    val launchFilePicker = { slot: String ->
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            PackageManager.PERMISSION_GRANTED -> {
                slotToImport = slot
                filePickerLauncher.launch("*/*")
            }
            else -> {
                slotToImport = slot
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        ModelSlotView(
            title = "Motore del Dungeon Master",
            subtitle = "Consigliato: Gemma (formato TASK)",
            model = dmModelState,
            token = hfToken, // Passiamo il token
            isReadOnly = false, // Ora entrambi sono interattivi
            viewModel = viewModel,
            workManager = workManager,
            onSetUrlClick = { showUrlDialogFor = "DM" },
            onImportFileClick = { launchFilePicker("DM") },
            onDownloadComplete = { modelPrefs.saveDmModel(it) },
            onDeleteClick = {
                workManager.cancelAllWorkByTag(dmModelState.name)
                dmModelState.destination.delete()
                modelPrefs.clearDmModel()
                (context as? Activity)?.recreate()
            }
        )
        Divider()
        ModelSlotView(
            title = "Motore dei Personaggi",
            subtitle = "Consigliato: Llama/Mistral (formato GGUF)",
            model = playerModelState,
            token = hfToken, // Passiamo il token
            isReadOnly = false,
            viewModel = viewModel,
            workManager = workManager,
            onSetUrlClick = { showUrlDialogFor = "PLAYER" },
            onImportFileClick = { launchFilePicker("PLAYER") },
            onDeleteClick = {
                workManager.cancelAllWorkByTag(playerModelState.name)
                playerModelState.destination.delete()
                modelPrefs.clearPlayerModel()
                (context as? Activity)?.recreate()
            },
            onDownloadComplete = { modelPrefs.savePlayerModel(it) },
        )
        // Sezione per l'input del token in fondo
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
    isReadOnly: Boolean,
    viewModel: MainViewModel,
    workManager: WorkManager,
    onSetUrlClick: () -> Unit,
    onImportFileClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDownloadComplete: (Downloadable) -> Unit,
    token: String
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

        // MODIFICA: Abbiamo rimosso 'horizontalArrangement = Arrangement.SpaceBetween'
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isReadOnly) {
                val fileExists = model.destination.exists()
                val icon = if (fileExists) Icons.Default.CheckCircle else Icons.Default.Error
                val text = if (fileExists) "Modello Locale Trovato" else "File non Trovato"
                val color = if (fileExists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onImportFileClick) {
                        Icon(Icons.Default.FileOpen, "Importa File Locale")
                    }
                    Icon(imageVector = icon, contentDescription = text, tint = color)
                    Spacer(Modifier.width(8.dp))
                    Text(text = text, color = color)
                }
            } else {
                // MODIFICA: Aggiunto Modifier.weight(1f) al pulsante (o al suo contenitore).
                // Dato che non conosco la struttura di Downloadable.Button, lo avvolgo in un Box
                // per applicare il peso e garantire che il testo all'interno si tronchi.
                // Idealmente, il `weight` va applicato direttamente al componente Button/Text.
                Box(modifier = Modifier.weight(1f)) {
                    Downloadable.Button(status = status, item = model, onClick = onClick)
                }

                Spacer(modifier = Modifier.width(8.dp)) // Aggiungiamo uno spazio tra il pulsante e le icone

                Row {
                    if (status is Downloadable.Companion.State.Downloading) {
                        IconButton(onClick = onClick) {
                            Icon(Icons.Default.Cancel, "Cancella Download")
                        }
                    } else {
                        IconButton(onClick = onImportFileClick) {
                            Icon(Icons.Default.FileOpen, "Importa File Locale")
                        }
                        IconButton(onClick = onSetUrlClick) {
                            Icon(Icons.Default.AddLink, "Imposta URL Modello")
                        }
                    }
                    // Ora questa icona sarà sempre visibile!
                    if (status is Downloadable.Companion.State.Downloaded) {
                        IconButton(onClick = onDeleteClick) {
                            Icon(Icons.Default.Delete, "Cancella Modello", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// NUOVO COMPOSABLE: Aggiungi questa funzione in fondo al file ModelActivity.kt
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

// --- FUNZIONE HELPER RI-AGGIUNTA ---
/**
 * Funzione di estensione per ottenere il nome del file da un Uri.
 * Necessaria per il selettore di file.
 */
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
                    isDownloaded = true,
                    isReadOnly = true
                )
                Divider()
                ModelSlotViewPreview(
                    title = "Motore dei Personaggi",
                    subtitle = "Consigliato: Llama/Mistral (formato GGUF)",
                    modelName = "Llama-3.1-8B-Q6_K.gguf",
                    isDownloaded = false,
                    isReadOnly = false
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
    isDownloaded: Boolean,
    isReadOnly: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isReadOnly) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Modello Locale Trovato", color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Button(onClick = {}, enabled = !isDownloaded) {
                    Text(if (isDownloaded) "Load $modelName" else "Download $modelName")
                }
            }

            if (!isReadOnly) {
                Row {
                    IconButton(onClick = {}) { Icon(Icons.Default.FileOpen, contentDescription = "Importa") }
                    IconButton(onClick = {}) { Icon(Icons.Default.AddLink, contentDescription = "URL") }
                    if (isDownloaded) {
                        IconButton(onClick = {}) { Icon(Icons.Default.Delete, "Cancella", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}
