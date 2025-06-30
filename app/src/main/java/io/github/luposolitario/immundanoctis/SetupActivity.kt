package io.github.luposolitario.immundanoctis

// Aggiungi questo import all'inizio del file SetupActivity.kt
import androidx.compose.foundation.rememberScrollState
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.data.KAI_DISCIPLINES
import io.github.luposolitario.immundanoctis.data.SessionData
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.GameStateManager
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import io.github.luposolitario.immundanoctis.view.SetupViewModel
import java.text.SimpleDateFormat
import java.util.*

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
                        val defaultSession = remember { gameStateManager.createDefaultSession() }
                        CharacterCreationScreen(
                            viewModel = viewModel,
                            defaultSession = defaultSession,
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
    defaultSession: SessionData,
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
        // ... Contenuto di CharacterCreationScreen che abbiamo già definito ...
        Text("Crea il tuo Lupo Solitario", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(
            value = uiState.heroName,
            onValueChange = { viewModel.updateHeroName(it) },
            label = { Text("Nome del tuo Eroe") },
            modifier = Modifier.fillMaxWidth()
        )
        // ... (resto dei componenti)
        Button(
            onClick = {
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


// --- FUNZIONI RIPRISTINATE ---

@Composable
fun ExistingSessionScreen(
    session: SessionData,
    onContinue: () -> Unit,
    onCreateNew: () -> Unit
) {
    val formattedDate = remember {
        SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(session.lastUpdate))
    }
    val hero = session.characters.find { it.id == CharacterID.HERO }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bentornato!", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))
        Card(elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Campagna:", style = MaterialTheme.typography.titleMedium)
                Text(session.sessionName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Ultimo salvataggio: $formattedDate", style = MaterialTheme.typography.bodySmall)
                if (hero != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    RobustImage(
                        resId = hero.portraitResId,
                        contentDescription = "Ritratto Eroe",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(128.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(hero.name, style = MaterialTheme.typography.titleLarge)
                    Text(hero.characterClass, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Continua questa Avventura")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onCreateNew, modifier = Modifier.fillMaxWidth()) {
            Text("Crea Nuova Avventura (sovrascrivi)")
        }
    }
}

@Composable
fun RobustImage(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: @Composable (Modifier) -> Unit = { mod ->
        Icon(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = "Immagine non caricata",
            modifier = mod
        )
    }
) {
    val painter = painterResource(id = resId)
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}