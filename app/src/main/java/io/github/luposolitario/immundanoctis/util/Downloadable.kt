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

data class Downloadable(val name: String, val source: Uri, val destination: File)