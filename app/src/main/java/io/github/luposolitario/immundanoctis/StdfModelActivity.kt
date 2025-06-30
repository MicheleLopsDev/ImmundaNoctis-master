package io.github.luposolitario.immundanoctis

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import io.github.luposolitario.immundanoctis.stdf.ui.StdfModelSlotView
import io.github.luposolitario.immundanoctis.stdf.worker.StdfDownloadWorker
import io.github.luposolitario.immundanoctis.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctis.util.ThemePreferences
import io.github.luposolitario.immundanoctis.view.StdfViewModel
import kotlinx.coroutines.launch

class StdfModelActivity : ComponentActivity() {

    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    private val viewModel: StdfViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val useDarkTheme = themePreferences.useDarkTheme(isSystemInDarkTheme())
            ImmundaNoctisTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StdfModelManagementScreen(viewModel = viewModel)
                }
            }
        }
    }
}

private const val DEBUG_TAG = "StdfDebug"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StdfModelManagementScreen(viewModel: StdfViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val workInfos by viewModel.workInfosLiveData.observeAsState(initial = emptyList())

    // Effetto per ricaricare la lista dei modelli quando un download ha successo
    // e il ViewModel non lo sa ancora.
    LaunchedEffect(workInfos) {
        workInfos.forEach { workInfo ->
            if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                val modelId = workInfo.tags.firstOrNull { it != "stdf_download_tag" }
                val modelInState = uiState.models.find { it.id == modelId }
                if (modelInState != null && !modelInState.isDownloaded) {
                    Log.d(DEBUG_TAG, "Work per ${modelId} SUCCEEDED ma stato non ancora aggiornato. Ricarico.")
                    viewModel.reloadModels()
                }
            }
        }
    }

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val tabTitles = listOf("NPU Models", "CPU Models")

    val npuModels = uiState.models.filter { !it.runOnCpu }
    val cpuModels = uiState.models.filter { it.runOnCpu }

    Scaffold(topBar = { TopAppBar(title = { Text("Gestione Modelli Immagine (STDF)") }) }) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]))
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) }
                    )
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val modelsToShow = if (page == 0) npuModels else cpuModels
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(modelsToShow, key = { it.id }) { model ->

                        val relevantWorkInfo = remember(workInfos, model.id) {
                            workInfos.find { it.tags.contains(model.id) }
                        }

                        // --- 👇 LOGICA DI STATO DEFINITIVA 👇 ---
                        val isDownloading = relevantWorkInfo?.state == WorkInfo.State.RUNNING || relevantWorkInfo?.state == WorkInfo.State.ENQUEUED
                        val isCompleted = relevantWorkInfo?.state == WorkInfo.State.SUCCEEDED

                        // Consideriamo il modello scaricato se il nostro stato interno (isDownloaded) è true
                        // OPPURE se il worker ha appena finito con successo (per coprire il ritardo di aggiornamento)
                        val isEffectivelyDownloaded = model.isDownloaded || isCompleted

                        val progress = relevantWorkInfo?.progress?.getInt(StdfDownloadWorker.KEY_PROGRESS, 0) ?: 0

                        Log.d(DEBUG_TAG, "Modello: ${model.id} -> isDownloaded (da repo): ${model.isDownloaded}, isCompleted (da worker): $isCompleted, isDownloading: $isDownloading, Progress: $progress%")

                        StdfModelSlotView(
                            model = model,
                            // SOSTITUIAMO la variabile isDownloaded originale con la nostra calcolata
                            isDownloaded = isEffectivelyDownloaded,
                            isDownloading = isDownloading,
                            isSelected = (model.id == uiState.selectedModelId),
                            downloadProgress = progress,
                            onDownloadClick = { viewModel.startDownload(model) },
                            onDeleteClick = { viewModel.deleteModel(model) },
                            onSelectClick = { viewModel.selectModel(model.id) }, // <-- Colleghiamo la selezione
                            onGenerateClick = { selectedModel ->
                                val intent = Intent(context, StdfGenerationActivity::class.java).apply {
                                    putExtra("modelId", selectedModel.id)
                                    putExtra("defaultPrompt", selectedModel.defaultPrompt)
                                    putExtra("defaultNegativePrompt", selectedModel.defaultNegativePrompt)
                                }
                                context.startActivity(intent)
                            }

                        )
                    }
                }
            }
        }
    }
}