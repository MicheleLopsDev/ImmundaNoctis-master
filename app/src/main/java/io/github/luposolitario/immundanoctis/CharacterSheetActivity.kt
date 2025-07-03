// immundanoctis/CharacterSheetActivity.kt

package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable // Importa combinedClickable
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
import androidx.compose.runtime.*
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
import io.github.luposolitario.immundanoctis.data.FISTS_WEAPON
import io.github.luposolitario.immundanoctis.view.CharacterSheetViewModel
import io.github.luposolitario.immundanoctis.view.CharacterSheetUiState
import io.github.luposolitario.immundanoctis.R
import io.github.luposolitario.immundanoctis.ui.adventure.getIconForDiscipline
import androidx.compose.foundation.ExperimentalFoundationApi // Importa ExperimentalFoundationApi


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
                    CharacterSheetScreen(viewModel = viewModel)
                }
            }
        }
    }
}

// --- CHARACTER SHEET SCREEN MODIFICATA ---
@OptIn(ExperimentalMaterial3Api::class) // Per AlertDialog, se non già presente nel tuo tema
@Composable
fun CharacterSheetScreen(viewModel: CharacterSheetViewModel) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    // Stato per il pop-up di conferma scarto
    var showDiscardConfirmDialog by remember { mutableStateOf<GameItem?>(null) }

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

        WeaponsCard(
            visibleWeapons = uiState.visibleWeapons,
            selectedWeapon = uiState.selectedWeapon,
            onWeaponSelected = { weapon -> viewModel.selectWeapon(weapon) },
            onWeaponLongPress = { weapon -> showDiscardConfirmDialog = weapon }
        )

        KaiDisciplinesCard(
            kaiDisciplines = uiState.kaiDisciplines
        )

        CommonItemsCard(
            commonItems = uiState.backpackItems,
            onItemClick = { item -> viewModel.useBackpackItem(item) },
            onItemLongPress = { item -> showDiscardConfirmDialog = item }
        )

        SpecialItemsTableCard(
            specialItems = uiState.specialItems
        )
    }

    // Dialogo di conferma per lo scarto
    showDiscardConfirmDialog?.let { itemToDiscard ->
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = null },
            title = { Text("Conferma Scarto Oggetto") },
            text = { Text("Sei sicuro di voler scartare '${itemToDiscard.name}'? Quest'azione non è reversibile.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.discardItem(itemToDiscard)
                        showDiscardConfirmDialog = null
                    }
                ) {
                    Text("SCARTA")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmDialog = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}

// MODIFICATO: Aggiunto onWeaponLongPress
@Composable
fun WeaponsCard(
    visibleWeapons: List<GameItem>,
    selectedWeapon: GameItem,
    onWeaponSelected: (GameItem?) -> Unit,
    onWeaponLongPress: (GameItem) -> Unit // NUOVO PARAMETRO
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
            Text("Armi", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                visibleWeapons.forEach { weapon ->
                    WeaponSlot(
                        weapon = weapon,
                        isSelected = weapon.id == selectedWeapon.id,
                        onClick = onWeaponSelected,
                        onLongPress = onWeaponLongPress // MODIFICA: Passato onLongPress
                    )
                }
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

// MODIFICATO: Aggiunto onLongPress e logica combinedClickable
@OptIn(ExperimentalFoundationApi::class) // <--- AGGIUNTA ANNOTAZIONE
@Composable
fun WeaponSlot(
    weapon: GameItem?,
    isSelected: Boolean,
    onClick: (GameItem?) -> Unit,
    onLongPress: (GameItem) -> Unit // NUOVO PARAMETRO
) {
    val borderColor = if (isSelected) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline
    val backgroundColor = if (isSelected) Color(0xFFFFD700).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant

    val canBeNormallyClicked = weapon != null
    val canBeLongPressed = weapon != null && weapon.isDiscardable

    OutlinedCard(
        modifier = Modifier
            .width(100.dp)
            .height(120.dp)
            .padding(4.dp)
            .combinedClickable( // <--- MODIFICA: Usato combinedClickable
                onClick = { if (canBeNormallyClicked) onClick(weapon) },
                onLongClick = {
                    if (canBeLongPressed) onLongPress(weapon!!) // Assicurati che weapon non sia nullo
                }
            ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (weapon == null || weapon.name.isEmpty()) {
                Image(
                    painter = painterResource(id = R.drawable.ic_unknown_item),
                    contentDescription = "Slot Arma Vuoto",
                    modifier = Modifier.size(48.dp)
                )
                Text("Vuoto", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            } else {
                val weaponIconRes = when (weapon.id) {
                    "FISTS" -> R.drawable.ic_fists
                    "Ascia" -> R.drawable.ic_axe
                    "Spada" -> R.drawable.ic_sword
                    "Mazza" -> R.drawable.ic_mace
                    "Bastone" -> R.drawable.ic_staff
                    "Lancia" -> R.drawable.ic_spear
                    "Spada Larga" -> R.drawable.ic_broadsword
                    else -> R.drawable.ic_unknown_item
                }
                Image(
                    painter = painterResource(id = weaponIconRes),
                    contentDescription = weapon.name,
                    modifier = Modifier.size(48.dp)
                )
                Text(weapon.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
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


// MODIFICATO: Aggiunto onItemLongPress
@Composable
fun CommonItemsCard(
    commonItems: List<GameItem>,
    onItemClick: (GameItem) -> Unit,
    onItemLongPress: (GameItem) -> Unit // NUOVO PARAMETRO
) {
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
                    CommonItemSlot(
                        item = item,
                        onClick = onItemClick,
                        onLongPress = onItemLongPress // MODIFICA: Passato onLongPress
                    )
                }
            }
        }
    }
}

// MODIFICATO: Aggiunto onLongPress e logica combinedClickable
@OptIn(ExperimentalFoundationApi::class) // <--- AGGIUNTA ANNOTAZIONE
@Composable
fun CommonItemSlot(
    item: GameItem,
    onClick: (GameItem) -> Unit,
    onLongPress: (GameItem) -> Unit // NUOVO PARAMETRO
) {
    val isEmpty = item.name.isEmpty() || item.quantity == 0
    val canBeNormallyClicked = !isEmpty && item.isConsumable
    val canBeLongPressed = !isEmpty && item.isDiscardable // <--- Controllo su isDiscardable

    val iconRes: Any = if (isEmpty) R.drawable.ic_unknown_item else item.iconResId ?: R.drawable.ic_unknown_item

    OutlinedCard(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxSize()
            .combinedClickable( // <--- MODIFICA: Usato combinedClickable
                onClick = { if (canBeNormallyClicked) onClick(item) },
                onLongClick = {
                    if (canBeLongPressed) onLongPress(item)
                }
            ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
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
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Statistiche e Pasti", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
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
fun KaiDisciplinesCard(kaiDisciplines: List<KaiDisciplineInfo>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Abilità Kai", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                kaiDisciplines.forEach { discipline ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val disciplineIconRes = getIconForDiscipline(discipline.id)
                            Icon(
                                imageVector = disciplineIconRes,
                                contentDescription = discipline.name,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(discipline.name, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(4.dp))
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