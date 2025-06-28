package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.ThemePreferences

class MainActivity : ComponentActivity() {
    private val themePreferences by lazy { ThemePreferences(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // --- 👇 LOGICA DEL TEMA RIPRISTINATA 👇 ---
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

// Componente per la singola icona del menu (rimane invariato)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuIcon(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState()
    ) {
        Column(
            modifier = modifier.clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(120.dp), // Dimensioni più generose
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(64.dp), // Icona grande
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
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
                // --- 👇 PULSANTE PER CAMBIARE TEMA RIPRISTINATO 👇 ---
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
        // --- 👇 LAYOUT A COLONNE E RIGHE PER UN CONTROLLO PRECISO 👇 ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp), // Aumentato il padding per più spazio
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Prima riga di icone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MenuIcon(icon = Icons.Default.SportsEsports, label = "Avventura") {
                    context.startActivity(Intent(context, SetupActivity::class.java))
                }
                MenuIcon(icon = Icons.Default.Palette, label = "Genera Immagini") {
                    context.startActivity(Intent(context, StdfGenerationActivity::class.java))
                }
            }

            Spacer(modifier = Modifier.height(32.dp)) // Aumentato lo spazio tra le righe

            // Seconda riga di icone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MenuIcon(icon = Icons.Default.Psychology, label = "Modelli LLM") {
                    context.startActivity(Intent(context, ModelActivity::class.java))
                }
                MenuIcon(icon = Icons.Default.ImageSearch, label = "Modelli STDF") {
                    context.startActivity(Intent(context, StdfModelActivity::class.java))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Terza riga con l'icona delle impostazioni centrata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                MenuIcon(icon = Icons.Default.Settings, label = "Impostazioni") {
                    context.startActivity(Intent(context, ConfigurationActivity::class.java))
                }
            }
        }
    }
}