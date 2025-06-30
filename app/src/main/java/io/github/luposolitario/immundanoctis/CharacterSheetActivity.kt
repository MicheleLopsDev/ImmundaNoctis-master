package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import io.github.luposolitario.immundanoctis.data.*
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.ThemePreferences

class CharacterSheetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themePreferences = ThemePreferences(applicationContext)

        setContent {
            val useDarkTheme = themePreferences.useDarkTheme(isSystemInDarkTheme())
            ImmundaNoctisTheme(darkTheme = useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Creiamo un personaggio MOCK con dati aggiornati per il preview
                    val mockHero = GameCharacter(
                        id = "hero",
                        name = "Lupo Solitario",
                        type = CharacterType.PLAYER,
                        characterClass = "Iniziato Kai", // Rango invece di classe
                        portraitResId = R.drawable.portrait_hero_male,
                        gender = "MALE",
                        language = "it",
                        stats = LoneWolfStats(combattivita = 18, resistenza = 25),
                        kaiDisciplines = listOf("HEALING", "WEAPONSKILL", "MINDSHIELD", "SIXTH_SENSE", "HUNTING")
                    )
                    CharacterSheetScreen(
                        character = mockHero,
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(character: GameCharacter, onNavigateBack: () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.statusBarColor = Color.Black.toArgb()
            WindowCompat.getInsetsController(window, view)?.isAppearanceLightStatusBars = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheda Personaggio") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Torna indietro")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(character.name, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Colonna Sinistra
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatsCard(character.stats) // <-- NUOVA CARD STATISTICHE
                    KaiDisciplinesCard(character.kaiDisciplines) // <-- NUOVA CARD DISCIPLINE
                }
                // Colonna Destra
                Column(
                    modifier = Modifier.weight(0.8f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PortraitCard(character)
                }
            }
            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))
            Text("Equipaggiamento", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            InventorySection() // <-- Sezione Inventario (da dettagliare in futuro)
            Spacer(Modifier.height(16.dp))
        }
    }
}

// --- Sezione dei Composable Helper Aggiornati ---

@Composable
fun StatsCard(stats: LoneWolfStats?) {
    if (stats == null) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), Arrangement.spacedBy(8.dp)) {
            Text("Statistiche", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Combattività", style = MaterialTheme.typography.titleSmall)
                    Text("${stats.combattivita}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Resistenza", style = MaterialTheme.typography.titleSmall)
                    Text("${stats.resistenza}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun KaiDisciplinesCard(disciplineIds: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), Arrangement.spacedBy(8.dp)) {
            Text("Discipline Kai", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
            disciplineIds.forEach { disciplineId ->
                val disciplineInfo = KAI_DISCIPLINES.find { it.id == disciplineId }
                if (disciplineInfo != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(disciplineInfo.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}


@Composable
fun PortraitCard(character: GameCharacter) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Rango: ${character.characterClass}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f) // Manteniamo le proporzioni del ritratto
        ) {
            Image(
                painter = painterResource(id = character.portraitResId),
                contentDescription = "Ritratto di ${character.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun InventorySection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Sezione Armi e Oro
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = "Ascia", onValueChange = {}, label = { Text("Arma 1") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = "", onValueChange = {}, label = { Text("Arma 2") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = "10", onValueChange = {}, label = { Text("Corone d'Oro") }, modifier = Modifier.width(100.dp))
        }
        // Sezione Oggetti Speciali
        Text("Oggetti Speciali", style = MaterialTheme.typography.titleMedium)
        // Qui potremmo avere una LazyColumn o una Column per mostrare gli oggetti
        Text("Mappa di Sommerlund", style = MaterialTheme.typography.bodyMedium)

        // Sezione Zaino
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Zaino (8 slot)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4), // 4 colonne per essere più compatto
                    modifier = Modifier.height(120.dp), // Altezza fissa per la griglia
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(8) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}