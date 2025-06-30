package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.github.luposolitario.immundanoctis.data.*
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.GameStateManager
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import io.github.luposolitario.immundanoctis.view.SetupViewModel

class SetupActivity : ComponentActivity() {

    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    private lateinit var gameStateManager: GameStateManager
    private val viewModel: SetupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameStateManager = GameStateManager(this)

        setContent {
            val useDarkTheme = themePreferences.useDarkTheme(isSystemInDarkTheme())
            ImmundaNoctisTheme(darkTheme = useDarkTheme) {

                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as? Activity)?.window
                        window?.statusBarColor = Color.Black.toArgb()
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var sessionToLoad by remember { mutableStateOf(gameStateManager.loadSession()) }
                    var showCreationScreen by remember { mutableStateOf(sessionToLoad == null) }

                    if (showCreationScreen) {
                        // Creiamo la sessione di default qui
                        val defaultSession = remember { gameStateManager.createDefaultSession() }
                        CharacterCreationScreen(
                            viewModel = viewModel,
                            defaultSession = defaultSession, // <-- E LA PASSIAMO QUI
                            onSessionCreate = { sessionData ->
                                gameStateManager.saveSession(sessionData)
                                val intent = Intent(this, AdventureActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        )
                    } else {
                        sessionToLoad?.let {
                            ExistingSessionScreen(
                                session = it,
                                onContinue = {
                                    val intent = Intent(this, AdventureActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                },
                                onCreateNew = { showCreationScreen = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterCreationScreen(
    viewModel: SetupViewModel,
    defaultSession: SessionData, // <-- ORA LA RICEVE COME PARAMETRO
    onSessionCreate: (SessionData) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDisciplines = viewModel.selectedDisciplines

    val canProceed = uiState.combattivita > 0 && uiState.resistenza > 0 && selectedDisciplines.size == 5 && uiState.heroName.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Crea il tuo Lupo Solitario", style = MaterialTheme.typography.headlineLarge)

        OutlinedTextField(
            value = uiState.heroName,
            onValueChange = { viewModel.updateHeroName(it) },
            label = { Text("Nome del tuo Eroe") },
            modifier = Modifier.fillMaxWidth()
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Statistiche di Combattimento", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text("Combattività: ${uiState.combattivita}", fontWeight = FontWeight.Bold)
                    Text("Resistenza: ${uiState.resistenza}", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.rollStats() }) {
                    Text("Tira le Statistiche")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Scegli 5 Discipline Kai (${selectedDisciplines.size}/5)",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                KAI_DISCIPLINES.forEach { discipline ->
                    val isSelected = selectedDisciplines.contains(discipline.id)
                    val isEnabled = isSelected || selectedDisciplines.size < 5
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = isSelected,
                                enabled = isEnabled,
                                role = Role.Checkbox,
                                onValueChange = { viewModel.toggleDiscipline(discipline.id) }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isSelected, onCheckedChange = null, enabled = isEnabled)
                        Spacer(Modifier.width(16.dp))
                        Text(discipline.name, color = if (isEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Genera il tuo Ritratto", style = MaterialTheme.typography.titleLarge)
                Text("Descrivi l'aspetto del tuo personaggio.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.stdfPrompt,
                    onValueChange = { viewModel.updateStdfPrompt(it) },
                    label = { Text("es. capelli neri, occhi di ghiaccio...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { /* TODO: Avviare generazione STDF */ }) {
                    Text("Genera Ritratto (non implementato)")
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                // ORA LA VARIABILE È DISPONIBILE QUI
                val newSession = viewModel.finalizeSessionCreation(defaultSession)
                onSessionCreate(newSession)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = canProceed
        ) {
            Text("Inizia l'Avventura")
        }
    }
}

@Composable
fun ExistingSessionScreen(
    session: SessionData,
    onContinue: () -> Unit,
    onCreateNew: () -> Unit
) {
    // Il codice di questa funzione rimane invariato
    // ...
}