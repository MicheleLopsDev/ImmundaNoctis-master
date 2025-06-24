// app/src/test/java/io/github/luposolitario.immundanoctis/StringTagParserTest.kt

package io.github.luposolitario.immundanoctis

import io.github.luposolitario.immundanoctis.data.CharacterType
import io.github.luposolitario.immundanoctis.util.StringTagParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import android.content.Context
import android.content.res.AssetManager
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.UUID
import org.mockito.Mockito.`when`

class StringTagParserTest {

    // Il JSON di configurazione completo, ora PRIVO DI COMMENTI
    private val testJsonConfig = """
        {
          "tags": [
            {
              "id": "e963b8d1-7c9f-4e0a-b1e7-c1a9d0f4e6b2",
              "type": "text_substitution",
              "regex": "\\{object\\}",
              "replacement": "un antico tomo rilegato in pelle",
              "parameters": [],
              "actor": "ANY",
              "command": null,
              "replace": true
            },
            {
              "id": "f5a2c1b0-8d3e-4f7c-9a2b-1c3d4e5f6a7b",
              "type": "text_substitution",
              "regex": "\\{action\\}",
              "replacement": "il personaggio si muove furtivamente tra le ombre",
              "parameters": [],
              "actor": "PLAYER",
              "command": null,
              "replace": true
            },
            {
              "id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
              "type": "prompt_description",
              "regex": "\\{prompt\\[(\\d+)\\]\\}",
              "replacement": null,
              "parameters": [
                {
                  "name": "param_index",
                  "description": "Indice del parametro all'interno del tag prompt",
                  "default_value": 0
                },
                {
                  "name": "prompt_value_0",
                  "description": "Il primo valore per il prompt parametrizzato",
                  "value": "un maestoso drago che vola sopra montagne innevate"
                },
                {
                  "name": "prompt_value_1",
                  "description": "Il secondo valore per il prompt parametrizzato",
                  "value": "un eroe che brandisce una spada fiammeggiante"
                },
                {
                  "name": "prompt_value_2",
                  "description": "Il terzo valore per il prompt parametrizzato",
                  "value": "un misterioso manufatto antico avvolto in un'aura magica"
                }
              ],
              "actor": "DM",
              "command": null,
              "replace": true
            },
            {
              "id": "b2c3d4e5-f6a7-8901-2345-67890abcdef0",
              "type": "dnd_environment_description",
              "regex": "\\{ambiente_dnd\\[([^\\]]+)\\]\\}",
              "replacement": null,
              "parameters": [
                {
                  "name": "environment_type",
                  "description": "Il tipo di ambiente da descrivere (es. 'foresta', 'caverna', 'città')",
                  "default_value": "generico"
                },
                {
                  "name": "dettagli_aggiuntivi",
                  "description": "Dettagli specifici per l'ambiente",
                  "default_value": ""
                },
                {
                  "name": "description_forest",
                  "value": "Ti trovi in una fitta foresta, alberi secolari si ergono imponenti e la luce filtra a stento tra le fronde, creando giochi d'ombra misteriosi. L'aria è umida e si sente il fruscio delle foglie sotto i tuoi piedi."
                },
                {
                  "name": "description_cave",
                  "value": "L'oscurità ti avvolge mentre entri nella caverna. L'eco dei tuoi passi si disperde nel silenzio, interrotto solo dal gocciolio dell'acqua. Un leggero odore di umidità e muschio pervade l'aria."
                }
              ],
              "actor": "DM",
              "command": null,
              "replace": true
            },
            {
              "id": "c3d4e5f6-a7b8-9012-3456-7890abcdef1",
              "type": "chat_bot_saluto",
              "regex": "\\{greeting\\}",
              "replacement": "Ciao! Sono qui per aiutarti. Come posso assisterti oggi?",
              "parameters": [],
              "actor": "NPC",
              "command": null,
              "replace": true
            },
            {
              "id": "d4e5f6a7-b8c9-0123-4567-890abcdef2",
              "type": "game_mechanic",
              "regex": "\\{strength\\[(base|medium|advanced|master)\\]\\}",
              "replacement": null,
              "parameters": [
                {
                  "name": "challenge_level",
                  "description": "The challenge level for Strength check",
                  "default_value": "base"
                },
                {
                  "name": "strength_base_desc",
                  "value": "Perform a basic physical feat, like pushing a small crate."
                },
                {
                  "name": "strength_medium_desc",
                  "value": "Attempt to open a rusty, stuck door."
                },
                {
                  "name": "strength_advanced_desc",
                  "value": "Try to lift a fallen stone pillar to clear a path."
                },
                {
                  "name": "strength_master_desc",
                  "value": "Exert immense power to break free from magical restraints."
                }
              ],
              "actor": "DM",
              "command": "game_challenge",
              "replace": true
            },
            {
              "id": "e5f6a7b8-c9d0-1234-5678-90abcdef3",
              "type": "trigger_audio",
              "regex": "\\{play_audio\\[([^\\]]+)\\]\\}",
              "replacement": null,
              "parameters": [
                {
                  "name": "audio_file",
                  "description": "The name of the audio file to play",
                  "default_value": "default.mp3"
                }
              ],
              "actor": "DM",
              "command": "play_audio",
              "replace": false
            },
            {
              "id": "f6a7b8c9-d0e1-2345-6789-0abcdef4",
              "type": "generate_image",
              "regex": "\\{generate_image\\[([^\\]]+)\\]\\}",
              "replacement": null,
              "parameters": [
                {
                  "name": "prompt",
                  "description": "The prompt to send to the stable diffusion model",
                  "default_value": "a default image"
                }
              ],
              "actor": "DM",
              "command": "generate_image",
              "replace": false
            },
            {
              "id": "a7b8c9d0-e1f2-3456-7890-abcdef5",
              "type": "trigger_graphic_effect",
              "regex": "\\{graphic_effect\\[([^\\]]+)\\]\\}",
              "replacement": null,
              "parameters": [
                {
                  "name": "effect_name",
                  "description": "The name of the graphic effect to trigger",
                  "default_value": "default_effect"
                }
              ],
              "actor": "DM",
              "command": "trigger_graphic_effect",
              "replace": false
            }
          ]
        }
    """.trimIndent()

    private lateinit var parser: StringTagParser
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        val mockAssetManager = mock(AssetManager::class.java)

        `when`(mockContext.assets).thenReturn(mockAssetManager)
        `when`(mockAssetManager.open("config.json")).thenReturn(ByteArrayInputStream(testJsonConfig.toByteArray()))

        parser = StringTagParser(mockContext)
    }

    @Test
    fun `test text substitution tag`() {
        val input = "C'è un {object} lì."
        val expectedText = "C'è un un antico tomo rilegato in pelle lì."
        val (actualText, commands) = parser.parseAndReplaceWithCommands(input, CharacterType.DM)
        assertEquals(expectedText, actualText)
        assertTrue("Nessun comando atteso", commands.isEmpty())
    }

    @Test
    fun `test parameterized prompt tag`() {
        val input = "Genera un'immagine di {prompt[0]}."
        val expectedText = "Genera un'immagine di un maestoso drago che vola sopra montagne innevate."
        val (actualText, commands) = parser.parseAndReplaceWithCommands(input, CharacterType.DM)
        assertEquals(expectedText, actualText)
        assertTrue("Nessun comando atteso", commands.isEmpty())
    }

    @Test
    fun `test game mechanic tag base level`() {
        val input = "{strength[base]}"
        val expectedText = "SFIDA_STRENGTH_BASE: Perform a basic physical feat, like pushing a small crate."
        val (actualText, commands) = parser.parseAndReplaceWithCommands(input, CharacterType.DM)
        assertEquals(expectedText, actualText)

        assertEquals(1, commands.size)
        val command = commands[0]
        assertEquals("game_challenge", command.commandName)
        assertEquals("base", command.parameters["captured_value_from_regex"])
        assertEquals("Perform a basic physical feat, like pushing a small crate.", command.parameters["strength_base_desc"])
    }

    @Test
    fun `test game mechanic tag medium level`() {
        val input = "{strength[medium]}"
        val expectedText = "SFIDA_STRENGTH_MEDIUM: Attempt to open a rusty, stuck door."
        val (actualText, commands) = parser.parseAndReplaceWithCommands(input, CharacterType.DM)
        assertEquals(expectedText, actualText)

        assertEquals(1, commands.size)
        val command = commands[0]
        assertEquals("game_challenge", command.commandName)
        assertEquals("medium", command.parameters["captured_value_from_regex"])
        assertEquals("Attempt to open a rusty, stuck door.", command.parameters["strength_medium_desc"])
    }

    @Test
    fun `test actor filtering`() {
        val input = "Un saluto {greeting} dal NPC."
        val input2 = "C'è un {object}."

        // Test con filtro per DM (il tag greeting (NPC) non dovrebbe essere processato, rimane intatto)
        val (actualDMText, actualDMCommands) = parser.parseAndReplaceWithCommands(input, CharacterType.DM)
        assertEquals(input, actualDMText)
        assertTrue(actualDMCommands.isEmpty())

        // Il tag {object} (DM) DEVE essere sostituito
        val (actualDM2Text, actualDM2Commands) = parser.parseAndReplaceWithCommands(input2, CharacterType.DM)
        assertEquals("C'è un un antico tomo rilegato in pelle.", actualDM2Text)
        assertTrue(actualDM2Commands.isEmpty())

        // Test con filtro per NPC (il tag object (DM) non dovrebbe essere processato, rimane intatto)
        val (actualNPCText, actualNPCCommands) = parser.parseAndReplaceWithCommands(input, CharacterType.NPC)
        assertEquals("Un saluto Ciao! Sono qui per aiutarti. Come posso assisterti oggi? dal NPC.", actualNPCText)
        assertTrue(actualNPCCommands.isEmpty())

        val (actualNPC2Text, actualNPC2Commands) = parser.parseAndReplaceWithCommands(input2, CharacterType.NPC)
        assertEquals(input2, actualNPC2Text)
        assertTrue(actualNPC2Commands.isEmpty())
    }

    @Test
    fun `test play audio command tag with no replacement`() {
        val input = "Suona l'audio {play_audio[ambient_forest.mp3]}."
        val expectedText = "Suona l'audio {play_audio[ambient_forest.mp3]}."
        val (actualText, commands) = parser.parseAndReplaceWithCommands(input, CharacterType.DM)

        assertEquals(expectedText, actualText)
        assertEquals(1, commands.size)

        val command = commands[0]
        assertEquals("play_audio", command.commandName)
        assertEquals("ambient_forest.mp3", command.parameters["captured_value_from_regex"])
    }

    @Test
    fun `test generate image command tag with no replacement`() {
        val input = "Genera l'immagine di {generate_image[a magical forest at dusk]}."
        val expectedText = "Genera l'immagine di {generate_image[a magical forest at dusk]}."
        val (actualText, commands) = parser.parseAndReplaceWithCommands(input, CharacterType.DM)

        assertEquals(expectedText, actualText)
        assertEquals(1, commands.size)

        val command = commands[0]
        assertEquals("generate_image", command.commandName)
        assertEquals("a magical forest at dusk", command.parameters["captured_value_from_regex"])
    }

    @Test
    fun `test graphic effect command tag with no replacement`() {
        val input = "Un lampo di luce accecante {graphic_effect[flash]} apparve."
        val expectedText = "Un lampo di luce accecante {graphic_effect[flash]} apparve."
        val (actualText, commands) = parser.parseAndReplaceWithCommands(input, CharacterType.DM)

        assertEquals(expectedText, actualText)
        assertEquals(1, commands.size)

        val command = commands[0]
        assertEquals("trigger_graphic_effect", command.commandName)
        assertEquals("flash", command.parameters["captured_value_from_regex"])
    }

    @Test
    fun `test mixed text and commands with mixed replacement`() {
        val input = "Il drago attacca! {graphic_effect[dragon_roar]}. Il {object} si rompe."
        val expectedText = "Il drago attacca! {graphic_effect[dragon_roar]}. Il un antico tomo rilegato in pelle si rompe."
        val (actualText, commands) = parser.parseAndReplaceWithCommands(input, CharacterType.DM)

        assertEquals(expectedText, actualText)
        assertEquals(1, commands.size)

        val command = commands[0]
        assertEquals("trigger_graphic_effect", command.commandName)
        assertEquals("dragon_roar", command.parameters["captured_value_from_regex"])
    }

    @Test
    fun `test tag not found`() {
        val input = "Questo è un test senza {unknown_tag}."
        val expectedText = "Questo è un test senza {unknown_tag}."
        val (actualText, commands) = parser.parseAndReplaceWithCommands(input)
        assertEquals(expectedText, actualText)
        assertTrue(commands.isEmpty())
    }
}