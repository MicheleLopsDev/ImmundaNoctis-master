package io.github.luposolitario.immundanoctis.ui.configuration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.Downloadable
import io.github.luposolitario.immundanoctis.view.MainViewModel
import io.github.luposolitario.immundanoctis.worker.DownloadWorker


// Incolla questo blocco all'inizio del file ModelSlot.kt

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Download
import java.io.FileOutputStream


// Inserisci questo codice DOPO la fine della classe ConfigurationActivity

// File: ui/configuration/ModelSlot.kt

@Composable
fun ModelSlotView(
    title: String,
    subtitle: String,
    model: Downloadable,
    workManager: WorkManager,
    onSetUrlClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onSelectFileClick: () -> Unit,
    onDeleteClick: () -> Unit,
    enabled: Boolean
) {
    // --- Osservazione dello stato del Worker (corretta per gestire la lista) ---
    val workInfoList by workManager.getWorkInfosByTagLiveData(model.name).observeAsState()
    val workInfo = workInfoList?.firstOrNull()
    val isDownloading = workInfo?.state == WorkInfo.State.RUNNING

    val downloadProgress = if (isDownloading && workInfo != null) {
        val downloaded = workInfo.progress.getLong(DownloadWorker.KEY_BYTES_DOWNLOADED, 0L)
        val total = workInfo.progress.getLong(DownloadWorker.KEY_TOTAL_BYTES, 1L)
        if (total > 0) downloaded.toFloat() / total.toFloat() else 0f
    } else {
        0f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            // --- NUOVA LOGICA ---
            // Prima controlliamo solo se il file esiste.
            if (model.destination.exists() && model.destination.length() > 0) {
                // STATO 1: File scaricato e presente.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Modello Caricato", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Caricato: ${model.name}",
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onDeleteClick, enabled = enabled) {
                        Icon(Icons.Filled.Delete, contentDescription = "Cancella Modello")
                    }
                }
            } else {
                // STATO 2: File non presente. Mostriamo sempre i controlli per aggiungerlo.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onSetUrlClick, enabled = enabled, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("URL")
                    }
                    OutlinedButton(onClick = onSelectFileClick, enabled = enabled, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("File")
                    }
                }

                Spacer(Modifier.height(12.dp))

                // E POI, mostriamo la barra di progresso o il pulsante di download.
                if (isDownloading) {
                    // Se il download è ATTIVO, mostriamo la progress bar.
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(text = "Download in corso... (${(downloadProgress * 100).toInt()}%)")
                    }
                } else {
                    // Altrimenti, mostriamo il pulsante per AVVIARE il download.
                    Button(
                        onClick = onDownloadClick,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Scarica Modello")
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
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inserisci URL del Modello") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL Hugging Face o diretto") },
                // --- MODIFICA: Aumentata la dimensione del campo di testo ---
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (url.isNotBlank()) onConfirm(url) }) {
                Text("Conferma")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
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
