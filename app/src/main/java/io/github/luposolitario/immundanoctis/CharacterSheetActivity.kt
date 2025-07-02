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
import androidx.compose.material.icons.filled.Info // Rimosso se non più usato per pasti +/-
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
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.data.GameItem // Necessario per i tipi
import io.github.luposolitario.immundanoctis.data.ItemType // Necessario per i tipi
import io.github.luposolitario.immundanoctis.data.LoneWolfStats // Necessario per i tipi


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
                    CharacterSheetScreenMock()
                }
            }
        }
    }
}

@Composable
fun CharacterSheetScreenMock() {
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
                // Placeholder per il ritratto - Icona aggiornata
                Image(
                    painter = painterResource(id = R.drawable.portrait_hero_male), // Icona specifica per il ritratto
                    contentDescription = "Ritratto Eroe",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text("Lupo Solitario", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Cavaliere Kai", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
            // ORO - Icona aggiornata
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.5f)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_gold), // Icona specifica per l'oro
                    contentDescription = "Oro",
                    modifier = Modifier.size(48.dp)
                )
                Text("Oro", style = MaterialTheme.typography.titleMedium)
                Text("15", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) // Mock data per oro
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 1. Card "Stats e Pasti" con pulsanti Note e Mappa
        StatsAndMealsCardMock(
            combatSkill = 25, // Mock data
            endurance = 30, // Mock data
            meals = 2 // Mock data
        )

        // 2. Card "Armi"
        WeaponsCardMock(
            primaryWeaponName = "Spada Suprema", // Arma attiva
            secondaryWeaponName = "Ascia da Battaglia" // Seconda arma (o null se vuota)
        )

        // 3. Card "Oggetti Comuni" (Zaino)
        CommonItemsCardMock()

        // 4. Card "Oggetti Speciali" (Tabella)
        SpecialItemsTableCardMock()
    }
}

@Composable
fun StatsAndMealsCardMock(combatSkill: Int, endurance: Int, meals: Int) {
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
                    // Rimosso i pulsanti (+) e (-) per i pasti
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painter = painterResource(id = R.drawable.ic_meal), contentDescription = "Pasti", modifier = Modifier.size(32.dp)) // Icona pasti
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
                // Pulsante Note - Icona aggiornata
                Button(onClick = { /* Mock: Open Notes */ }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painter = painterResource(id = R.drawable.ic_unknown_item), contentDescription = "Note", modifier = Modifier.size(24.dp)) // Icona specifica
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
fun WeaponsCardMock(primaryWeaponName: String, secondaryWeaponName: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) { // Riduci padding per compattezza
            Text("Armi", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp)) // Riduci altezza per compattezza
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primo Slot Arma (l'arma primaria/attiva) - Icona aggiornata
                WeaponSlotMock(weaponName = primaryWeaponName, isActive = true)

                // Secondo Slot Arma - Icona aggiornata
                WeaponSlotMock(weaponName = secondaryWeaponName ?: "Slot Arma", isActive = false, isEmpty = secondaryWeaponName == null)
            }
        }
    }
}

@Composable
fun WeaponSlotMock(weaponName: String, isActive: Boolean, isEmpty: Boolean = false) {
    val borderColor = if (isActive) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline
    val backgroundColor = if (isActive) Color(0xFFFFD700).copy(alpha = 0.2f) else Color.Transparent

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
                Icon(
                    imageVector = Icons.Default.Info, // Placeholder per slot vuoto, o ic_empty_slot
                    contentDescription = "Slot Arma Vuoto",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Slot Arma", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_sword), // Icona specifica per armi
                    contentDescription = weaponName,
                    modifier = Modifier.size(48.dp)
                )
                Text(weaponName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun CommonItemsCardMock() {
    val commonItems = listOf(
        GameItem(name = "Pozione Curativa", type = ItemType.BACKPACK_ITEM, quantity = 1, iconResId = R.drawable.ic_potion), // Icona specifica
        GameItem(name = "Pasto", type = ItemType.BACKPACK_ITEM, quantity = 2, iconResId = R.drawable.ic_meal), // Icona specifica
        GameItem(name = "Fune", type = ItemType.BACKPACK_ITEM, quantity = 1, iconResId = R.drawable.ic_unknown_item),
        GameItem(name = "Torcia", type = ItemType.BACKPACK_ITEM, quantity = 1, iconResId = R.drawable.ic_unknown_item),
        // Riempiamo gli slot vuoti per un totale di 8
        GameItem(name = "", type = ItemType.BACKPACK_ITEM, quantity = 0, iconResId = R.drawable.ic_unknown_item), // Icona specifica per slot vuoto
        GameItem(name = "", type = ItemType.BACKPACK_ITEM, quantity = 0, iconResId = R.drawable.ic_unknown_item),
        GameItem(name = "", type = ItemType.BACKPACK_ITEM, quantity = 0, iconResId = R.drawable.ic_unknown_item),
        GameItem(name = "", type = ItemType.BACKPACK_ITEM, quantity = 0, iconResId = R.drawable.ic_unknown_item)
    )

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
                items(commonItems) { item ->
                    CommonItemSlotMock(item = item)
                }
            }
        }
    }
}

@Composable
fun CommonItemSlotMock(item: GameItem) {
    val isEmpty = item.name.isEmpty() || item.quantity == 0
    // L'icona dello slot vuoto è ora R.drawable.ic_empty_slot
    val iconRes = if (isEmpty) R.drawable.ic_unknown_item else item.iconResId ?: R.drawable.ic_unknown_item

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
fun SpecialItemsTableCardMock() {
    val specialItems = listOf(
        GameItem(name = "Mappa", description = "Rivela la tua posizione nel mondo di gioco.", type = ItemType.SPECIAL_ITEM),
        GameItem(name = "Elmo", description = "Aggiunge 2 punti RESISTENZA al tuo totale.", type = ItemType.HELMET),
        GameItem(name = "Gilet di maglia di ferro", description = "Aggiunge 4 punti RESISTENZA al tuo totale.", type = ItemType.ARMOR),
        GameItem(name = "Amuleto di Protezione", description = "Un oggetto magico che fornisce una piccola protezione contro la magia oscura.", type = ItemType.SPECIAL_ITEM),
        GameItem(name = "Chiave Misteriosa", description = "Una chiave antica e arrugginita, chissà cosa apre...", type = ItemType.SPECIAL_ITEM)
    )
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


@Preview(showBackground = true)
@Composable
fun CharacterSheetScreenMockPreview() {
    ImmundaNoctisTheme {
        CharacterSheetScreenMock()
    }
}