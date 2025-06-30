package io.github.luposolitario.immundanoctis

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctis.stdf.data.StdfModel
import io.github.luposolitario.immundanoctis.stdf.data.StdfModelRepository
import io.github.luposolitario.immundanoctis.stdf.service.BackendService
import io.github.luposolitario.immundanoctis.stdf.service.BackgroundGenerationService
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.ImageGenerationPreferences
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import kotlinx.coroutines.flow.collectLatest

class StdfGenerationActivity : ComponentActivity() {

    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    private val imageGenerationPreferences by lazy { ImageGenerationPreferences(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var modelId = intent.getStringExtra("modelId")
        if (modelId == null) {
            modelId = imageGenerationPreferences.getSelectedModelId()
        }

        if (modelId == null) {
            Toast.makeText(this, "Nessun modello selezionato. Scegline uno dalla lista.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val modelRepository = StdfModelRepository(this)
        val model = modelRepository.models.find { it.id == modelId }

        if (model == null) {
            Toast.makeText(this, "Errore: Modello selezionato non trovato.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            val useDarkTheme = themePreferences.useDarkTheme(isSystemInDarkTheme())
            ImmundaNoctisTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StdfGenerationScreen(model = model)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, BackendService::class.java))
        Log.d("StdfGenerationActivity", "onDestroy: Inviato comando di stop al BackendService.")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StdfGenerationScreen(model: StdfModel) {
    val context = LocalContext.current

    // --- 👇 LOGICA DEI PROMPT SPOSTATA QUI 👇 ---
    // Inizializziamo i prompt basandoci sull'ID del modello ricevuto
    val initialPrompt = remember(model.id) {
        when {
            model.id.contains("anything") -> "masterpiece, best quality, 1girl, solo, cute, ((white hair)), looking at viewer, upper body, garden background"
            model.id.contains("qtea") -> "chibi, masterpiece, best quality, 1girl, solo, cute, ((pink hair)), cat ears, playful pose, candy background"
            model.id.contains("yuki") -> "cute, masterpiece, best quality, 1girl, solo, ((light blue hair)), beautiful detailed eyes, school uniform, classroom background"
            model.id.contains("reality") -> "photograph of a beautiful woman, 24 years old, detailed skin, soft light, 8k, uhd, photorealistic"
            model.id.contains("chillout") -> "RAW photo, 1korean girl, masterpiece, best quality, photorealistic, cinematic light, sitting on a cafe chair"
            else -> "a majestic lion jumping from a waterfall, cinematic, dramatic light"
        }
    }

    val initialNegativePrompt = remember(model.id) {
        when {
            model.id.contains("reality") || model.id.contains("chillout") -> "deformed, bad anatomy, disfigured, poorly drawn face, mutation, mutated, extra limb, ugly, disgusting, poorly drawn hands, missing limb, floating limbs, disconnected limbs, malformed hands, blurry, ((((mutated hands and fingers)))), watermark, cgi, 3d, render, cartoon, anime, drawing"
            else -> "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry"
        }
    }

    var prompt by remember { mutableStateOf(initialPrompt) }
    var negativePrompt by remember { mutableStateOf(initialNegativePrompt) }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var generationState by remember { mutableStateOf<BackgroundGenerationService.GenerationState>(BackgroundGenerationService.GenerationState.Idle) }

    LaunchedEffect(Unit) {
        BackgroundGenerationService.generationState.collectLatest { state ->
            generationState = state
            if (state is BackgroundGenerationService.GenerationState.Complete) {
                generatedBitmap = state.bitmap
            }
        }
    }

    LaunchedEffect(key1 = model.id) {
        Log.d("StdfGenerationScreen", "Invio comando di avvio al BackendService per il modello: ${model.id}")
        val intent = Intent(context, BackendService::class.java).apply {
            putExtra("modelId", model.id)
        }
        context.startService(intent)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "Genera con ${model.name}") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            OutlinedTextField(
                value = negativePrompt,
                onValueChange = { negativePrompt = it },
                label = { Text("Negative Prompt") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Button(
                onClick = {
                    val intent = Intent(context, BackgroundGenerationService::class.java).apply {
                        putExtra("prompt", prompt)
                        putExtra("negative_prompt", negativePrompt)
                    }
                    context.startService(intent)
                },
                enabled = generationState !is BackgroundGenerationService.GenerationState.Progress,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Genera")
            }
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // La logica di visualizzazione rimane la stessa
                when (val state = generationState) {
                    is BackgroundGenerationService.GenerationState.Idle -> {
                        generatedBitmap?.let {
                            Image(bitmap = it.asImageBitmap(), contentDescription = "Ultima immagine generata", modifier = Modifier.fillMaxSize())
                        } ?: Text("Pronto per generare un'immagine.")
                    }
                    is BackgroundGenerationService.GenerationState.Progress -> {
                        CircularProgressIndicator(progress = { state.progress })
                        Text("${(state.progress * 100).toInt()}%", modifier = Modifier.padding(top = 80.dp))
                    }
                    is BackgroundGenerationService.GenerationState.Complete -> {
                        Image(bitmap = state.bitmap.asImageBitmap(), contentDescription = "Immagine generata", modifier = Modifier.fillMaxSize())
                    }
                    is BackgroundGenerationService.GenerationState.Error -> {
                        Text("Errore: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}