package io.github.luposolitario.immundanoctis.ui.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.Downloadable
import io.github.luposolitario.immundanoctis.view.MainViewModel
import io.github.luposolitario.immundanoctis.worker.DownloadWorker

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
                DownloadWorker.KEY_MODEL_DOWNLOAD_ACCESS_TOKEN to token
            )
            val downloadWorkRequest =
                OneTimeWorkRequestBuilder<DownloadWorker>()
                    .setInputData(downloadData)
                    .addTag(model.name)
                    .build()
            workManager.enqueueUniqueWork(
                model.name,
                ExistingWorkPolicy.REPLACE,
                downloadWorkRequest
            )
            viewModel.log("Download per ${model.name} messo in coda.")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                        Icon(
                            Icons.Default.Delete,
                            "Cancella Modello",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
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
