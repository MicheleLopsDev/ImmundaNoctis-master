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
import io.github.luposolitario.immundanoctis.ui.adventure.getIconForDiscipline
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info // Rimosso se non più usato
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Help
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.data.GameItem
import io.github.luposolitario.immundanoctis.data.ItemType
import io.github.luposolitario.immundanoctis.data.LoneWolfStats
// Import per le discipline Kai (KAI_DISCIPLINES non è più KaiDiscipline, ma KaiDisciplineInfo)
import io.github.luposolitario.immundanoctis.data.KaiDisciplineInfo
import io.github.luposolitario.immundanoctis.view.CharacterSheetViewModel
import io.github.luposolitario.immundanoctis.view.CharacterSheetUiState
// Import delle icone delle discipline (saranno necessarie se non le hai già)
import io.github.luposolitario.immundanoctis.R // Per R.drawable

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
                    val viewModel: CharacterSheetViewModel = viewModel()
                    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                    CharacterSheetScreen(uiState = uiState)
                }
            }
        }
    }
}

@Composable
fun CharacterSheetScreen(uiState: CharacterSheetUiState) {
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
                Image(
                    painter = painterResource(id = R.drawable.lupo_solitario),
                    contentDescription = "Ritratto Eroe",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text(uiState.heroCharacter?.name ?: "Eroe Sconosciuto", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(uiState.kaiRank, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.5f)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_gold),
                    contentDescription = "Oro",
                    modifier = Modifier.size(48.dp)
                )
                Text("Oro", style = MaterialTheme.typography.titleMedium)
                Text("${uiState.goldCount}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 1. Card "Statistiche e Pasti"
        StatsAndMealsCard(
            combatSkill = uiState.combatSkill,
            endurance = uiState.endurance,
            meals = uiState.mealsCount
        )

        // 2. Card "Armi"
        WeaponsCard(
            primaryWeapon = uiState.equippedWeapon,
            secondaryWeapons = uiState.otherWeapons
        )

        // NUOVA CARD: Abilità Kai - Ora riceve List<KaiDisciplineInfo>
        KaiDisciplinesCard(
            kaiDisciplines = uiState.kaiDisciplines // Qui passiamo la lista di KaiDisciplineInfo
        )

        // 3. Card "Oggetti Comuni" (Zaino)
        CommonItemsCard(
            commonItems = uiState.backpackItems
        )

        // 4. Card "Oggetti Speciali" (Tabella)
        SpecialItemsTableCard(
            specialItems = uiState.specialItems
        )
    }
}

@Composable
fun StatsAndMealsCard(combatSkill: Int, endurance: Int, meals: Int) {
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
                        Image(painter = painterResource(id = R.drawable.ic_meal), contentDescription = "Pasti", modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("$meals", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = { /* Mock: Open Notes */ }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painter = painterResource(id = R.drawable.ic_map_icon), contentDescription = "Note", modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Note", fontSize = 14.sp)
                    }
                }
                Button(onClick = { /* Mock: Open Map */ }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painter = painterResource(id = R.drawable.ic_map), contentDescription = "Mappa", modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mappa", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun WeaponsCard(primaryWeapon: GameItem?, secondaryWeapons: List<GameItem>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
            Text("Armi", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WeaponSlot(weapon = primaryWeapon, isActive = true)
                val secondaryWeapon = secondaryWeapons.firstOrNull()
                WeaponSlot(weapon = secondaryWeapon, isActive = false)
            }
        }
    }
}

@Composable
fun WeaponSlot(weapon: GameItem?, isActive: Boolean) {
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
                    painter = painterResource(id = R.drawable.ic_map_icon), // Usiamo ic_empty_slot
                    contentDescription = "Slot Arma Vuoto",
                    modifier = Modifier.size(48.dp),
                )
                Text("Slot Arma", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
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
fun KaiDisciplinesCard(kaiDisciplines: List<KaiDisciplineInfo>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Abilità Kai", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))

            // Non più LazyVerticalGrid, ma una semplice Column scrollabile se necessario
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 400.dp) // Altezza flessibile, ma con limiti
                    .verticalScroll(rememberScrollState()), // Aggiungiamo lo scroll qui se la lista è lunga
                verticalArrangement = Arrangement.spacedBy(12.dp) // Spazio tra le discipline
            ) {
                kaiDisciplines.forEach { discipline ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start // Allinea a sinistra
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val disciplineIconRes = getIconForDiscipline(discipline.id)
                            Icon(
                                imageVector = disciplineIconRes,
                                contentDescription = discipline.name,
                                modifier = Modifier.size(28.dp), // Icona leggermente più grande
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(discipline.name, style = MaterialTheme.typography.titleMedium) // Nome un po' più grande
                        }
                        Spacer(Modifier.height(4.dp))
                        // Descrizione dell'abilità
                        Text(
                            discipline.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommonItemsCard(commonItems: List<GameItem>) {
    val paddedCommonItems = commonItems.toMutableList()
    while (paddedCommonItems.size < 8) {
        paddedCommonItems.add(GameItem(name = "", type = ItemType.BACKPACK_ITEM, quantity = 0, iconResId = R.drawable.ic_unknown_item))
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Oggetti Comuni (Zaino)", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().height(200.dp),
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

// File: app/src/main/java/io/github/luposolitario/immundanoctis/CharacterSheetActivity.kt

@Composable
fun CommonItemSlot(item: GameItem) {
    val isEmpty = item.name.isEmpty() || item.quantity == 0
    // L'errore è qui: iconRes può essere un Any, ma painterResource vuole Int
    val iconRes: Any = if (isEmpty) R.drawable.ic_gold else item.iconResId ?: R.drawable.ic_unknown_item

    OutlinedCard(
        modifier = Modifier
            .aspectRatio(1f)
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
                // Correggiamo passando iconRes come Int con un cast sicuro
                painter = painterResource(id = iconRes as Int),
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
fun SpecialItemsTableCard(specialItems: List<GameItem>) {
    val paddedSpecialItems = specialItems.toMutableList()
    while (paddedSpecialItems.size < 10) {
        paddedSpecialItems.add(GameItem(name = "", description = "", type = ItemType.SPECIAL_ITEM))
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Oggetti Speciali", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
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

