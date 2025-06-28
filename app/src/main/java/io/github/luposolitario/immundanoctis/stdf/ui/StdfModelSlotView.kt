package io.github.luposolitario.immundanoctis.stdf.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctis.stdf.data.StdfModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StdfModelSlotView(
    model: StdfModel,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isSelected: Boolean,
    downloadProgress: Int,
    onDownloadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSelectClick: () -> Unit,
    onGenerateClick: (StdfModel) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        // --- 👇 MODIFICA CHIAVE QUI 👇 ---
        // La logica del bordo ora è applicata tramite il Modifier
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        shape = CardDefaults.elevatedShape // Usa la stessa forma della card per avere angoli arrotondati
                    )
                } else {
                    Modifier // Se non è selezionato, non applichiamo nessun bordo aggiuntivo
                }
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = model.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (model.runOnCpu) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = if (model.runOnCpu) "CPU" else "NPU",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Dimensione",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = model.approximateSize,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Risoluzione",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${model.generationSize}x${model.generationSize}px",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            if (isDownloaded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSelected) {
                        Button(
                            onClick = { onGenerateClick(model) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Selezionato")
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Genera con questo")
                        }
                    } else {
                        OutlinedButton(onClick = onSelectClick, modifier = Modifier.weight(1f)) {
                            Text("Seleziona come Attivo")
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Filled.Delete, "Cancella Modello", tint = MaterialTheme.colorScheme.error)
                    }
                }
            } else if (isDownloading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Download in corso... ($downloadProgress%)")
                }
            } else {
                Button(
                    onClick = onDownloadClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null)
                    Spacer(Modifier.size(ButtonDefaults.IconSize))
                    Text("Scarica Modello (${model.approximateSize})")
                }
            }
        }
    }
}