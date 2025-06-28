package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.ThemePreferences

class MainActivity : ComponentActivity() {
    private val themePreferences by lazy { ThemePreferences(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isSystemDark = isSystemInDarkTheme()
            var isDarkTheme by remember {
                mutableStateOf(themePreferences.useDarkTheme(isSystemDark))
            }

            ImmundaNoctisTheme(darkTheme = isDarkTheme) {
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        window.statusBarColor = Color.Black.toArgb()
                        // --- MODIFICA CHIAVE ---
                        // Forziamo le icone della status bar a essere CHIARE, sempre.
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                    }
                }

                MainMenuScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = {
                        val newThemeState = !isDarkTheme
                        isDarkTheme = newThemeState
                        themePreferences.saveTheme(newThemeState)
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
                            contentDescription = "Cambia Tema",
                            // --- TEST: Usiamo un colore fisso ---
                            tint = Color.Red
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // ... (dentro il Composable MainMenuScreen in MainActivity.kt)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding), // Usa innerPadding dal Scaffold
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                val intent = Intent(context, SetupActivity::class.java)
                context.startActivity(intent)
            }) {
                Text("Inizia / Continua Avventura")
            }

            Spacer(modifier = Modifier.height(16.dp))


            Button(onClick = {
                // Lancia la nostra nuova Activity per i modelli di IMMAGINE
                val intent = Intent(context, StdfGenerationActivity::class.java)
                context.startActivity(intent)
            }) {
                Text("Genera immagini")
            }

            // --- 👇 AGGIUNGI QUESTO NUOVO PULSANTE 👇 ---
            Spacer(modifier = Modifier.height(16.dp))


            Button(onClick = {
                val intent = Intent(context, ConfigurationActivity::class.java)
                context.startActivity(intent)
            }) {
                Text("Impostazioni Generali")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 👇 MODIFICA QUI 👇 ---
            // Specifichiamo che questo è per i modelli di LINGUAGGIO (LLM)
            Button(onClick = {
                val intent = Intent(context, ModelActivity::class.java)
                context.startActivity(intent)
            }) {
                Text("Gestione Modelli Linguaggio (LLM)")
            }

            // --- 👇 AGGIUNGI QUESTO NUOVO PULSANTE 👇 ---
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                // Lancia la nostra nuova Activity per i modelli di IMMAGINE
                val intent = Intent(context, StdfModelActivity::class.java)
                context.startActivity(intent)
            }) {
                Text("Gestione Modelli Immagine (STDF)")
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
