package io.github.luposolitario.immundanoctis

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import io.github.luposolitario.immundanoctis.data.ChatMessage
import io.github.luposolitario.immundanoctis.data.CharacterStats
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.HeroDetails
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.view.CharacterID
import io.github.luposolitario.immundanoctis.view.ChatViewModel
import io.github.luposolitario.immundanoctis.view.GameUiState

/**
 * L'Activity principale, il punto d'ingresso della nostra applicazione.
 */
class AdventureActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImmundaNoctisTheme {
                val uiState by chatViewModel.uiState.collectAsState()

                GameScreen(
                    uiState = uiState,
                    onMessageSent = { messageText ->
                        chatViewModel.addMessage(messageText, CharacterID.HERO)
                    }
                )
            }
        }
    }
}

/**
 * Il Composable che rappresenta l'intera schermata di gioco.
 */
@Composable
fun GameScreen(uiState: GameUiState, onMessageSent: (String) -> Unit) {
    val context = LocalContext.current
    val hero = uiState.characters[CharacterID.HERO]

    Column(modifier = Modifier.fillMaxSize()) {
        HeaderSection(
            characters = uiState.characters.values.toList(),
            speakingCharacterId = uiState.speakingCharacterId,
            onCharacterClick = { characterId ->
                Toast.makeText(context, "Hai cliccato su ${uiState.characters[characterId]?.name}", Toast.LENGTH_SHORT).show()
            }
        )

        if (hero != null) {
            HeroStatsSection(hero = hero)
            HeroDetailsSection(hero = hero)
        }

        Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        // Riquadro per la chat, per separarla visivamente.
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .padding(bottom = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                reverseLayout = true
            ) {
                items(uiState.messages.reversed()) { message ->
                    MessageBubble(
                        message = message,
                        character = uiState.characters[message.authorId]
                    )
                }
            }
        }

        MessageInput(onMessageSent = onMessageSent)
    }
}

/**
 * La sezione superiore (header), con la mappa e i ritratti dei personaggi.
 */
@Composable
fun HeaderSection(
    characters: List<GameCharacter>,
    speakingCharacterId: String?,
    onCharacterClick: (String) -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        val (dm, mage, cleric, barbarian, map) = createRefs()
        val dmChar = characters.find { it.id == CharacterID.DM }
        val mageChar = characters.find { it.id == CharacterID.MAGE }
        val clericChar = characters.find { it.id == CharacterID.CLERIC }
        val barbChar = characters.find { it.id == CharacterID.BARBARIAN }

        Image(
            painter = painterResource(id = R.drawable.map_dungeon),
            contentDescription = "Mappa del Dungeon",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .constrainAs(map) {
                    top.linkTo(parent.top)
                    centerHorizontallyTo(parent)
                }
        )

        if (dmChar != null) {
            CharacterPortrait(
                character = dmChar,
                isSpeaking = dmChar.id == speakingCharacterId,
                onClick = { onCharacterClick(dmChar.id) },
                size = 72.dp,
                modifier = Modifier.constrainAs(dm) {
                    top.linkTo(map.bottom, margin = 8.dp)
                }
            )
        }
        if (mageChar != null) {
            CharacterPortrait(
                character = mageChar,
                isSpeaking = mageChar.id == speakingCharacterId,
                onClick = { onCharacterClick(mageChar.id) },
                size = 60.dp,
                modifier = Modifier.constrainAs(mage) {
                    top.linkTo(map.bottom, margin = 8.dp)
                }
            )
        }
        if (clericChar != null) {
            CharacterPortrait(
                character = clericChar,
                isSpeaking = clericChar.id == speakingCharacterId,
                onClick = { onCharacterClick(clericChar.id) },
                size = 60.dp,
                modifier = Modifier.constrainAs(cleric) {
                    top.linkTo(map.bottom, margin = 8.dp)
                }
            )
        }
        if (barbChar != null) {
            CharacterPortrait(
                character = barbChar,
                isSpeaking = barbChar.id == speakingCharacterId,
                onClick = { onCharacterClick(barbChar.id) },
                size = 60.dp,
                modifier = Modifier.constrainAs(barbarian) {
                    top.linkTo(map.bottom, margin = 8.dp)
                }
            )
        }

        createHorizontalChain(dm, mage, cleric, barbarian, chainStyle = ChainStyle.Spread)
    }
}

/**
 * Mostra i riquadri con le statistiche principali dell'eroe.
 */
@Composable
fun HeroStatsSection(hero: GameCharacter) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        hero.stats?.let { stats ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatBox(label = "PV", value = "${stats.currentHp} / ${stats.maxHp}")
                StatBox(label = "CA", value = stats.armorClass.toString())
                StatBox(label = "FOR", value = stats.strength.toString())
                StatBox(label = "DES", value = stats.dexterity.toString())
                StatBox(label = "COS", value = stats.constitution.toString())
                StatBox(label = "INT", value = stats.intelligence.toString())
                StatBox(label = "SAG", value = stats.wisdom.toString())
                StatBox(label = "CAR", value = stats.charisma.toString())
            }
        }
    }
}

/**
 * Mostra i dettagli dell'eroe come classe, abilità ed equipaggiamento.
 */
@Composable
fun HeroDetailsSection(hero: GameCharacter) {
    hero.details?.let { details ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Classe: ${hero.characterClass}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Abilità: ${details.specialAbilities.joinToString()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                val equipmentText = listOfNotNull(
                    details.equippedWeapon,
                    details.equippedArmor,
                    details.equippedShield
                ).joinToString(" • ")
                Text(
                    text = "Equip: $equipmentText",
                    style = MaterialTheme.typography.bodyMedium
                )
                val coinsText = details.coins.map { "${it.value} ${it.key}" }.joinToString()
                Text(
                    text = "Monete: $coinsText",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}


/**
 * Un piccolo riquadro per mostrare un singolo parametro.
 */
@Composable
fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/**
 * Un singolo ritratto cliccabile e di dimensione variabile.
 */
@Composable
fun CharacterPortrait(
    character: GameCharacter,
    isSpeaking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val borderColor = if (isSpeaking) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = character.portraitResId),
            contentDescription = "Ritratto di ${character.name}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(4.dp, borderColor, CircleShape)
        )
        Text(text = character.name, fontSize = 12.sp)
    }
}

/**
 * La "bolla" che contiene un singolo messaggio nella chat.
 */
@Composable
fun MessageBubble(message: ChatMessage, character: GameCharacter?) {
    val isHero = message.authorId == CharacterID.HERO
    val alignment = if (isHero) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = when {
        isHero -> MaterialTheme.colorScheme.primary
        character != null -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        isHero -> MaterialTheme.colorScheme.onPrimary
        character != null -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Column(horizontalAlignment = if (isHero) Alignment.End else Alignment.Start) {
            if (!isHero) {
                Text(
                    text = character?.name ?: "Narratore",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = bubbleColor)
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(12.dp),
                    color = textColor,
                    textAlign = if (isHero) TextAlign.End else TextAlign.Start
                )
            }
        }
    }
}

/**
 * L'area di input del testo con il pulsante di invio.
 */
@Composable
fun MessageInput(onMessageSent: (String) -> Unit) {
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    OutlinedTextField(
        value = textState,
        onValueChange = { textState = it },
        placeholder = { Text("Cosa fai?") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        trailingIcon = {
            IconButton(onClick = {
                if (textState.text.isNotBlank()) {
                    onMessageSent(textState.text)
                    textState = TextFieldValue("")
                }
            }) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_send),
                    contentDescription = "Invia messaggio"
                )
            }
        }
    )
}

/**
 * L'anteprima per visualizzare la schermata di gioco direttamente in Android Studio.
 */
@Preview(showBackground = true, name = "Schermata di Gioco")
@Composable
fun GameScreenPreview() {
    val previewHeroStats = CharacterStats(
        currentHp = 20, maxHp = 20, armorClass = 14,
        strength = 16, dexterity = 12, constitution = 14,
        intelligence = 10, wisdom = 11, charisma = 8
    )
    val previewHeroDetails = HeroDetails(
        specialAbilities = listOf("Attacco Poderoso", "Ira Funesta"),
        equippedWeapon = "Spadone a due mani",
        equippedArmor = "Armatura di cuoio",
        equippedShield = null,
        coins = mapOf("GP" to 15, "SP" to 22)
    )
    val previewCharacters = mapOf(
        CharacterID.HERO to GameCharacter("hero", "Tu", "Barbaro", android.R.drawable.ic_menu_view, previewHeroStats, previewHeroDetails),
        CharacterID.DM to GameCharacter("dm", "Master", "Dungeon Master", R.drawable.portrait_dm),
        CharacterID.MAGE to GameCharacter("mage", "Elara", "Maga", R.drawable.portrait_mage),
        CharacterID.CLERIC to GameCharacter("cleric", "Kael", "Chierico", R.drawable.portrait_cleric),
        CharacterID.BARBARIAN to GameCharacter("barbarian", "Grog", "Barbaro", R.drawable.portrait_barbarian)
    )
    val previewMessages = listOf(
        ChatMessage(
            authorId = CharacterID.DM,
            text = "L'aria è gelida. Davanti a te, una porta di ferro arrugginita socchiusa."
        ),
        ChatMessage(
            authorId = CharacterID.HERO,
            text = "Ispeziono la porta in cerca di trappole."
        ),
        ChatMessage(
            authorId = CharacterID.MAGE,
            text = "Aspetta! Percepisco una debole aura magica provenire da essa."
        )
    )
    val previewState = GameUiState(
        characters = previewCharacters,
        messages = previewMessages,
        speakingCharacterId = CharacterID.MAGE
    )

    ImmundaNoctisTheme {
        Surface {
            GameScreen(
                uiState = previewState,
                onMessageSent = {}
            )
        }
    }
}
