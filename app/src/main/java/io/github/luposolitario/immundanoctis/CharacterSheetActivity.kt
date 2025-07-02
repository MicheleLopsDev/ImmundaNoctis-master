// File: app/src/main/java/io/github/luposolitario/immundanoctis/CharacterSheetActivity.kt
package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Info // Potrebbe non servire più se si usano icone specifiche per slot vuoti
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Import per osservare StateFlow
import androidx.lifecycle.viewmodel.compose.viewModel // Import per ottenere il ViewModel
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.data.GameItem
import io.github.luposolitario.immundanoctis.data.ItemType
import io.github.luposolitario.immundanoctis.data.LoneWolfStats
import io.github.luposolitario.immundanoctis.view.CharacterSheetViewModel // Import del nuovo ViewModel
import io.github.luposolitario.immundanoctis.view.CharacterSheetUiState // Import dello stato UI

class CharacterSheetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImmundaNoctisTheme {
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as? Activity)?.window
                        window?.statusBarColor = Color.Black.toArgb()
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                    }
                }
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Ottiene un'istanza del CharacterSheetViewModel
                    // Il ViewModel viene creato e gestito dal sistema Android
                    val viewModel: CharacterSheetViewModel = viewModel()
                    // Osserva lo stato della UI dal ViewModel
                    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                    CharacterSheetScreen(uiState = uiState)
                }
            }
        }
    }
}

@Composable
fun CharacterSheetScreen(uiState: CharacterSheetUiState) { // Ora accetta lo stato UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sezione Superiore (Ritratto, Nome, Rango e ORO)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                // Ritratto Eroe - Usa l'icona lupo_solitario.png
                Image(
                    painter = painterResource(id = R.drawable.lupo_solitario), // Icona specifica per il ritratto
                    contentDescription = "Ritratto Eroe",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text(uiState.heroCharacter?.name ?: "Eroe Sconosciuto", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(uiState.kaiRank, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary) // Rango Kai dinamico
            }
            // ORO - Usa il conteggio reale dall'uiState
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.5f)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_gold), // Icona specifica per l'oro
                    contentDescription = "Oro",
                    modifier = Modifier.size(48.dp)
                )
                Text("Oro", style = MaterialTheme.typography.titleMedium)
                Text("${uiState.goldCount}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) // Oro dinamico
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 1. Card "Statistiche e Pasti"
        StatsAndMealsCard( // Rimosso "Mock" dal nome
            combatSkill = uiState.combatSkill, // Dati dinamici
            endurance = uiState.endurance,     // Dati dinamici
            meals = uiState.mealsCount         // Dati dinamici
        )

        // 2. Card "Armi"
        WeaponsCard( // Rimosso "Mock" dal nome
            primaryWeapon = uiState.equippedWeapon, // Arma equipaggiata dinamica
            secondaryWeapons = uiState.otherWeapons // Altre armi dinamiche
        )

        // 3. Card "Oggetti Comuni" (Zaino)
        CommonItemsCard( // Rimosso "Mock" dal nome
            commonItems = uiState.backpackItems // Oggetti zaino dinamici
        )

        // 4. Card "Oggetti Speciali" (Tabella)
        SpecialItemsTableCard( // Rimosso "Mock" dal nome
            specialItems = uiState.specialItems // Oggetti speciali dinamici
        )
    }
}

@Composable
fun StatsAndMealsCard(combatSkill: Int, endurance: Int, meals: Int) { // Rimosso "Mock" dal nome
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Statistiche e Pasti", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Combattività", style = MaterialTheme.typography.titleMedium)
                    Text("$combatSkill", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Resistenza", style = MaterialTheme.typography.titleMedium)
                    Text("$endurance", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Pasti", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painter = painterResource(id = R.drawable.ic_meal), contentDescription = "Pasti", modifier = Modifier.size(32.dp)) // Icona pasti
                        Spacer(Modifier.width(8.dp))
                        Text("$meals", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        // RIMOSSI i pulsanti (+) e (-) come richiesto
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Pulsante Note - Icona aggiornata
                Button(onClick = { /* Mock: Open Notes */ }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painter = painterResource(id = R.drawable.ic_map), contentDescription = "Note", modifier = Modifier.size(24.dp)) // Icona specifica
                        Spacer(Modifier.width(8.dp))
                        Text("Note", fontSize = 14.sp)
                    }
                }
                // Pulsante Mappa - Icona aggiornata
                Button(onClick = { /* Mock: Open Map */ }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painter = painterResource(id = R.drawable.ic_map), contentDescription = "Mappa", modifier = Modifier.size(24.dp)) // Icona specifica
                        Spacer(Modifier.width(8.dp))
                        Text("Mappa", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun WeaponsCard(primaryWeapon: GameItem?, secondaryWeapons: List<GameItem>) { // Rimosso "Mock" dal nome, accetta GameItem
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
            Text("Armi", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primo Slot Arma (l'arma primaria/attiva)
                WeaponSlot(weapon = primaryWeapon, isActive = true)

                // Secondo Slot Arma (se presente, altrimenti vuoto)
                // Se otherWeapons è vuota, mostriamo uno slot vuoto
                // Altrimenti, mostriamo la prima arma dalla lista otherWeapons
                val secondaryWeapon = secondaryWeapons.firstOrNull()
                WeaponSlot(weapon = secondaryWeapon, isActive = false)
            }
        }
    }
}

@Composable
fun WeaponSlot(weapon: GameItem?, isActive: Boolean) { // Rimosso "Mock" dal nome, accetta GameItem
    val borderColor = if (isActive) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline
    val backgroundColor = if (isActive) Color(0xFFFFD700).copy(alpha = 0.2f) else Color.Transparent
    val isEmpty = weapon == null || weapon.name.isEmpty()

    OutlinedCard(
        modifier = Modifier
            .width(150.dp)
            .height(120.dp)
            .padding(4.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isEmpty) {
                Image(
                    painter = painterResource(id = R.drawable.ic_unknown_item), // Icona specifica per slot vuoto
                    contentDescription = "Slot Arma Vuoto",
                    modifier = Modifier.size(48.dp),
                    // tint = MaterialTheme.colorScheme.onSurfaceVariant // Rimosso tint per Image
                )
                Text("Slot Arma", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                // Determina l'icona dell'arma in base al nome o tipo
                val weaponIconRes = when (weapon?.name?.lowercase()) {
                    "spada suprema", "spada" -> R.drawable.ic_sword
                    "ascia da battaglia", "ascia" -> R.drawable.ic_axe
                    "broadsword" -> R.drawable.ic_broadsword
                    "mace" -> R.drawable.ic_mace
                    "spear" -> R.drawable.ic_spear
                    "staff" -> R.drawable.ic_staff
                    else -> R.drawable.ic_sword // Fallback se non c'è un'icona specifica
                }
                Image(
                    painter = painterResource(id = weaponIconRes),
                    contentDescription = weapon?.name ?: "Arma",
                    modifier = Modifier.size(48.dp)
                )
                Text(weapon?.name ?: "N/A", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun CommonItemsCard(commonItems: List<GameItem>) { // Rimosso "Mock" dal nome, accetta lista reale
    // Riempiamo gli slot vuoti per un totale di 8, se ci sono meno di 8 oggetti
    val paddedCommonItems = commonItems.toMutableList()
    while (paddedCommonItems.size < 8) {
        paddedCommonItems.add(GameItem(name = "", type = ItemType.BACKPACK_ITEM, quantity = 0, iconResId = R.drawable.ic_unknown_item))
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Oggetti Comuni (Zaino)", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4), // 4 colonne
                modifier = Modifier.fillMaxWidth().height(200.dp), // Altezza fissa per 2 righe di 4
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(paddedCommonItems) { item ->
                    CommonItemSlot(item = item)
                }
            }
        }
    }
}

@Composable
fun CommonItemSlot(item: GameItem) { // Rimosso "Mock" dal nome
    val isEmpty = item.name.isEmpty() || item.quantity == 0
    val iconRes = if (isEmpty) R.drawable.ic_unknown_item else item.iconResId ?: R.drawable.ic_unknown_item // Usa ic_empty_slot per vuoto

    OutlinedCard(
        modifier = Modifier
            .aspectRatio(1f) // Rende gli slot quadrati
            .fillMaxSize(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = item.name.ifEmpty { "Slot Vuoto" },
                modifier = Modifier.size(40.dp)
            )
            if (!isEmpty) {
                Text(item.name, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                if (item.quantity > 1) {
                    Text("x${item.quantity}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Vuoto", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun SpecialItemsTableCard(specialItems: List<GameItem>) { // Rimosso "Mock" dal nome, accetta lista reale
    // Riempiamo fino a 10 righe se ci sono meno di 10 oggetti
    val paddedSpecialItems = specialItems.toMutableList()
    while (paddedSpecialItems.size < 10) {
        paddedSpecialItems.add(GameItem(name = "", description = "", type = ItemType.SPECIAL_ITEM)) // Oggetto vuoto
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Oggetti Speciali", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                // Intestazione della tabella
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text("Nome", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.4f), textAlign = TextAlign.Center)
                    Text("Descrizione", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
                }
                // Righe della tabella
                paddedSpecialItems.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            item.name.ifEmpty { "---" },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(0.4f),
                            textAlign = if (item.name.isEmpty()) TextAlign.Center else TextAlign.Start
                        )
                        Text(
                            item.description!!.ifEmpty { "---" },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.6f),
                            textAlign = if (item.description.isEmpty()) TextAlign.Center else TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}