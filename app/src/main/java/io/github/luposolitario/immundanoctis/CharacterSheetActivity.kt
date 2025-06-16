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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.luposolitario.immundanoctis.data.CharacterType
import io.github.luposolitario.immundanoctis.data.GameCharacter
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
                    // Creiamo un personaggio MOCK con dati di esempio
                    val mockHero = GameCharacter(
                        id = "hero",
                        name = "Kaelan",
                        type = CharacterType.PLAYER,
                        characterClass = "Guerriero",
                        portraitResId = R.drawable.portrait_hero_male,
                        gender = "MALE",
                        language = "it"
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
    // --- Gestione Colori Status Bar ---
    val view = LocalView.current
    if (!view.isInEditMode) {
        val primaryColor = MaterialTheme.colorScheme.primary
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = primaryColor.toArgb()
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
            // --- PARTE SUPERIORE ---
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
                    InfoCard(character)
                    AttributesCard()
                }
                // Colonna Destra
                Column(
                    modifier = Modifier.weight(0.8f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PortraitCard(character)
                    HistoryCard()
                }
            }
            Spacer(Modifier.height(16.dp))
            ExperienceCard()

            // --- PARTE INFERIORE ---
            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))
            Text("Inventario", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            InventorySection()
            Spacer(Modifier.height(16.dp))
        }
    }
}

// --- Sezione dei Composable Helper per pulizia ---

@Composable
fun InfoCard(character: GameCharacter) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Classe: ${character.characterClass}", fontWeight = FontWeight.Bold)
                Text("Livello: 1", fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("PV: 25/25")
                Text("CA: 16")
            }
            Text("Punti Fato: 3")
        }
    }
}

@Composable
fun AttributesCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), Arrangement.spacedBy(8.dp)) {
            Text("Caratteristiche", style = MaterialTheme.typography.titleMedium)
            AttributeRow("Forza", "16", "+3")
            AttributeRow("Astuzia", "12", "+1")
            AttributeRow("Sapere", "10", "+0")
            AttributeRow("Magia", "8", "-1")
        }
    }
}

@Composable
fun AttributeRow(name: String, value: String, modifier: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        OutlinedTextField(
            value = modifier,
            onValueChange = {},
            modifier = Modifier.width(70.dp).padding(start = 16.dp),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )
    }
}

@Composable
fun PortraitCard(character: GameCharacter) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Ritratto", style = MaterialTheme.typography.titleSmall)
        Card(
            modifier = Modifier
                .width(150.dp) // Più stretto
                .height(200.dp) // Più alto
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
fun HistoryCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Storia", style = MaterialTheme.typography.titleSmall)
            Text(
                "Alex ha lasciato il suo piccolo villaggio dopo un'incursione di goblin. Cerca risposte e vendetta, armato solo della vecchia spada di suo padre e di una determinazione incrollabile.",
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ExperienceCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Esperienza (PX)", style = MaterialTheme.typography.titleMedium)
                Text("250 / 1000")
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 0.25f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun InventorySection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = "Armatura di Cuoio", onValueChange = {}, label = { Text("Armatura") }, modifier = Modifier.weight(2f))
            OutlinedTextField(value = "1d6", onValueChange = {}, label = { Text("Parata") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = "Anello della Protezione", onValueChange = {}, label = { Text("Anello") }, modifier = Modifier.weight(2f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = "Spada Lunga", onValueChange = {}, label = { Text("Arma") }, modifier = Modifier.weight(2f))
            OutlinedTextField(value = "1d8", onValueChange = {}, label = { Text("Danno") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = "15", onValueChange = {}, label = { Text("Oro") }, modifier = Modifier.weight(2f))
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Zaino (9 slot)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(180.dp), // Altezza fissa per la griglia
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(9) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}
