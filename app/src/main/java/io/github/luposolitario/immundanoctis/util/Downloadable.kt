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
            cancelIcon: @Composable (() -> Unit)? = null,
            // --- MODIFICA 1: Aggiunto il parametro 'enabled' ---
            enabled: Boolean = true
        ) {
            val context = LocalContext.current
            // Ora l'abilitazione del pulsante dipende sia dallo stato del download
            // SIA dal parametro 'enabled' che viene passato dall'esterno.
            val isClickable = enabled && (status !is State.Downloading)

            if (status is State.Downloading && cancelIcon != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {}, enabled = false) { // Il pulsante di progresso è sempre disabilitato
                        val progressText = if (status.totalBytes > 0) {
                            "${((status.bytesDownloaded.toDouble() / status.totalBytes) * 100).toInt()}%"
                        } else {
                            Formatter.formatShortFileSize(context, status.bytesDownloaded)
                        }
                        Text(text = "Download ($progressText)")
                    }
                    cancelIcon()
                }
            } else {
                // --- MODIFICA 2: Usiamo la nostra nuova variabile 'isClickable' ---
                Button(onClick = onClick, enabled = isClickable) {
                    val text = when (status) {
                        is State.Downloading -> {
                            val progressText = if (status.totalBytes > 0) {
                                "${((status.bytesDownloaded.toDouble() / status.totalBytes) * 100).toInt()}%"
                            } else {
                                Formatter.formatShortFileSize(context, status.bytesDownloaded)
                            }
                            "Annulla: " + item.name + " ($progressText)"
                        }
                        is State.Downloaded -> "Ricarica: " + item.name
                        is State.Ready -> "Download: " + item.name
                        is State.Error -> "Riprova: " + item.name
                    }
                    Text(text)
                }
            }
        }
    }
}
