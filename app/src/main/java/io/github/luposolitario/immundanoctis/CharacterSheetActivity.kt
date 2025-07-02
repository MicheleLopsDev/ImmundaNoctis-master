// File: app/src/main/java/io/github/luposolitario/immundanoctis/CharacterSheetActivity.kt

package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable // <--- AGGIUNGI QUESTA RIGA!
import androidx.compose.material.icons.filled.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
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
import io.github.luposolitario.immundanoctis.data.KaiDisciplineInfo
import io.github.luposolitario.immundanoctis.data.FISTS_WEAPON // Importa FISTS_WEAPON
import io.github.luposolitario.immundanoctis.view.CharacterSheetViewModel
import io.github.luposolitario.immundanoctis.view.CharacterSheetUiState
import io.github.luposolitario.immundanoctis.R
import io.github.luposolitario.immundanoctis.ui.adventure.getIconForDiscipline

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
                    // Passiamo il ViewModel alla Screen per accedere a tutto lo stato
                    CharacterSheetScreen(viewModel = viewModel)
                }
            }
        }
    }
}

// --- CHARACTER SHEET SCREEN MODIFICATA ---
@Composable
fun CharacterSheetScreen(viewModel: CharacterSheetViewModel) { // Accetta il ViewModel
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

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
                    painter = painterResource(id = R.drawable.ic_hero_portrait_placeholder),
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

        StatsAndMealsCard(
            combatSkill = uiState.combatSkill,
            endurance = uiState.endurance,
            meals = uiState.mealsCount
        )

        // Passiamo direttamente la lista di 2 armi (o pugni) visibili
        WeaponsCard(
            visibleWeapons = uiState.visibleWeapons,
            selectedWeapon = uiState.selectedWeapon,
            onWeaponSelected = { weapon -> viewModel.selectWeapon(weapon) }
        )

        KaiDisciplinesCard(
            kaiDisciplines = uiState.kaiDisciplines
        )

        CommonItemsCard(
            commonItems = uiState.backpackItems
        )

        SpecialItemsTableCard(
            specialItems = uiState.specialItems
        )
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


@Composable
fun CommonItemSlot(item: GameItem) {
    val isEmpty = item.name.isEmpty() || item.quantity == 0
    // L'errore è qui: iconRes può essere un Any, ma painterResource vuole Int
    val iconRes: Any = if (isEmpty) R.drawable.ic_unknown_item else item.iconResId ?: R.drawable.ic_unknown_item

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

// --- WEAPONS CARD MODIFICATA: USA visibleWeapons ---
@Composable
fun WeaponsCard(
    visibleWeapons: List<GameItem>, // Riceve la lista di 2 armi/pugni
    selectedWeapon: GameItem,
    onWeaponSelected: (GameItem?) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
            Text("Armi", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))

            Row( // Usiamo una Row per affiancare gli slot arma e pugni
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Visualizza le 2 armi/pugni dalla lista visibleWeapons
                visibleWeapons.forEach { weapon ->
                    WeaponSlot(
                        weapon = weapon,
                        isSelected = weapon.id == selectedWeapon.id,
                        onClick = onWeaponSelected
                    )
                }
                // Lo slot per i pugni, se non è già presente in visibleWeapons
                // Questa parte è opzionale se i pugni sono SEMPRE garantiti in visibleWeapons
                // Se la tua logica prevede che visibleWeapons sia SOLO armi reali (max 2)
                // e i pugni siano un'opzione SEMPRE a parte, riabilitiamo un terzo slot.
                // Per la tua richiesta "2 armi all'inizio conterrano un arma e i pugni"
                // e "non è mai vuota anche se perdi 2 armi rimani con 2 pugni",
                // la lista visibleWeapons dovrebbe già contenere i pugni se non ci sono armi.
                // Quindi, iteriamo solo su visibleWeapons che ha sempre 2 elementi.
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Arma selezionata: ${selectedWeapon.name} (CS: ${selectedWeapon.bonuses?.get("CombatSkill") ?: 0})",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

// --- WEAPON SLOT MODIFICATO ---
@Composable
fun WeaponSlot(
    weapon: GameItem?,
    isSelected: Boolean,
    onClick: (GameItem?) -> Unit
) {
    val borderColor = if (isSelected) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline
    val backgroundColor = if (isSelected) Color(0xFFFFD700).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant

    OutlinedCard(
        modifier = Modifier
            .width(100.dp) // Larghezza fissa dello slot
            .height(120.dp) // Altezza fissa dello slot
            .padding(4.dp)
            .clickable { onClick(weapon) }, // Rende cliccabile
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (weapon == null || weapon.name.isEmpty()) { // Se lo slot è vuoto
                Image(
                    painter = painterResource(id = R.drawable.ic_unknown_item), // Icona per slot vuoto
                    contentDescription = "Slot Arma Vuoto",
                    modifier = Modifier.size(48.dp)
                )
                Text("Vuoto", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            } else {
                val weaponIconRes = when (weapon.id) { // Usiamo l'ID dell'arma per le icone specifiche
                    "FISTS" -> R.drawable.ic_fists
                    "Ascia" -> R.drawable.ic_axe
                    "Spada" -> R.drawable.ic_sword
                    "Mazza" -> R.drawable.ic_mace
                    "Bastone" -> R.drawable.ic_staff
                    "Lancia" -> R.drawable.ic_spear
                    "Spada Larga" -> R.drawable.ic_broadsword
                    else -> R.drawable.ic_unknown_item // Fallback
                }
                Image(
                    painter = painterResource(id = weaponIconRes),
                    contentDescription = weapon.name,
                    modifier = Modifier.size(48.dp)
                )
                Text(weapon.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                // Mostra il bonus di combattività dall'oggetto
                val combatSkillBonus = weapon.bonuses?.get("CombatSkill") ?: 0
                if (combatSkillBonus != 0) {
                    Text(
                        text = "CS: +$combatSkillBonus",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
