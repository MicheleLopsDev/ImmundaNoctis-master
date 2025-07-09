import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileNotFoundException

// --- DATA CLASSES (da GameData.kt) ---
// Definiscono la struttura dei tuoi file JSON per una corretta (de)serializzazione con Gson.
// Le annotazioni @Serializable non sono più necessarie.

data class LocalizedText(
    val english: String?,
    val italian: String? = null
)

data class NarrativeChoice(
    val scene: String,
    val progressive: String,
    val choiceText: LocalizedText,
    val nextSceneId: String,
    val requiredItem: String? = null
)

data class DisciplineChoice(
    val scene: String,
    val discipline: String,
    val choiceText: LocalizedText,
    val nextSceneId: String
)

data class SceneImage(
    val imageUrl: String,
    val caption: String
)

// Classe per deserializzare il file di struttura (structured.json)
// Le liste sono rese nullable (?) per gestire in modo sicuro i campi mancanti nel JSON
data class StructuralScene(
    val id: String,
    val sceneType: String,
    val genre: String,
    val challengeLevel: String,
    val gameMechanics: List<String>?,
    val images: List<SceneImage>?,
    val choices: List<NarrativeChoice>?,
    val disciplineChoices: List<DisciplineChoice>?
)

// Classe per il JSON narrativo (narrative.json)
data class NarrativeOnlyScene(
    val id: String,
    val narrativeText: LocalizedText
)

// Classe finale per la scena unita (corrisponde alla tua classe Scene)
// Qui le liste sono non-nullable, come nella tua app, perché garantiamo un valore di default.
data class Scene(
    val id: String,
    val sceneType: String,
    val genre: String,
    val challengeLevel: String,
    val narrativeText: LocalizedText, // Campo obbligatorio
    val gameMechanics: List<String> = emptyList(),
    val images: List<SceneImage> = emptyList(),
    val choices: List<NarrativeChoice> = emptyList(),
    val disciplineChoices: List<DisciplineChoice> = emptyList()
)

// Wrapper per i file JSON di input e output
data class StructuralWrapper(
    val adventureName: String,
    val scenes: List<StructuralScene>
)

data class ScenesWrapper( // Corrisponde al tuo wrapper per Gson
    val adventureName: String,
    val scenes: List<Scene>
)


fun main() {
    // Configurazione del parser JSON con Gson
    val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping() // Impedisce a Gson di convertire <, >, & ecc. in sequenze \uXXXX
        .create()

    // --- PERCORSO CONFIGURABILE ---
    // Modifica questa stringa per puntare alla directory che contiene i tuoi file JSON.
    val assetsDirectoryPath = "C:\\DEV\\ImmundaNoctis-master\\app\\src\\main\\assets\\"

    // --- NOMI DEI FILE ---
    val structureFileName = "structured.json"
    val narrativeFileName = "narrative.json"
    val outputMergedFileName = "scenes.json"
    val outputMechanicsFileName = "test-xml.json" // Nuovo file per i tag XML

    println("--- INIZIO PROCESSO DI MERGE CON GSON ---")
    println("--- Directory di lavoro: ${File(assetsDirectoryPath).absolutePath} ---")


    try {
        // 1. Costruisci il percorso completo e leggi il contenuto dei file
        val structureFile = File(assetsDirectoryPath, structureFileName)
        val narrativeFile = File(assetsDirectoryPath, narrativeFileName)

        val structureJsonContent = structureFile.readText()
        val narrativeJsonContent = narrativeFile.readText()
        println("✅ File '${structureFile.path}' e '${narrativeFile.path}' letti correttamente.")

        // 2. Deserializza i due JSON
        val structuralData = gson.fromJson(structureJsonContent, StructuralWrapper::class.java)
        val narrativeListType = object : TypeToken<List<NarrativeOnlyScene>>() {}.type
        val narrativeDataList: List<NarrativeOnlyScene> = gson.fromJson(narrativeJsonContent, narrativeListType)
        println("✅ Dati di struttura e narrativi parsati correttamente con Gson.")

        // 3. Crea una mappa delle narrazioni per un accesso efficiente
        val narrativeMap = narrativeDataList.associateBy({ it.id }, { it.narrativeText })
        println("✅ Mappa delle narrazioni creata.")

        // 4. Esegue il merge e raccoglie i tag di gameMechanics
        val allGameMechanicsTags = mutableListOf<String>()
        val mergedScenes = structuralData.scenes.mapNotNull { structuralScene ->
            val narrativeText = narrativeMap[structuralScene.id]

            if (narrativeText == null) {
                println("⚠️ ATTENZIONE: Nessun testo narrativo trovato per la scena ID: ${structuralScene.id}. La scena verrà saltata.")
                null
            } else {
                // Aggiunge i tag di questa scena alla lista complessiva
                structuralScene.gameMechanics?.let { allGameMechanicsTags.addAll(it) }

                Scene(
                    id = structuralScene.id,
                    sceneType = structuralScene.sceneType,
                    genre = structuralScene.genre,
                    challengeLevel = structuralScene.challengeLevel,
                    narrativeText = narrativeText,
                    gameMechanics = structuralScene.gameMechanics ?: emptyList(),
                    images = structuralScene.images ?: emptyList(),
                    choices = structuralScene.choices ?: emptyList(),
                    disciplineChoices = structuralScene.disciplineChoices ?: emptyList()
                )
            }
        }
        println("✅ Merge completato: ${mergedScenes.size} scene processate.")
        println("✅ Raccolti ${allGameMechanicsTags.size} tag da 'gameMechanics'.")


        // 5. Crea l'oggetto finale per le scene unite
        val finalMergedData = ScenesWrapper(
            adventureName = structuralData.adventureName,
            scenes = mergedScenes
        )

        // 6. Serializza e salva il file delle scene unite
        val finalJsonString = gson.toJson(finalMergedData)
        val outputMergedFile = File(assetsDirectoryPath, outputMergedFileName)
        outputMergedFile.writeText(finalJsonString)
        println("\n--- RISULTATO ---")
        println("✅ JSON delle scene unite salvato correttamente nel file: ${outputMergedFile.absolutePath}")

        // 7. Serializza e salva il file con i soli tag di gameMechanics
        val mechanicsJsonString = gson.toJson(allGameMechanicsTags)
        val outputMechanicsFile = File(assetsDirectoryPath, outputMechanicsFileName)
        outputMechanicsFile.writeText(mechanicsJsonString)
        println("✅ JSON con i tag XML di validazione salvato correttamente nel file: ${outputMechanicsFile.absolutePath}")


    } catch (e: FileNotFoundException) {
        println("\n❌ ERRORE: Uno dei file di input non è stato trovato. Assicurati che i file esistano nel percorso specificato: '${File(assetsDirectoryPath).absolutePath}'")
    } catch (e: Exception) {
        // Cattura eccezioni più generiche, incluse quelle di parsing di Gson
        println("\n❌ ERRORE IMPREVISTO: ${e.message}")
        e.printStackTrace()
    }
}
