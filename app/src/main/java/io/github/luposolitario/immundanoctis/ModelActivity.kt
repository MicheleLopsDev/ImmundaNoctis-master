package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.app.ActivityManager
import android.app.DownloadManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.Downloadable
import io.github.luposolitario.immundanoctis.view.MainViewModel
import java.io.File

class ModelActivity(
    activityManager: ActivityManager? = null,
    downloadManager: DownloadManager? = null,
): ComponentActivity() {
    private val tag: String? = this::class.simpleName

    private val activityManager by lazy { activityManager ?: getSystemService<ActivityManager>()!! }
    private val downloadManager by lazy { downloadManager ?: getSystemService<DownloadManager>()!! }

    private val viewModel: MainViewModel by viewModels()

    private fun availableMemory(): ActivityManager.MemoryInfo {
        return ActivityManager.MemoryInfo().also { memoryInfo ->
            activityManager.getMemoryInfo(memoryInfo)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StrictMode.setVmPolicy(
            VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )

        val free = Formatter.formatFileSize(this, availableMemory().availMem)
        val total = Formatter.formatFileSize(this, availableMemory().totalMem)

        viewModel.log("Current memory: $free / $total")
        viewModel.log("Downloads directory: ${getDownloadDirectory()}")

        val extFilesDir = getDownloadDirectory()

        val models = listOf(
            Downloadable(
                "Llama 3.1 8B Uncensored (Q6_K)",
                Uri.parse("https://huggingface.co/jott1970/Llama-3.1-8B-Instruct-Fei-v1-Uncensored-Q6_K-GGUF/resolve/main/llama-3.1-8b-instruct-fei-v1-uncensored-q6_k.gguf?download=true"),
                File(extFilesDir, "llama-3.1-8b-instruct-q6_k.gguf")
            ),
        )

        setContent {
            ImmundaNoctisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainCompose(
                        viewModel,
                        downloadManager,
                        models,
                    )
                }
            }
        }
    }

    private fun getDownloadDirectory(): File {
        val appSpecificDir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val immundaDir = File(appSpecificDir, "immunda")

        if (!immundaDir.exists()) {
            immundaDir.mkdirs()
        }
        return immundaDir
    }
}

@Composable
fun MainCompose(
    viewModel: MainViewModel,
    dm: DownloadManager,
    models: List<Downloadable>
) {
    val showUrlDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Variabile "trigger" per forzare la ricomposizione della UI
    var recompositionTrigger by remember { mutableStateOf(0) }

    if (showUrlDialog.value) {
        AddUrlDialog(
            onDismiss = { showUrlDialog.value = false },
            onConfirm = { url ->
                viewModel.log("Nuovo URL aggiunto: $url")
                showUrlDialog.value = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- SEZIONE LOG MESSAGES ---
        Box(modifier = Modifier.weight(1f)) {
            val scrollState = rememberLazyListState()
            LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
                items(viewModel.messages) {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // --- SEZIONE DOWNLOADS ---
        LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            items(models, key = { it.name + recompositionTrigger }) { model ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Passiamo la callback al pulsante per sapere quando il download è finito
                    Downloadable.Button(
                        viewModel = viewModel,
                        dm = dm,
                        item = model,
                        onDownloadComplete = {
                            // Quando il download finisce, incrementiamo il trigger
                            // per forzare l'aggiornamento della UI
                            recompositionTrigger++
                        }
                    )

                    // Il controllo viene eseguito di nuovo dopo la ricomposizione
                    if (model.destination.exists()) {
                        IconButton(onClick = {
                            if (model.destination.delete()) {
                                (context as? Activity)?.recreate()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Cancella modello",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // --- PULSANTE AGGIUNGI ---
        Button(
            onClick = { showUrlDialog.value = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Aggiungi Modello da URL")
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
        title = { Text("Aggiungi Nuovo Modello") },
        text = {
            OutlinedTextField(
                value = urlText.value,
                onValueChange = { urlText.value = it },
                label = { Text("URL del modello GGUF") },
                placeholder = { Text("https://huggingface.co/...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (urlText.value.isNotBlank()) {
                        onConfirm(urlText.value)
                    }
                }
            ) {
                Text("Aggiungi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Preview(showBackground = true, name = "Schermata Principale")
@Composable
fun MainComposePreview() {
    val messaggiDiEsempio = listOf(
        "Log: Avvio dell'applicazione...",
        "Memoria disponibile: 4 GB / 8 GB",
        "Directory di download: /path/to/downloads"
    )
    val modelliDiEsempio = listOf(
        Downloadable("Llama 3.1 8B (Q6_K)", Uri.parse(""), File("preview.gguf")),
        Downloadable("Mistral 7B (Q4)", Uri.parse(""), File("preview.gguf")),
        Downloadable("Phi 3 Mini (Q5_K)", Uri.parse(""), File("preview.gguf")),
        Downloadable("Gemma 2 9B (Q6_K)", Uri.parse(""), File("preview.gguf"))
    )

    ImmundaNoctisTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(messaggiDiEsempio) { messaggio ->
                            Text(messaggio, modifier = Modifier.padding(16.dp))
                        }
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                    items(modelliDiEsempio) { modello ->
                        Button(
                            onClick = { },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text("Download ${modello.name}")
                        }
                    }
                }

                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Aggiungi Modello da URL")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Popup Aggiungi URL")
@Composable
fun AddUrlDialogPreview() {
    ImmundaNoctisTheme {
        AddUrlDialog(
            onDismiss = {},
            onConfirm = {}
        )
    }
}
