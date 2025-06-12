package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.app.DownloadManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.Downloadable
import io.github.luposolitario.immundanoctis.util.ModelPreferences
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import io.github.luposolitario.immundanoctis.view.MainViewModel
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ModelActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val modelPreferences by lazy { ModelPreferences(applicationContext) }
    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    private val downloadManager by lazy { getSystemService<DownloadManager>()!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dmDirectory = getDownloadDirectory("dm")
        val plDirectory = getDownloadDirectory("pl")

        val dmModelDefault = Downloadable(
            name = "gemma-3n-E4B-it-int4.task",
            source = Uri.parse("https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task?download=true"),
            destination = File(dmDirectory, "gemma-3n-E4B-it-int4.task")
        )
        val playerModelDefault = Downloadable(
            name = "Llama-3.1-8B-Q6_K.gguf",
            source = Uri.parse("https://huggingface.co/jott1970/Llama-3.1-8B-Instruct-Fei-v1-Uncensored-Q6_K-GGUF/resolve/main/llama-3.1-8b-instruct-fei-v1-uncensored-q6_k.gguf?download=true"),
            destination = File(plDirectory, "llama-3.1-8b-instruct-q6_k.gguf")
        )

        val dmModel = modelPreferences.getDmModel() ?: dmModelDefault
        val playerModel = modelPreferences.getPlayerModel() ?: playerModelDefault

        setContent {
            val useDarkTheme = themePreferences.useDarkTheme(isSystemInDarkTheme())
            ImmundaNoctisTheme(darkTheme = useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainEngineScreen(
                        viewModel = viewModel,
                        dm = downloadManager,
                        modelPrefs = modelPreferences,
                        initialDmModel = dmModel,
                        initialPlayerModel = playerModel,
                        dmDirectory = dmDirectory,
                        plDirectory = plDirectory
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

// NOTA: La versione duplicata di MainEngineScreen è stata rimossa. È rimasta solo questa.
@Composable
fun MainEngineScreen(
    viewModel: MainViewModel,
    dm: DownloadManager,
    modelPrefs: ModelPreferences,
    initialDmModel: Downloadable,
    initialPlayerModel: Downloadable,
    dmDirectory: File,
    plDirectory: File
) {
    var dmModelState by remember { mutableStateOf(initialDmModel) }
    var playerModelState by remember { mutableStateOf(initialPlayerModel) }
    var showUrlDialogFor by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    if (showUrlDialogFor != null) {
        val isDm = showUrlDialogFor == "DM"
        AddUrlDialog(
            onDismiss = { showUrlDialogFor = null },
            onConfirm = { url ->
                val directory = if (isDm) dmDirectory else plDirectory
                val currentModel = if (isDm) dmModelState else playerModelState
                currentModel.destination.delete()
                val newUri = Uri.parse(url)
                val fileName = newUri.lastPathSegment?.substringBefore('?') ?: "downloaded_model"
                val newModel = Downloadable(fileName, newUri, File(directory, fileName))
                if (isDm) {
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ModelSlotView(
            title = "Motore del Dungeon Master",
            subtitle = "Consigliato: Gemma (formato .task)",
            model = dmModelState,
            viewModel = viewModel,
            dm = dm,
            onSetUrlClick = { showUrlDialogFor = "DM" },
            onDownloadComplete = { downloadedModel ->
                viewModel.log("Modello DM scaricato. Salvo preferenza.")
                modelPrefs.saveDmModel(downloadedModel)
            },
            onDeleteClick = {
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
            viewModel = viewModel,
            dm = dm,
            onSetUrlClick = { showUrlDialogFor = "PLAYER" },
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
    viewModel: MainViewModel,
    dm: DownloadManager,
    onSetUrlClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDownloadComplete: (Downloadable) -> Unit
) {
    var status by remember(model) {
        mutableStateOf(
            if (model.destination.exists()) Downloadable.Companion.State.Downloaded(model)
            else Downloadable.Companion.State.Ready
        )
    }
    val coroutineScope = rememberCoroutineScope()

    val onClick: () -> Unit = {
        when (status) {
            is Downloadable.Companion.State.Downloaded -> {
                viewModel.log("Modello '${model.name}' pronto per l'uso in AdventureActivity.")
            }
            else -> { // Ready o Error
                model.destination.delete()
                val request = DownloadManager.Request(model.source).apply {
                    setTitle("Downloading model: ${model.name}")
                    setDestinationUri(model.destination.toUri())
                }
                val id = dm.enqueue(request)
                viewModel.log("Avvio download per ${model.name}")

                coroutineScope.launch {
                    var progress = 0.0
                    status = Downloadable.Companion.State.Downloading(progress)

                    while (status is Downloadable.Companion.State.Downloading) {
                        val cursor = dm.query(DownloadManager.Query().setFilterById(id))
                        if (cursor != null && cursor.moveToFirst()) {
                            val sofar = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            cursor.close()
                            if (total > 0) {
                                progress = sofar.toDouble() / total
                                status = Downloadable.Companion.State.Downloading(progress)
                            }
                            if (sofar > 0 && sofar == total) {
                                status = Downloadable.Companion.State.Downloaded(model)
                                onDownloadComplete(model)
                                break
                            }
                        }
                        delay(1000)
                    }
                }
            }
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
            Downloadable.Button(
                status = status,
                item = model,
                onClick = onClick
            )
            Row {
                IconButton(onClick = onSetUrlClick) {
                    Icon(imageVector = Icons.Default.AddLink, contentDescription = "Imposta URL Modello")
                }
                if (status is Downloadable.Companion.State.Downloaded) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Cancella Modello", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

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

// ... Le Preview rimangono invariate ...
