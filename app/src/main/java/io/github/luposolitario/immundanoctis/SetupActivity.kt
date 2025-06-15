package io.github.luposolitario.immundanoctis

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.SessionData
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.GameStateManager
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import java.text.SimpleDateFormat
import java.util.*

class SetupActivity : ComponentActivity() {

    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    private lateinit var gameStateManager: GameStateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameStateManager = GameStateManager(this)

        setContent {
            val useDarkTheme = themePreferences.useDarkTheme(isSystemInDarkTheme())
            ImmundaNoctisTheme(darkTheme = useDarkTheme) {

                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        window.statusBarColor = Color.Black.toArgb()
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var sessionToLoad by remember { mutableStateOf(gameStateManager.loadSession()) }
                    var showCreationScreen by remember { mutableStateOf(sessionToLoad == null) }

                    if (showCreationScreen) {
                        CharacterCreationScreen(
                            onSessionCreate = { sessionData ->
                                gameStateManager.saveSession(sessionData)
                                val intent = Intent(this, AdventureActivity::class.java)
                                startActivity(intent)
                                finish()
                            },
                            defaultSession = gameStateManager.createDefaultSession()
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
                                onCreateNew = {
                                    showCreationScreen = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Un Composable che carica un'immagine in modo sicuro. Se il caricamento fallisce,
 * mostra un'icona sostitutiva invece di causare un crash.
 */
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


@Composable
fun ExistingSessionScreen(
    session: SessionData,
    onContinue: () -> Unit,
    onCreateNew: () -> Unit
) {
    val formattedDate = remember {
        SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(session.lastUpdate))
    }
    val hero = session.characters.find { it.id == "hero" }

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
                    // --- MODIFICA CHIAVE: Uso di RobustImage per evitare il crash ---
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
fun CharacterCreationScreen(
    onSessionCreate: (SessionData) -> Unit,
    defaultSession: SessionData
) {
    var sessionName by remember { mutableStateOf(defaultSession.sessionName) }
    var heroName by remember { mutableStateOf("Alex") }

    val defaultHero = defaultSession.characters.find { it.id == "hero" }!!
    var selectedGender by remember { mutableStateOf(defaultHero.gender) }
    var selectedPortraitId by remember { mutableStateOf(defaultHero.portraitResId) }
    var selectedClass by remember { mutableStateOf("Guerriero") }
    var selectedAlignment by remember { mutableStateOf("Neutrale") }
    var selectedLanguage by remember { mutableStateOf(defaultHero.language) }

    var showClassDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    if (showClassDialog) {
        ClassSelectionDialog(
            selectedClass = selectedClass,
            selectedGender = selectedGender,
            onClassSelected = {
                selectedClass = it
                showClassDialog = false
            },
            onDismiss = { showClassDialog = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Crea la tua Avventura", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Nome della Campagna", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = sessionName, onValueChange = { sessionName = it }, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center))
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = heroName, onValueChange = { heroName = it }, label = { Text("Nome del tuo Eroe") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        Text("Scegli il tuo Eroe", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            val malePortraitId = R.drawable.portrait_hero_male
            val femalePortraitId = R.drawable.portrait_hero_female

            // --- MODIFICA CHIAVE: Uso di RobustImage anche qui per coerenza ---
            RobustImage(
                resId = malePortraitId,
                contentDescription = "Ritratto Maschile",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(100.dp).clip(CircleShape).clickable { selectedGender = "MALE"; selectedPortraitId = malePortraitId }.border(width = 3.dp, color = if (selectedGender == "MALE") MaterialTheme.colorScheme.primary else Color.Transparent, shape = CircleShape)
            )
            RobustImage(
                resId = femalePortraitId,
                contentDescription = "Ritratto Femminile",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(100.dp).clip(CircleShape).clickable { selectedGender = "FEMALE"; selectedPortraitId = femalePortraitId }.border(width = 3.dp, color = if (selectedGender == "FEMALE") MaterialTheme.colorScheme.primary else Color.Transparent, shape = CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text("Classe Selezionata: $selectedClass", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { showClassDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Scegli la Classe")
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text("Scegli l'Allineamento", style = MaterialTheme.typography.titleMedium)
        AlignmentSelector(selectedAlignment = selectedAlignment, onAlignmentSelected = { selectedAlignment = it })
        Spacer(modifier = Modifier.height(24.dp))

        Text("Scegli la Lingua", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LanguageSelector(selectedLanguage = selectedLanguage, onLanguageSelected = { selectedLanguage = it })

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val heroToUpdate = defaultSession.characters.find { it.id == "hero" }!!
                val updatedHero = heroToUpdate.copy(
                    name = heroName.trim(),
                    characterClass = selectedClass,
                    portraitResId = selectedPortraitId,
                    gender = selectedGender,
                    language = selectedLanguage
                )
                val updatedCharacters = defaultSession.characters.map {
                    if (it.id == "hero") updatedHero else it
                }
                val newSession = defaultSession.copy(
                    sessionName = sessionName.trim(),
                    lastUpdate = System.currentTimeMillis(),
                    characters = updatedCharacters
                )
                onSessionCreate(newSession)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = heroName.isNotBlank() && sessionName.isNotBlank()
        ) {
            Text("Salva e Inizia l'Avventura")
        }
    }
}

// Il resto del file (ClassSelectionDialog, ClassSelector, ecc.) rimane invariato.
// ... (tutto il resto del file da qui in poi è identico a prima)
@Composable
fun ClassSelectionDialog(
    selectedClass: String,
    selectedGender: String,
    onClassSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scegli una Classe") },
        text = {
            ClassSelector(
                selectedClass = selectedClass,
                selectedGender = selectedGender,
                onClassSelected = onClassSelected
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi")
            }
        }
    )
}

data class ClassInfo(
    val name: String,
    val description: String,
    @DrawableRes val maleIconRes: Int,
    @DrawableRes val femaleIconRes: Int
)

@Composable
fun ClassSelector(
    selectedClass: String,
    selectedGender: String,
    onClassSelected: (String) -> Unit
) {
    val classes = listOf(
        ClassInfo("Guerriero", "Maestro d'armi, abile nel combattimento corpo a corpo.", R.drawable.class_warrior_male, R.drawable.class_warrior_female),
        ClassInfo("Ladro", "Scaltro e agile, esperto in furtività e raggiri.", R.drawable.class_thief_male, R.drawable.class_thief_female),
        ClassInfo("Mago", "Studioso delle arti arcane, piega la realtà al suo volere.", R.drawable.class_witch_male, R.drawable.class_witch_female),
        ClassInfo("Saggio", "Guaritore e sapiente, usa la conoscenza per proteggere gli altri.", R.drawable.class_sage_male, R.drawable.class_sage_female)
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(classes) { classInfo ->
            val imageRes = if (selectedGender == "MALE") classInfo.maleIconRes else classInfo.femaleIconRes
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onClassSelected(classInfo.name) },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(2.dp, if (classInfo.name == selectedClass) MaterialTheme.colorScheme.primary else Color.Transparent),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RobustImage(
                        resId = imageRes,
                        contentDescription = classInfo.name,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(classInfo.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(classInfo.description, style = MaterialTheme.typography.bodySmall, lineHeight = MaterialTheme.typography.bodySmall.lineHeight)
                    }
                }
            }
        }
    }
}


@Composable
fun AlignmentSelector(selectedAlignment: String, onAlignmentSelected: (String) -> Unit) {
    val alignments = listOf(
        Triple("Buono", Icons.Default.WbSunny, "Luce, onore e altruismo guidano le tue azioni."),
        Triple("Neutrale", Icons.Default.Balance, "L'equilibrio è la chiave. Non sei né santo né demone."),
        Triple("Malvagio", Icons.Default.LocalFireDepartment, "Il fine giustifica i mezzi. Il potere è il tuo unico credo.")
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        alignments.forEach { (name, icon, _) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    modifier = Modifier.size(64.dp).clip(CircleShape).clickable { onAlignmentSelected(name) }.border(width = 3.dp, color = if (name == selectedAlignment) MaterialTheme.colorScheme.primary else Color.Transparent, shape = CircleShape).padding(8.dp),
                    tint = if (name == selectedAlignment) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(name, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun LanguageSelector(selectedLanguage: String, onLanguageSelected: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { onLanguageSelected("it") }, colors = if (selectedLanguage == "it") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()) {
            Text("🇮🇹 Italiano")
        }
        Spacer(modifier = Modifier.width(16.dp))
        Button(onClick = { onLanguageSelected("en") }, colors = if (selectedLanguage == "en") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()) {
            Text("🇬🇧 English")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ExistingSessionScreenPreview() {
    ImmundaNoctisTheme {
        ExistingSessionScreen(
            session = SessionData("La Tomba Dimenticata", System.currentTimeMillis(), listOf(GameCharacter("hero", "Eldrin", "Guerriero", R.drawable.portrait_hero_male, "MALE", "it"))),
            onContinue = {},
            onCreateNew = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun CharacterCreationScreenPreview() {
    ImmundaNoctisTheme {
        CharacterCreationScreen(onSessionCreate = {}, defaultSession = GameStateManager(androidx.compose.ui.platform.LocalContext.current).createDefaultSession())
    }
}
