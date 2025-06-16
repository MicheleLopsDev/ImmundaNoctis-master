package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.work.WorkManager
import io.github.luposolitario.immundanoctis.service.TtsService
import io.github.luposolitario.immundanoctis.ui.configuration.AddUrlDialog
import io.github.luposolitario.immundanoctis.ui.configuration.EngineRadioButton
import io.github.luposolitario.immundanoctis.ui.configuration.ModelSlotView
import io.github.luposolitario.immundanoctis.ui.configuration.TokenInputSection
import io.github.luposolitario.immundanoctis.ui.configuration.VoiceDropdown
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.*
import io.github.luposolitario.immundanoctis.view.MainViewModel
import java.io.File
import io.github.luposolitario.immundanoctis.util.getAppSpecificDirectory // <-- Assicurati che ci sia questo import



class ConfigurationActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val modelPreferences by lazy { ModelPreferences(applicationContext) }
    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    private val ttsPreferences by lazy { TtsPreferences(applicationContext) }
    private val workManager by lazy { WorkManager.getInstance(applicationContext) }
    private lateinit var enginePreferences: EnginePreferences
    private lateinit var gameStateManager: GameStateManager
    private val savePreferences by lazy { SavePreferences(applicationContext) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enginePreferences = EnginePreferences(applicationContext)
        gameStateManager = GameStateManager(applicationContext)
        val dmDirectory = getAppSpecificDirectory(applicationContext,"dm")
        val plDirectory = getAppSpecificDirectory(applicationContext,"pl")

        val dmModelDefault = Downloadable("gemma-2b-it-quant.bin", Uri.parse(""), File(dmDirectory, "gemma-2b-it-quant.bin"))
        val playerModelDefault = Downloadable("llama-3-8b-instruct.Q4_K_M.gguf", Uri.parse(""), File(plDirectory, "llama-3-8b-instruct.Q4_K_M.gguf"))

        val dmModel = modelPreferences.getDmModel() ?: dmModelDefault
        val playerModel = modelPreferences.getPlayerModel() ?: playerModelDefault

        setContent {
            val useDarkTheme = themePreferences.useDarkTheme(isSystemInDarkTheme())
            ImmundaNoctisTheme(darkTheme = useDarkTheme) {

                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        window.statusBarColor = Color.Black.toArgb()
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainEngineScreen(
                        viewModel = viewModel,
                        workManager = workManager,
                        modelPrefs = modelPreferences,
                        initialDmModel = dmModel,
                        initialPlayerModel = playerModel,
                        dmDirectory = dmDirectory,
                        plDirectory = plDirectory,
                        themePrefs = themePreferences,
                        enginePreferences = enginePreferences,
                        ttsPrefs = ttsPreferences,
                        savePrefs = savePreferences,
                        onDeleteSession = {
                            if (gameStateManager.deleteSession()) {
                                Toast.makeText(this, "Sessione cancellata con successo.", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, MainActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, "Errore durante la cancellazione della sessione.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

private enum class EngineOption { MIXED, GEMMA_ONLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainEngineScreen(
    viewModel: MainViewModel,
    workManager: WorkManager,
    modelPrefs: ModelPreferences,
    initialDmModel: Downloadable,
    initialPlayerModel: Downloadable,
    dmDirectory: File?,
    plDirectory: File?,
    themePrefs: ThemePreferences,
    enginePreferences: EnginePreferences,
    ttsPrefs: TtsPreferences,
    savePrefs: SavePreferences,
    onDeleteSession: () -> Unit
) {
    var dmModelState by remember { mutableStateOf(initialDmModel) }
    var playerModelState by remember { mutableStateOf(initialPlayerModel) }
    var showUrlDialogFor by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var hfToken by remember { mutableStateOf(themePrefs.getToken() ?: "") }
    var selectedEngine by remember {
        mutableStateOf(if (enginePreferences.useGemmaForAll) EngineOption.GEMMA_ONLY else EngineOption.MIXED)
    }
    var autoReadEnabled by remember { mutableStateOf(ttsPrefs.isAutoReadEnabled()) }
    var autoSaveEnabled by remember { mutableStateOf(savePrefs.isAutoSaveEnabled) }
    var speechRate by remember { mutableStateOf(ttsPrefs.getSpeechRate()) }
    var pitch by remember { mutableStateOf(ttsPrefs.getPitch()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var availableVoices by remember { mutableStateOf<List<android.speech.tts.Voice>>(emptyList()) }

    // Due stati separati per le voci maschile e femminile
    var selectedMaleVoiceName by remember { mutableStateOf(ttsPrefs.getVoiceForGender("MALE")) }
    var selectedFemaleVoiceName by remember { mutableStateOf(ttsPrefs.getVoiceForGender("FEMALE")) }

    var isMaleDropdownExpanded by remember { mutableStateOf(false) }
    var isFemaleDropdownExpanded by remember { mutableStateOf(false) }

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

    if (showUrlDialogFor != null) {
        AddUrlDialog(
            onDismiss = { showUrlDialogFor = null },
            onConfirm = { url ->
                val directory = if (showUrlDialogFor == "DM") dmDirectory else plDirectory
                val currentModel = if (showUrlDialogFor == "DM") dmModelState else playerModelState
                currentModel.destination.delete()
                val newUri = Uri.parse(url)
                val fileName = newUri.lastPathSegment?.substringBefore('?') ?: "downloaded_model"
                val newModel = Downloadable(fileName, newUri, File(directory, fileName))
                if (showUrlDialogFor == "DM") {
                    modelPrefs.saveDmModel(newModel)
                    dmModelState = newModel
                } else {
                    modelPrefs.savePlayerModel(newModel)
                    playerModelState = newModel
                }
                showUrlDialogFor = null
            }
        )
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

        // --- INSERISCI QUESTA NUOVA SEZIONE ALL'INIZIO ---
        Text("Impostazioni di Salvataggio", style = MaterialTheme.typography.titleLarge)
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
                    "Salva la chat ad ogni messaggio. Se disattivato, potrai salvare solo manualmente dal menu.",
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
        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        Text("Modalità Motore AI", style = MaterialTheme.typography.titleLarge)
        Text(
            "Scegli come l'IA gestirà i personaggi. Richiede un riavvio dell'app per avere effetto.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column {
            EngineRadioButton(
                text = "Modalità Solo Gemma (Consigliata)",
                description = "Usa Gemma per tutti i personaggi. Qualità alta, più esigente.",
                selected = selectedEngine == EngineOption.GEMMA_ONLY,
                onClick = {
                    selectedEngine = EngineOption.GEMMA_ONLY
                    enginePreferences.useGemmaForAll = true
                }
            )
            EngineRadioButton(
                text = "Modalità Mista ",
                description = "Usa Gemma per il DM e GGUF per i PG. Puoi sperimentare piu motori.",
                selected = selectedEngine == EngineOption.MIXED,
                onClick = {
                    selectedEngine = EngineOption.MIXED
                    enginePreferences.useGemmaForAll = false
                }
            )
        }
        Spacer(Modifier.height(16.dp))
        val isGgufEnabled = selectedEngine == EngineOption.MIXED

        ModelSlotView(
            title = "Motore del Dungeon Master (Gemma)",
            subtitle = "Modello per narrazione e ambiente.Consigliato: Gemma",
            model = dmModelState,
            token = hfToken,
            viewModel = viewModel,
            workManager = workManager,
            onSetUrlClick = { showUrlDialogFor = "DM" },
            onDownloadComplete = { modelPrefs.saveDmModel(it) },
            onDeleteClick = {
                workManager.cancelAllWorkByTag(dmModelState.name)
                dmModelState.destination.delete()
                modelPrefs.clearDmModel()
                (context as? Activity)?.recreate()
            },
            enabled = true
        )

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        Column(modifier = Modifier.alpha(if (isGgufEnabled) 1f else 0.5f)) {
            ModelSlotView(
                title = "Motore dei Personaggi (GGUF)",
                subtitle = "Modello per le risposte dei PG. Disabilitato in modalità 'Solo Gemma'.",
                model = playerModelState,
                token = hfToken,
                viewModel = viewModel,
                workManager = workManager,
                onSetUrlClick = { if (isGgufEnabled) showUrlDialogFor = "PLAYER" },
                onDeleteClick = {
                    workManager.cancelAllWorkByTag(playerModelState.name)
                    playerModelState.destination.delete()
                    modelPrefs.clearPlayerModel()
                    (context as? Activity)?.recreate()
                },
                onDownloadComplete = { modelPrefs.savePlayerModel(it) },
                enabled = isGgufEnabled
            )
        }

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        // --- SEZIONE IMPOSTAZIONI TTS ---
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
        Divider()
        Spacer(Modifier.height(16.dp))
        // --- MENU PER VOCE MASCHILE ---
        Spacer(Modifier.height(16.dp))
        Text("Velocità Voce", style = MaterialTheme.typography.bodyLarge)
        Slider(value = speechRate, onValueChange = { speechRate = it }, onValueChangeFinished = { ttsPrefs.saveSpeechRate(speechRate) }, valueRange = 0.5f..2.0f)
        Spacer(Modifier.height(16.dp))
        Text("Tono Voce", style = MaterialTheme.typography.bodyLarge)
        Slider(value = pitch, onValueChange = { pitch = it }, onValueChangeFinished = { ttsPrefs.savePitch(pitch) }, valueRange = 0.5f..2.0f)

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
        // --- MENU PER VOCE FEMMINILE ---
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


        // --- SEZIONE: Operazioni di Emergenza (CORRETTA) ---
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
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error) // Corretto
        ) {
            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Cancella Sessione di Gioco")
        }


        Spacer(Modifier.weight(1f, fill = false))
        Spacer(Modifier.height(24.dp))
        TokenInputSection(
            token = hfToken,
            onTokenChange = { hfToken = it },
            onSaveClick = {
                themePrefs.saveToken(hfToken)
                viewModel.log("Token Hugging Face salvato.")
            }
        )
    }
  }
}

