package io.github.luposolitario.immundanoctis

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.GameStateManager

class DeathActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImmundaNoctisTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DeathScreen(
                        onRestart = {
                            // Cancella la sessione di gioco e ricomincia
                            GameStateManager.getInstance(this).deleteSession()
                            val intent = Intent(this, SetupActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish()
                        },
                        onExit = {
                            // Chiude l'applicazione
                            finishAffinity()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DeathScreen(onRestart: () -> Unit, onExit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LA TUA AVVENTURA TERMINA QUI",
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "La tua Resistenza è scesa a zero. Il tuo viaggio si conclude nell'oscurità.",
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text("RICOMINCIA DALL'INIZIO")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text("ESCI DAL GIOCO")
        }
    }
}