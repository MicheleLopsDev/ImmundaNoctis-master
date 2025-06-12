package io.github.luposolitario.immundanoctis.util

import android.net.Uri
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import java.io.File

data class Downloadable(val name: String, val source: Uri, val destination: File) {
    companion object {
        sealed interface State {
            data object Ready : State
            data class Downloading(val progress: Double) : State
            data class Downloaded(val item: Downloadable) : State
            data class Error(val message: String) : State
        }

        @JvmStatic
        @Composable
        fun Button(
            status: State,
            item: Downloadable,
            onClick: () -> Unit
        ) {
            val isEnabled = status !is State.Downloading
            Button(onClick = onClick, enabled = isEnabled) {
                when (status) {
                    is State.Downloading -> Text(text = "Downloading ${(status.progress * 100).toInt()}%")
                    is State.Downloaded -> Text("Load ${item.name}")
                    is State.Ready -> Text("Download ${item.name}")
                    is State.Error -> Text("Error: Retry Download")
                }
            }
        }
    }
}
