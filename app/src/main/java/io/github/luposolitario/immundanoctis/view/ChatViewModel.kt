package io.github.luposolitario.immundanoctis.view

import androidx.lifecycle.ViewModel
import io.github.luposolitario.immundanoctis.R
import io.github.luposolitario.immundanoctis.data.ChatMessage
import io.github.luposolitario.immundanoctis.data.CharacterStats
import io.github.luposolitario.immundanoctis.data.GameCharacter
import io.github.luposolitario.immundanoctis.data.HeroDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Un "object" per contenere gli ID statici e univoci dei personaggi.
 */
object CharacterID {
    const val HERO = "hero"
    const val DM = "dm"
    const val MAGE = "mage"
    const val CLERIC = "cleric"
    const val BARBARIAN = "barbarian"
}

/**
 * Rappresenta l'intero stato della nostra schermata di gioco in un dato momento.
 */
data class GameUiState(
    val messages: List<ChatMessage> = emptyList(),
    val characters: Map<String, GameCharacter> = emptyMap(),
    val speakingCharacterId: String? = null
)

/**
 * Il ViewModel che gestisce la logica e lo stato di "Immunda Noctis".
 */
class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        initializeGame()
    }

    /**
     * Prepara la partita: crea tutti i personaggi, incluse le statistiche
     * e i dettagli dell'eroe, e il primo messaggio del Master.
     */
    private fun initializeGame() {
        // Creiamo le statistiche iniziali per l'eroe.
        val heroStats = CharacterStats(
            currentHp = 20, maxHp = 20, armorClass = 14,
            strength = 16, dexterity = 12, constitution = 14,
            intelligence = 10, wisdom = 11, charisma = 8
        )

        // Creiamo i dettagli (abilità, equip) per l'eroe.
        val heroDetails = HeroDetails(
            specialAbilities = listOf("Attacco Poderoso", "Ira Funesta"),
            equippedWeapon = "Spadone a due mani",
            equippedArmor = "Armatura di cuoio",
            equippedShield = null, // Niente scudo
            coins = mapOf("GP" to 15, "SP" to 22)
        )

        // Creiamo la mappa dei personaggi che parteciperanno al gioco.
        val characterMap = mapOf(
            // Ora l'eroe è un personaggio completo di stats e dettagli.
            CharacterID.HERO to GameCharacter(
                id = CharacterID.HERO, name = "Tu", characterClass = "Barbaro",
                portraitResId = android.R.drawable.ic_menu_view, // Placeholder, non visibile
                stats = heroStats,
                details = heroDetails
            ),
            CharacterID.DM to GameCharacter(
                id = CharacterID.DM,
                name = "Master",
                characterClass = "Dungeon Master",
                portraitResId = R.drawable.portrait_dm
            ),
            CharacterID.MAGE to GameCharacter(
                id = CharacterID.MAGE,
                name = "Elara",
                characterClass = "Maga",
                portraitResId = R.drawable.portrait_mage
            ),
            CharacterID.CLERIC to GameCharacter(
                id = CharacterID.CLERIC,
                name = "Kael",
                characterClass = "Chierico",
                portraitResId = R.drawable.portrait_cleric
            ),
            CharacterID.BARBARIAN to GameCharacter(
                id = CharacterID.BARBARIAN,
                name = "Grog",
                characterClass = "Barbaro",
                portraitResId = R.drawable.portrait_barbarian
            )
        )

        // Creiamo il messaggio di apertura del Master.
        val firstMessage = ChatMessage(
            authorId = CharacterID.DM,
            text = "L'aria è gelida. Un odore di terra umida e putrefazione ti riempie le narici. Davanti a te, una porta di ferro arrugginita socchiusa."
        )

        // Impostiamo lo stato iniziale del gioco.
        _uiState.value = GameUiState(
            characters = characterMap,
            messages = listOf(firstMessage),
            speakingCharacterId = CharacterID.DM
        )
    }

    /**
     * Aggiunge un nuovo messaggio alla chat e aggiorna lo stato.
     */
    fun addMessage(text: String, authorId: String) {
        val newMessage = ChatMessage(authorId = authorId, text = text)

        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + newMessage,
                speakingCharacterId = authorId
            )
        }

        // --- PARTE FUTURA ---
        // Se a inviare il messaggio è l'eroe, qui attiveremo la logica
        // dell'IA locale per generare una risposta.
    }
}