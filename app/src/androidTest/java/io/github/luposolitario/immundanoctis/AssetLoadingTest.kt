package io.github.luposolitario.immundanoctis

// app/src/androidTest/java/io/github/luposolitario.immundanoctis/AssetLoadingTest.kt

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.luposolitario.immundanoctis.util.StringTagParser
import io.github.luposolitario.immundanoctis.data.CharacterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssetLoadingTest {

    @Test
    fun testConfigJsonIsLoadableAndParsable() {
        // Ottieni il contesto dell'applicazione
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        // Prova a istanziare il parser usando il contesto reale dell'app
        val parser = StringTagParser(appContext)

        // --- Test 1: Sostituzione di testo semplice ({object}) ---
        val inputStringObject = "C'è un {object} lì."
        val expectedTextObject = "C'è un un antico tomo rilegato in pelle lì." // Da config.json
        val (parsedObjectText, objectCommands) = parser.parseAndReplaceWithCommands(inputStringObject, CharacterType.DM)

        assertEquals("Il parser dovrebbe sostituire il tag {object}", expectedTextObject, parsedObjectText)
        assertTrue("Nessun comando atteso per {object}", objectCommands.isEmpty())

        // --- Test 2: Tag game_mechanic ({strength[base]}) ---
        // Nel config.json, {strength} ha replace=true e command="game_challenge"
        val inputGameMechanic = "Devi superare una prova di {strength[base]}."
        val expectedGameMechanicText = "Devi superare una prova di SFIDA_STRENGTH_BASE: Perform a basic physical feat, like pushing a small crate.." // Assicurati che l'output corrisponda esattamente
        val (parsedGameMechanicText, gameMechanicCommands) = parser.parseAndReplaceWithCommands(inputGameMechanic, CharacterType.DM)

        assertEquals("Il parser dovrebbe sostituire il tag {strength}", expectedGameMechanicText, parsedGameMechanicText)
        assertEquals("Dovrebbe essere estratto un comando game_challenge", 1, gameMechanicCommands.size)
        assertEquals("game_challenge", gameMechanicCommands[0].commandName)
        assertEquals("base", gameMechanicCommands[0].parameters["captured_value_from_regex"]) // Corretto chiave
        // Potresti anche asserire sul 'strength_base_desc' se lo estrai nella mappa parameters
        // assertEquals("Perform a basic physical feat, like pushing a small crate.", gameMechanicCommands[0].parameters["strength_base_desc"])

        // --- Test 3: Comando audio con replace=false ({play_audio}) ---
        val inputAudioCommand = "Suona l'audio {play_audio[test_music.mp3]}."
        // Expected: il tag rimane nel testo perché replace è false
        val expectedAudioCommandText = "Suona l'audio {play_audio[test_music.mp3]}."
        val (parsedAudioCommandText, extractedAudioCommands) = parser.parseAndReplaceWithCommands(inputAudioCommand, CharacterType.DM)

        assertEquals("Il tag {play_audio} non dovrebbe essere rimosso", expectedAudioCommandText, parsedAudioCommandText)
        assertEquals("Dovrebbe essere estratto un comando audio", 1, extractedAudioCommands.size)
        assertEquals("Il comando dovrebbe essere 'play_audio'", "play_audio", extractedAudioCommands[0].commandName)
        assertEquals("Il parametro 'captured_value_from_regex' dovrebbe essere 'test_music.mp3'", "test_music.mp3", extractedAudioCommands[0].parameters["captured_value_from_regex"]) // Corretto chiave

        // --- Test 4: Comando immagine con replace=false ({generate_image}) ---
        val inputImageCommand = "Genera un'immagine di {generate_image[a magical forest]}."
        val expectedImageCommandText = "Genera un'immagine di {generate_image[a magical forest]}."
        val (parsedImageCommandText, extractedImageCommands) = parser.parseAndReplaceWithCommands(inputImageCommand, CharacterType.DM)

        assertEquals("Il tag {generate_image} non dovrebbe essere rimosso", expectedImageCommandText, parsedImageCommandText)
        assertEquals("Dovrebbe essere estratto un comando immagine", 1, extractedImageCommands.size)
        assertEquals("Il comando dovrebbe essere 'generate_image'", "generate_image", extractedImageCommands[0].commandName)
        assertEquals("a magical forest", extractedImageCommands[0].parameters["captured_value_from_regex"])

        // --- Test 5: Comando effetto grafico con replace=false ({graphic_effect}) ---
        val inputGraphicEffectCommand = "Attiva l'effetto {graphic_effect[flash]}."
        val expectedGraphicEffectCommandText = "Attiva l'effetto {graphic_effect[flash]}."
        val (parsedGraphicEffectCommandText, extractedGraphicEffectCommands) = parser.parseAndReplaceWithCommands(inputGraphicEffectCommand, CharacterType.DM)

        assertEquals("Il tag {graphic_effect} non dovrebbe essere rimosso", expectedGraphicEffectCommandText, parsedGraphicEffectCommandText)
        assertEquals("Dovrebbe essere estratto un comando effetto grafico", 1, extractedGraphicEffectCommands.size)
        assertEquals("Il comando dovrebbe essere 'trigger_graphic_effect'", "trigger_graphic_effect", extractedGraphicEffectCommands[0].commandName)
        assertEquals("flash", extractedGraphicEffectCommands[0].parameters["captured_value_from_regex"])

        // --- Test 6: Test di actor filtering per un tag con replace=true ({greeting}) ---
        val inputGreetingNPC = "Un saluto {greeting}." // Questo tag ha attore "NPC" e replace=true
        val expectedGreetingText = "Un saluto Ciao! Sono qui per aiutarti. Come posso assisterti oggi?."
        val (parsedGreetingText, greetingCommands) = parser.parseAndReplaceWithCommands(inputGreetingNPC, CharacterType.NPC) // Filtro per NPC
        assertEquals("Il tag {greeting} dovrebbe essere sostituito se l'attore è NPC", expectedGreetingText, parsedGreetingText)
        assertTrue(greetingCommands.isEmpty()) // greeting non ha un comando

        // Test 7: {greeting} con attore non corrispondente (DM) - non dovrebbe sostituire
        val (parsedGreetingDMText, greetingDMCommands) = parser.parseAndReplaceWithCommands(inputGreetingNPC, CharacterType.DM) // Filtro per DM
        assertEquals("Il tag {greeting} non dovrebbe essere sostituito se l'attore è DM", inputGreetingNPC, parsedGreetingDMText)
        assertTrue(greetingDMCommands.isEmpty())

        // --- Test 8: Test con un tag sconosciuto ---
        val inputUnknownTag = "Questo è un testo con un {tag_sconosciuto}."
        val (parsedUnknownText, unknownCommands) = parser.parseAndReplaceWithCommands(inputUnknownTag, CharacterType.DM)
        assertEquals("Il tag sconosciuto non dovrebbe essere modificato", inputUnknownTag, parsedUnknownText)
        assertTrue("Nessun comando atteso per tag sconosciuto", unknownCommands.isEmpty())
    }
}