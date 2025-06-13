package io.github.luposolitario.immundanoctis.util

import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import java.io.File

data class Downloadable(val name: String, val source: Uri, val destination: File) {
    companion object {
        sealed interface State {
            data object Ready : State
            // MODIFICA: La definizione ora accetta due Long, come previsto
            data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : State
            data class Downloaded(val item: Downloadable) : State
            data class Error(val message: String) : State
        }

        @JvmStatic
        @Composable
        fun Button(
            status: State,
            item: Downloadable,
            onClick: () -> Unit,
            cancelIcon: @Composable (() -> Unit)? = null
        ) {
            val context = LocalContext.current
            val isEnabled = status !is State.Downloading

            if (status is State.Downloading && cancelIcon != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {}, enabled = false) {
                        // MODIFICA: Logica di visualizzazione del progresso aggiornata
                        val progressText = if (status.totalBytes > 0) {
                            "${((status.bytesDownloaded.toDouble() / status.totalBytes) * 100).toInt()}%"
                        } else {
                            Formatter.formatShortFileSize(context, status.bytesDownloaded)
                        }
                        Text(text = "Downloading ($progressText)")
                    }
                    cancelIcon()
                }
            } else {
                Button(onClick = onClick, enabled = isEnabled) {
                    when (status) {
                        is State.Downloading -> { // Questo caso serve se non c'è l'icona di annullamento
                            val progressText = if (status.totalBytes > 0) {
                                "${((status.bytesDownloaded.toDouble() / status.totalBytes) * 100).toInt()}%"
                            } else {
                                Formatter.formatShortFileSize(context, status.bytesDownloaded)
                            }
                            Text(text = "Downloading ($progressText)")
                        }
                        is State.Downloaded -> Text("Load ${item.name}")
                        is State.Ready -> Text("Download ${item.name}")
                        is State.Error -> Text(status.message)
                    }
                }
            }
        }
    }
}
