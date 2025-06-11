package io.github.luposolitario.immundanoctis

import android.app.ActivityManager
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
        viewModel.log("Downloads directory: ${getExternalFilesDir(null)}")

        val extFilesDir = getExternalFilesDir(null)

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
}

@Composable
fun MainCompose(
    viewModel: MainViewModel,
    dm: DownloadManager,
    models: List<Downloadable>
) {
    // Stato per controllare la visibilità del popup
    val showUrlDialog = remember { mutableStateOf(false) }

    // Se showUrlDialog è true, mostra il nostro popup
    if (showUrlDialog.value) {
        AddUrlDialog(
            onDismiss = { showUrlDialog.value = false },
            onConfirm = { url ->
                // Qui andrà la logica per aggiungere il nuovo modello.
                // Per ora, chiudiamo solo il dialogo.
                viewModel.log("Nuovo URL aggiunto: $url")
                showUrlDialog.value = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- SEZIONE LOG MESSAGES ---
        // Assegniamo un peso 1, occuperà metà dello spazio verticale
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
        // Assegniamo un peso 1 anche a questa, occuperà l'altra metà dello spazio
        LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            items(models) { model ->
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Downloadable.Button(viewModel, dm, model)
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
    // Stato per memorizzare l'URL inserito dall'utente nel campo di testo
    val urlText = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi Nuovo Modello") },
        text = {
            // Campo di testo per inserire l'URL
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
    // Dati di esempio per l'anteprima
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
                // Sezione log
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(messaggiDiEsempio) { messaggio ->
                            Text(messaggio, modifier = Modifier.padding(16.dp))
                        }
                    }
                }

                // Sezione download scrollabile
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

                // Pulsante Aggiungi
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
