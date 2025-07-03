package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import android.os.Bundle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.github.luposolitario.immundanoctis.service.TtsService
import io.github.luposolitario.immundanoctis.ui.configuration.VoiceDropdown
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.*
import kotlinx.coroutines.launch
import java.util.Locale
class ConfigurationActivity : ComponentActivity() {

    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    private val ttsPreferences by lazy { TtsPreferences(applicationContext) }
    private val savePreferences by lazy { SavePreferences(applicationContext) }
    private lateinit var gameStateManager: GameStateManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameStateManager = GameStateManager(applicationContext)

        setContent {
            val useDarkTheme = themePreferences.useDarkTheme(isSystemInDarkTheme())
            ImmundaNoctisTheme(darkTheme = useDarkTheme) {

                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        window.statusBarColor = Color.Black.toArgb()
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                            false
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainEngineScreen(
                        ttsPrefs = ttsPreferences,
                        savePrefs = savePreferences,
                        onDeleteSession = {
                            if (gameStateManager.deleteSession()) {
                                Toast.makeText(
                                    this,
                                    "Sessione cancellata con successo.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val intent = Intent(this, MainActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Errore durante la cancellazione della sessione.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainEngineScreen(
        ttsPrefs: TtsPreferences,
        savePrefs: SavePreferences,
        onDeleteSession: () -> Unit
    ) {
        val context = LocalContext.current
        var autoReadEnabled by remember { mutableStateOf(ttsPrefs.isAutoReadEnabled()) }
        var autoSaveEnabled by remember { mutableStateOf(savePrefs.isAutoSaveEnabled) }
        var chatEnabled by remember { mutableStateOf(savePrefs.isChatEnabled) } // Stato per la chat
        var speechRate by remember { mutableStateOf(ttsPrefs.getSpeechRate()) }
        var pitch by remember { mutableStateOf(ttsPrefs.getPitch()) }
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }
        var availableVoices by remember { mutableStateOf<List<android.speech.tts.Voice>>(emptyList()) }

        var selectedMaleVoiceName by remember { mutableStateOf(ttsPrefs.getVoiceForGender("MALE")) }
        var selectedFemaleVoiceName by remember { mutableStateOf(ttsPrefs.getVoiceForGender("FEMALE")) }

        var isMaleDropdownExpanded by remember { mutableStateOf(false) }
        var isFemaleDropdownExpanded by remember { mutableStateOf(false) }

        var selectedNarrativeTone by remember { mutableStateOf(savePrefs.narrativeTone) } // <-- NUOVO STATO
        var showToneDropdown by remember { mutableStateOf(false) } // <-- NUOVO STATO per il dropdown

        val narrativeTones = remember { // <-- NUOVA LISTA DI TONI
            listOf(
                "originale",
                "horror",
                "epico",
                "ironico"
            )
        }


        DisposableEffect(context) {
            var ttsService: TtsService? = null
            val onReadyListener = {
                availableVoices = ttsService?.getAvailableVoices() ?: emptyList()
            }
            ttsService = TtsService(context, onReadyListener)

            onDispose {
                ttsService.shutdown()
            }
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                icon = { Icon(Icons.Filled.Warning, contentDescription = "Attenzione") },
                title = { Text("Conferma Cancellazione") },
                text = { Text("Stai per cancellare permanentemente la sessione di gioco salvata. L'operazione non è reversibile. Vuoi continuare?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            onDeleteSession()
                        }
                    ) {
                        Text("CANCELLA")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Annulla")
                    }
                }
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Text("Impostazioni di Gioco", style = MaterialTheme.typography.titleLarge)
            // Switch per il salvataggio automatico
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Salvataggio Automatico", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Salva la chat ad ogni messaggio. Se disattivato, potrai salvare solo manualmente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoSaveEnabled,
                    onCheckedChange = {
                        autoSaveEnabled = it
                        savePrefs.isAutoSaveEnabled = it
                    }
                )
            }
            // --- NUOVO SWITCH PER LA CHAT ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Abilita Chat Personaggi", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Se disattivato, le icone di DM e PNG non saranno cliccabili per avviare una conversazione.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = chatEnabled,
                    onCheckedChange = {
                        chatEnabled = it
                        savePrefs.isChatEnabled = it
                    }
                )
            }


            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))


            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            Text("Tono Narrativo", style = MaterialTheme.typography.titleLarge)
            Text(
                "Scegli lo stile narrativo preferito per il Dungeon Master.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // NUOVO COMPONENTE DROPDOWN PER IL TONO
            ExposedDropdownMenuBox(
                expanded = showToneDropdown,
                onExpandedChange = { showToneDropdown = !showToneDropdown },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedNarrativeTone.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, // Formatta per la visualizzazione
                    onValueChange = { /* Non modificabile direttamente qui */ },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showToneDropdown) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.textFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = showToneDropdown,
                    onDismissRequest = { showToneDropdown = false }
                ) {
                    narrativeTones.forEach { tone ->
                        DropdownMenuItem(
                            text = { Text(tone.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) },
                            onClick = {
                                selectedNarrativeTone = tone
                                savePrefs.narrativeTone = tone // <-- Salva la preferenza
                                showToneDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Impostazioni Audio", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lettura Automatica Messaggi", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Leggi automaticamente i messaggi di DM e PNG.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoReadEnabled,
                    onCheckedChange = {
                        autoReadEnabled = it
                        ttsPrefs.saveAutoRead(it)
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Velocità Voce", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = speechRate,
                onValueChange = { speechRate = it },
                onValueChangeFinished = { ttsPrefs.saveSpeechRate(speechRate) },
                valueRange = 0.5f..2.0f
            )
            Spacer(Modifier.height(16.dp))
            Text("Tono Voce", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = pitch,
                onValueChange = { pitch = it },
                onValueChangeFinished = { ttsPrefs.savePitch(pitch) },
                valueRange = 0.5f..2.0f
            )

            Text("Voce Maschile", style = MaterialTheme.typography.bodyLarge)
            VoiceDropdown(
                expanded = isMaleDropdownExpanded,
                onExpandedChange = { isMaleDropdownExpanded = it },
                selectedValue = selectedMaleVoiceName,
                availableVoices = availableVoices,
                onVoiceSelected = { voiceName ->
                    selectedMaleVoiceName = voiceName
                    ttsPrefs.saveVoiceForGender("MALE", voiceName)
                    isMaleDropdownExpanded = false
                }
            )

            Spacer(Modifier.height(16.dp))
            Text("Voce Femminile", style = MaterialTheme.typography.bodyLarge)
            VoiceDropdown(
                expanded = isFemaleDropdownExpanded,
                onExpandedChange = { isFemaleDropdownExpanded = it },
                selectedValue = selectedFemaleVoiceName,
                availableVoices = availableVoices,
                onVoiceSelected = { voiceName ->
                    selectedFemaleVoiceName = voiceName
                    ttsPrefs.saveVoiceForGender("FEMALE", voiceName)
                    isFemaleDropdownExpanded = false
                }
            )

            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))
            Text("Operazioni di Emergenza", style = MaterialTheme.typography.titleLarge)
            Text(
                "Usa queste opzioni solo se l'app non funziona correttamente.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedButton(
                onClick = { showDeleteConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(
                    Icons.Filled.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Cancella Sessione di Gioco")
            }
        }
    }
}