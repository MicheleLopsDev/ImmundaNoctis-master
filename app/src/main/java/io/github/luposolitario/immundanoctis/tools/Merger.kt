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
data class StructuralScene(
    val id: String,
    val sceneType: String,
    val genre: String,
    val challengeLevel: String,
    val gameMechanics: List<String> = emptyList(),
    val images: List<SceneImage> = emptyList(),
    val choices: List<NarrativeChoice> = emptyList(),
    val disciplineChoices: List<DisciplineChoice> = emptyList()
)

// Classe per il JSON narrativo (narrative.json)
data class NarrativeOnlyScene(
    val id: String,
    val narrativeText: LocalizedText
)

// Classe finale per la scena unita (corrisponde alla tua classe Scene)
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
    val gson = GsonBuilder().setPrettyPrinting().create()

    // --- NOMI DEI FILE AGGIORNATI ---
    val structureFileName = "structured.json"
    val narrativeFileName = "narrative.json"
    val outputFileName = "scenes.json"

    println("--- INIZIO PROCESSO DI MERGE CON GSON ---")

    try {
        // 1. Leggi il contenuto dei file JSON di input
        val structureJsonContent = File(structureFileName).readText()
        val narrativeJsonContent = File(narrativeFileName).readText()
        println("✅ File '$structureFileName' e '$narrativeFileName' letti correttamente.")

        // 2. Deserializza (converte da testo a oggetti Kotlin) i due JSON usando Gson
        val structuralData = gson.fromJson(structureJsonContent, StructuralWrapper::class.java)

        // Per deserializzare una lista, Gson ha bisogno di un TypeToken
        val narrativeListType = object : TypeToken<List<NarrativeOnlyScene>>() {}.type
        val narrativeDataList: List<NarrativeOnlyScene> = gson.fromJson(narrativeJsonContent, narrativeListType)
        println("✅ Dati di struttura e narrativi parsati correttamente con Gson.")

        // 3. Converte la lista di narrazioni in una mappa per un accesso istantaneo.
        val narrativeMap = narrativeDataList.associateBy({ it.id }, { it.narrativeText })
        println("✅ Mappa delle narrazioni creata per un merge efficiente.")

        // 4. Esegue il merge
        val mergedScenes = structuralData.scenes.mapNotNull { structuralScene ->
            val narrativeText = narrativeMap[structuralScene.id]

            if (narrativeText == null) {
                println("⚠️ ATTENZIONE: Nessun testo narrativo trovato per la scena ID: ${structuralScene.id}. La scena verrà saltata.")
                null
            } else {
                Scene(
                    id = structuralScene.id,
                    sceneType = structuralScene.sceneType,
                    genre = structuralScene.genre,
                    challengeLevel = structuralScene.challengeLevel,
                    narrativeText = narrativeText,
                    gameMechanics = structuralScene.gameMechanics,
                    images = structuralScene.images,
                    choices = structuralScene.choices,
                    disciplineChoices = structuralScene.disciplineChoices
                )
            }
        }
        println("✅ Merge completato: ${mergedScenes.size} scene processate e unite.")

        // 5. Crea l'oggetto finale che conterrà i dati uniti
        val finalMergedData = ScenesWrapper(
            adventureName = structuralData.adventureName,
            scenes = mergedScenes
        )

        // 6. Serializza (converte da oggetto Kotlin a testo) l'oggetto finale in una stringa JSON
        val finalJsonString = gson.toJson(finalMergedData)

        // 7. Salva il risultato su un nuovo file
        val outputFile = File(outputFileName)
        outputFile.writeText(finalJsonString)
        println("\n--- RISULTATO ---")
        println("✅ JSON finale salvato correttamente nel file: ${outputFile.absolutePath}")

    } catch (e: FileNotFoundException) {
        println("\n❌ ERRORE: Uno dei file di input non è stato trovato. Assicurati che '$structureFileName' e '$narrativeFileName' siano nella stessa directory dello script.")
    } catch (e: Exception) {
        // Cattura eccezioni più generiche, incluse quelle di parsing di Gson
        println("\n❌ ERRORE IMPREVISTO: ${e.message}")
        e.printStackTrace()
    }
}
