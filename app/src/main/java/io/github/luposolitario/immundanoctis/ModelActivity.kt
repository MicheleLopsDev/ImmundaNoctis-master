package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.work.*
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.Downloadable
import io.github.luposolitario.immundanoctis.util.ModelPreferences
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import io.github.luposolitario.immundanoctis.view.MainViewModel
import io.github.luposolitario.immundanoctis.worker.DownloadWorker
import java.io.File
import java.util.UUID

class ModelActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val modelPreferences by lazy { ModelPreferences(applicationContext) }
    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    private val workManager by lazy { WorkManager.getInstance(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dmDirectory = getDownloadDirectory("dm")
        val plDirectory = getDownloadDirectory("pl")

        val dmModelDefault = Downloadable("gemma-2b-it-Q4_K_M.gguf", Uri.parse("https://huggingface.co/jankrepl/gemma-2b-it-GGUF/resolve/main/gemma-2b-it.Q4_K_M.gguf?download=true"), File(dmDirectory, "gemma-2b-it-Q4_K_M.gguf"))
        val playerModelDefault = Downloadable("Llama-3.1-8B-Q6_K.gguf", Uri.parse("https://huggingface.co/jott1970/Llama-3.1-8B-Instruct-Fei-v1-Uncensored-Q6_K-GGUF/resolve/main/llama-3.1-8b-instruct-fei-v1-uncensored-q6_k.gguf?download=true"), File(plDirectory, "llama-3.1-8b-instruct-q6_k.gguf"))

        val dmModel = modelPreferences.getDmModel() ?: dmModelDefault
        val playerModel = modelPreferences.getPlayerModel() ?: playerModelDefault

        setContent {
            val useDarkTheme = themePreferences.useDarkTheme(isSystemInDarkTheme())
            ImmundaNoctisTheme(darkTheme = useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainEngineScreen(viewModel, workManager, modelPreferences, dmModel, playerModel, dmDirectory, plDirectory)
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
fun MainEngineScreen(viewModel: MainViewModel, workManager: WorkManager, modelPrefs: ModelPreferences, initialDmModel: Downloadable, initialPlayerModel: Downloadable, dmDirectory: File, plDirectory: File) {
    var dmModelState by remember { mutableStateOf(initialDmModel) }
    var playerModelState by remember { mutableStateOf(initialPlayerModel) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        ModelSlotView(
            title = "Motore del Dungeon Master",
            subtitle = "Consigliato: Gemma (formato GGUF)",
            model = dmModelState,
            isReadOnly = false, // Ora entrambi sono interattivi
            viewModel = viewModel,
            workManager = workManager,
            onDownloadComplete = { downloadedModel ->
                viewModel.log("Modello DM scaricato. Salvo preferenza.")
                modelPrefs.saveDmModel(downloadedModel)
            },
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
            isReadOnly = false,
            viewModel = viewModel,
            workManager = workManager,
            onDownloadComplete = { downloadedModel ->
                viewModel.log("Modello PG scaricato. Salvo preferenza.")
                modelPrefs.savePlayerModel(downloadedModel)
            },
            onDeleteClick = {
                playerModelState.destination.delete()
                modelPrefs.clearPlayerModel()
                (context as? Activity)?.recreate()
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
    onDeleteClick: () -> Unit,
    onDownloadComplete: (Downloadable) -> Unit
) {
    val workInfo by workManager.getWorkInfosByTagLiveData(model.name).observeAsState()
    val runningWork = workInfo?.find { !it.state.isFinished }

    // Trova questa sezione nel tuo ModelActivity.kt (intorno alla riga 137)
    val status by remember(runningWork, model.destination.exists()) {
        derivedStateOf {
            when (runningWork?.state) {
                WorkInfo.State.RUNNING -> {
                    val progress = runningWork.progress.getInt(DownloadWorker.KEY_PROGRESS, 0)
                    // CORREZIONE: Aggiungi anche il parametro totalBytes
                    val totalBytes = runningWork.progress.getLong(DownloadWorker.KEY_TOTAL_BYTES, 0L)
                    val bytesDownloaded = runningWork.progress.getLong(DownloadWorker.KEY_BYTES_DOWNLOADED, 0L)

                    // Se hai solo il progresso in percentuale, puoi fare così:
                    // Downloadable.Companion.State.Downloading(progress.toLong(), 100L)

                    // Oppure se hai i bytes effettivi:
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
                DownloadWorker.KEY_DESTINATION to model.destination.absolutePath
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Downloadable.Button(status = status, item = model, onClick = onClick)
            Row {
                if (status is Downloadable.Companion.State.Downloaded) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, "Cancella Modello", tint = MaterialTheme.colorScheme.error)
                    }
                } else if (status is Downloadable.Companion.State.Downloading) {
                    IconButton(onClick = onClick) {
                        Icon(Icons.Default.Cancel, contentDescription = "Cancella Download")
                    }
                }
            }
        }
    }
}
// --- FUNZIONE RI-AGGIUNTA ---
@Composable
fun AddUrlDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val urlText = remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Imposta URL del Modello") },
        text = {
            OutlinedTextField(
                value = urlText.value,
                onValueChange = { urlText.value = it },
                label = { Text("URL diretto del modello") },
                placeholder = { Text("https://huggingface.co/...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (urlText.value.isNotBlank()) onConfirm(urlText.value) }) {
                Text("Conferma")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
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
    // ...
}

@Preview(showBackground = true, name = "Gestione Motori (Scuro)", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MainEngineScreenPreviewDark() {
    // ...
}

@Composable
private fun ModelSlotViewPreview(
    title: String,
    subtitle: String,
    modelName: String,
    isDownloaded: Boolean
) {
    // ...
}

@Preview(showBackground = true)
@Composable
private fun AddUrlDialogPreview() {
    ImmundaNoctisTheme {
        AddUrlDialog(onDismiss = {}, onConfirm = {})
    }
}
