package io.github.luposolitario.immundanoctis

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImmundaNoctisTheme {
                MainMenuScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen() {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Immunda Noctis") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
            /* TODO: Avviare la schermata di gioco vera e propria */
                val intent = Intent(context, io.github.luposolitario.immundanoctis.AdventureActivity::class.java)
                context.startActivity(intent)
            }) {
                Text("Avvia Avventura")
            }
            // Pulsante per avviare la gestione dei motori
            Button(onClick = {
                // Questa è la logica per avviare un'altra Activity in Android
                val intent = Intent(context, io.github.luposolitario.immundanoctis.ModelActivity::class.java)
                context.startActivity(intent)
            }) {
                Text("Gestisci Motori IA")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuPreview() {
    ImmundaNoctisTheme {
        MainMenuScreen()
    }
}
