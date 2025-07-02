package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.github.luposolitario.immundanoctis.data.CharacterID
import io.github.luposolitario.immundanoctis.data.KaiDisciplineInfo
import io.github.luposolitario.immundanoctis.data.KAI_DISCIPLINES
import io.github.luposolitario.immundanoctis.data.SessionData
import io.github.luposolitario.immundanoctis.ui.adventure.getIconForDiscipline
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.GameStateManager
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import io.github.luposolitario.immundanoctis.view.SetupUiState
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
        viewModel.initialize(applicationContext)

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

    val canProceed = uiState.combattivita > 0 &&
            uiState.resistenza > 0 &&
            selectedDisciplines.size == 5 &&
            uiState.selectedWeapon != null &&
            uiState.selectedSpecialItem != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Crea il Tuo Eroe", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Lupo Solitario",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        RandomStatsCard(
            combatSkill = uiState.combattivita,
            endurance = uiState.resistenza,
            onRandomizeClick = { viewModel.rollStats() }
        )

        EquipmentChoiceCard(
            uiState = uiState,
            onWeaponSelected = { viewModel.onWeaponSelected(it) },
            onSpecialItemSelected = { viewModel.onSpecialItemSelected(it) }
        )

        DisciplineGridCard(
            selectedDisciplines = selectedDisciplines.toList(),
            onDisciplineSelected = { viewModel.toggleDiscipline(it) }
        )

        Spacer(Modifier.weight(1f))

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

@Composable
fun RandomStatsCard(combatSkill: Int, endurance: Int, onRandomizeClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Statistiche di Combattimento", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text("Combattività: $combatSkill", fontWeight = FontWeight.Bold)
                Text("Resistenza: $endurance", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRandomizeClick) {
                Text("Tira le Statistiche")
            }
        }
    }
}

@Composable
fun EquipmentChoiceCard(
    uiState: SetupUiState,
    onWeaponSelected: (String) -> Unit,
    onSpecialItemSelected: (String) -> Unit
) {
    val weapons = listOf(
        EquipmentItem("Ascia", "Un'arma affidabile e bilanciata.", R.drawable.ic_axe),
        EquipmentItem("Spada", "Veloce e letale, un classico per ogni avventuriero.", R.drawable.ic_sword)
    )
    val specialItems = listOf(
        EquipmentItem("Mappa", "Rivela la tua posizione nel mondo di gioco.", R.drawable.ic_map),
        EquipmentItem("Zaino", "Permette di trasportare fino a 8 oggetti.", R.drawable.ic_backpack),
        EquipmentItem("Pozione di Vigorilla", "Ripristina 4 punti Resistenza quando usata.", R.drawable.ic_potion)
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Scegli Equipaggiamento Iniziale",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))

            Text("Scegli un'arma (obbligatorio):", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                weapons.forEach { item ->
                    EquipmentChoiceRow(
                        item = item,
                        isSelected = (uiState.selectedWeapon == item.name),
                        onClick = { onWeaponSelected(item.name) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            Text("Scegli UN solo oggetto speciale:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                specialItems.forEach { item ->
                    EquipmentChoiceRow(
                        item = item,
                        isSelected = (uiState.selectedSpecialItem == item.name),
                        onClick = { onSpecialItemSelected(item.name) }
                    )
                }
            }
        }
    }
}

private data class EquipmentItem(val name: String, val description: String, @DrawableRes val iconResId: Int)

@Composable
private fun EquipmentChoiceRow(
    item: EquipmentItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if(isSelected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = item.iconResId),
                contentDescription = item.name,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


// --- SEZIONE DISCIPLINE COMPLETAMENTE RIDISEGNATA ---
@Composable
fun DisciplineGridCard(
    selectedDisciplines: List<String>,
    onDisciplineSelected: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Scegli 5 Discipline Kai (${selectedDisciplines.size}/5)",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // Usiamo una griglia per un layout più compatto e piacevole
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(380.dp) // Altezza fissa per evitare scroll dentro scroll
            ) {
                items(KAI_DISCIPLINES) { discipline ->
                    val isSelected = selectedDisciplines.contains(discipline.id)
                    val isEnabled = isSelected || selectedDisciplines.size < 5
                    DisciplineChoiceCard(
                        discipline = discipline,
                        isSelected = isSelected,
                        enabled = isEnabled,
                        onClick = { onDisciplineSelected(discipline.id) }
                    )
                }
            }
        }
    }
}

// --- NUOVO COMPONENTE PER LA CARD DELLA DISCIPLINA ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisciplineChoiceCard(
    discipline: KaiDisciplineInfo,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp),
        modifier = Modifier.alpha(if (enabled) 1f else 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getIconForDiscipline(discipline.id),
                contentDescription = discipline.name,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = discipline.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = discipline.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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