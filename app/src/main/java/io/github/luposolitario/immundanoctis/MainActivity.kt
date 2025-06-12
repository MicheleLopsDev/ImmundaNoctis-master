package io.github.luposolitario.immundanoctis

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.ThemePreferences

class MainActivity : ComponentActivity() {
    private val themePreferences by lazy { ThemePreferences(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Leggiamo la preferenza salvata, usando quella di sistema come fallback
            val isSystemDark = isSystemInDarkTheme()
            var isDarkTheme by remember {
                mutableStateOf(themePreferences.useDarkTheme(isSystemDark))
            }

            // Passiamo lo stato al nostro tema principale
            ImmundaNoctisTheme(darkTheme = isDarkTheme) {
                MainMenuScreen(
                    // Passiamo lo stato e l'azione per cambiarlo
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = {
                        // Quando l'utente cambia tema...
                        val newThemeState = !isDarkTheme
                        isDarkTheme = newThemeState         // ...aggiorna lo stato della UI...
                        themePreferences.saveTheme(newThemeState) // ...e salva la scelta!
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(isDarkTheme: Boolean, onThemeToggle: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Immunda Noctis") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = onThemeToggle) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Cambia Tema"
                        )
                    }
                }
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
                val intent = Intent(context, AdventureActivity::class.java)
                context.startActivity(intent)
            }) {
                Text("Avvia Avventura")
            }
            Button(onClick = {
                val intent = Intent(context, ModelActivity::class.java)
                context.startActivity(intent)
            }) {
                Text("Gestisci Motori IA")
            }
        }
    }
}

@Preview(showBackground = true, name = "Menu Principale (Chiaro)")
@Composable
fun MainMenuPreview() {
    ImmundaNoctisTheme(darkTheme = false) {
        MainMenuScreen(isDarkTheme = false, onThemeToggle = {})
    }
}

@Preview(showBackground = true, name = "Menu Principale (Scuro)")
@Composable
fun MainMenuPreviewDark() {
    ImmundaNoctisTheme(darkTheme = true) {
        MainMenuScreen(isDarkTheme = true, onThemeToggle = {})
    }
}
