package io.github.luposolitario.immundanoctis.stdf.data

import android.content.Context
import android.os.Build
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.github.luposolitario.immundanoctis.R
import io.github.luposolitario.immundanoctis.stdf.worker.StdfDownloadWorker
import io.github.luposolitario.immundanoctis.util.ImageGenerationPreferences
import java.io.File
import com.google.gson.Gson

private fun getDeviceSoc(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else "CPU"
}
data class ModelFile(val name: String, val displayName: String, val uri: String)
data class HighresInfo(val size: Int, val patchFileName: String, val isDownloaded: Boolean = false, val isDownloading: Boolean = false)
data class DownloadProgress(val displayName: String, val currentFileIndex: Int, val totalFiles: Int, val progress: Float, val downloadedBytes: Long, val totalBytes: Long)
val chipsetModelSuffixes = mapOf("SM8475" to "8gen1", "SM8450" to "8gen1", "SM8550" to "8gen2", "SM8550P" to "8gen2", "QCS8550" to "8gen2", "QCM8550" to "8gen2", "SM8650" to "8gen3", "SM8650P" to "8gen3", "SM8750" to "8gen4", "SM8750P" to "8gen4")

data class StdfModel(
    val id: String,
    val name: String,
    val description: String,
    val baseUrl: String,
    val files: List<ModelFile> = emptyList(),
    val generationSize: Int = 512,
    val textEmbeddingSize: Int = 768,
    val approximateSize: String = "1GB",
    var isDownloaded: Boolean = false,
    val isPartiallyDownloaded: Boolean = false,
    val defaultPrompt: String = "",
    val defaultNegativePrompt: String = "",
    val runOnCpu: Boolean = false,
    val useCpuClip: Boolean = false,
    val supportedHighres: List<Int> = emptyList(),
    val highresInfo: Map<String, Any> = emptyMap()
) {
// Dentro la classe StdfModel in StdfModel.kt

    // Dentro la classe StdfModel
    fun download(context: Context, hfToken: String?) {
        val workManager = WorkManager.getInstance(context)
        val gson = Gson()

        // Serializza la lista di file in una stringa JSON
        val modelFilesJson = gson.toJson(this.files)

        // Crea un SINGOLO WorkRequest per l'intero modello
        val workRequest = OneTimeWorkRequestBuilder<StdfDownloadWorker>()
            .setInputData(
                workDataOf(
                    StdfDownloadWorker.KEY_MODEL_ID to this.id,
                    StdfDownloadWorker.KEY_MODEL_FILES_JSON to modelFilesJson,
                    StdfDownloadWorker.KEY_BASE_URL to this.baseUrl,
                    StdfDownloadWorker.KEY_HF_TOKEN to hfToken
                )
            )
            // Usiamo sia l'ID del modello che un tag generico
            .addTag(this.id)
            .addTag("stdf_download_tag")
            .build()

        workManager.enqueue(workRequest)
    }
    fun deleteModel(context: Context): Boolean {
        return try {
            val modelDir = File(getModelsDir(context), id)
            val fileVerification = StdfFileVerification(context)
            fileVerification.clearVerification(id)
            if (modelDir.exists() && modelDir.isDirectory) modelDir.deleteRecursively() else false
        } catch (e: Exception) { false }
    }
    companion object {
        private const val MODELS_DIR = "models"
        fun getModelsDir(context: Context): File {
            return File(context.filesDir, MODELS_DIR).apply { if (!exists()) mkdirs() }
        }
        fun isModelDownloaded(context: Context, modelId: String, files: List<ModelFile>): Boolean {
            val modelDir = File(getModelsDir(context), modelId)
            val fileVerification = StdfFileVerification(context)
            return files.all { modelFile ->
                val file = File(modelDir, modelFile.name)
                if (file.exists()) {
                    val savedSize = fileVerification.getFileSize(modelId, modelFile.name)
                    savedSize != -1L && file.length() == savedSize
                } else { false }
            }
        }
    }
}


class StdfModelRepository(private val context: Context) {
    private val generationPreferences = ImageGenerationPreferences(context)
    val models: List<StdfModel>

    init {
        models = initializeModels()
    }

    private fun initializeModels(): List<StdfModel> {
        val baseUrl = generationPreferences.getBaseUrl()
        val modelList = listOf(
            // NPU Versions
            createAnythingV5Model(baseUrl),
            createQteaMixModel(baseUrl),
            createCuteYukiMixModel(baseUrl),
            createAbsoluteRealityModel(baseUrl),
            createChilloutMixModel(baseUrl),
            createSD21Model(baseUrl),

            // CPU Versions
            createAnythingV5ModelCPU(baseUrl),
            createQteaMixModelCPU(baseUrl),
            createCuteYukiMixModelCPU(baseUrl),
            createAbsoluteRealityModelCPU(baseUrl),
            createChilloutMixModelCPU(baseUrl)
        )
        modelList.forEach { model ->
            model.isDownloaded = StdfModel.isModelDownloaded(context, model.id, model.files)
        }
        return modelList
    }

    // --- NPU Models ---
    private fun createAnythingV5Model(baseUrl: String) = StdfModel(
        id = "anythingv5", name = "Anything V5.0 (NPU)", description = context.getString(R.string.anythingv5_description),
        baseUrl = baseUrl, files = listOf(
            ModelFile("tokenizer.json", "tokenizer", "xororz/AnythingV5/resolve/main/tokenizer.json"),
            ModelFile("clip.mnn", "clip", "xororz/AnythingV5/resolve/main/clip_fp16.mnn"),
            ModelFile("vae_encoder.bin", "vae_encoder", "xororz/AnythingV5/resolve/main/vae_encoder_${chipsetModelSuffixes[getDeviceSoc()]}.bin"),
            ModelFile("vae_decoder.bin", "vae_decoder", "xororz/AnythingV5/resolve/main/vae_decoder_${chipsetModelSuffixes[getDeviceSoc()]}.bin"),
            ModelFile("unet.bin", "unet", "xororz/AnythingV5/resolve/main/unet_${chipsetModelSuffixes[getDeviceSoc()]}.bin")
        ), approximateSize = "1.1GB", useCpuClip = true,
        defaultPrompt = "masterpiece, best quality, 1girl, solo, cute, white hair,",
        defaultNegativePrompt = "bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, missing arms, poorly drawn face, bad face, fused face, cloned face, three crus, fused feet, fused thigh, extra crus, ugly fingers, horn, realistic photo, huge eyes, worst face, 2girl, long fingers, disconnected limbs,"
    )

    private fun createQteaMixModel(baseUrl: String) = StdfModel(
        id = "qteamix", name = "QteaMix (NPU)", description = context.getString(R.string.qteamix_description),
        baseUrl = baseUrl, files = listOf(
            ModelFile("tokenizer.json", "tokenizer", "xororz/QteaMix/resolve/main/tokenizer.json"),
            ModelFile("clip.mnn", "clip", "xororz/QteaMix/resolve/main/clip_fp16.mnn"),
            ModelFile("vae_encoder.bin", "vae_encoder", "xororz/AnythingV5/resolve/main/vae_encoder_${chipsetModelSuffixes[getDeviceSoc()]}.bin"),
            ModelFile("vae_decoder.bin", "vae_decoder", "xororz/QteaMix/resolve/main/vae_decoder_${chipsetModelSuffixes[getDeviceSoc()]}.bin"),
            ModelFile("unet.bin", "unet", "xororz/QteaMix/resolve/main/unet_${chipsetModelSuffixes[getDeviceSoc()]}.bin")
        ), approximateSize = "1.1GB", useCpuClip = true,
        defaultPrompt = "chibi, best quality, 1girl, solo, cute, pink hair,",
        defaultNegativePrompt = "bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, missing arms, poorly drawn face, bad face, fused face, cloned face, three crus, fused feet, fused thigh, extra crus, ugly fingers, horn, realistic photo, huge eyes, worst face, 2girl, long fingers, disconnected limbs,"
    )

    private fun createCuteYukiMixModel(baseUrl: String) = StdfModel(
        id = "cuteyukimix", name = "CuteYukiMix (NPU)", description = context.getString(R.string.cuteyukimix_description),
        baseUrl = baseUrl, files = listOf(
            ModelFile("tokenizer.json", "tokenizer", "xororz/CuteYukiMix/resolve/main/tokenizer.json"),
            ModelFile("clip.mnn", "clip", "xororz/QteaMix/resolve/main/clip_fp16.mnn"),
            ModelFile("vae_encoder.bin", "vae_encoder", "xororz/AnythingV5/resolve/main/vae_encoder_${chipsetModelSuffixes[getDeviceSoc()]}.bin"),
            ModelFile("vae_decoder.bin", "vae_decoder", "xororz/CuteYukiMix/resolve/main/vae_decoder_${chipsetModelSuffixes[getDeviceSoc()]}.bin"),
            ModelFile("unet.bin", "unet", "xororz/CuteYukiMix/resolve/main/unet_${chipsetModelSuffixes[getDeviceSoc()]}.bin")
        ), approximateSize = "1.2GB", useCpuClip = true,
        defaultPrompt = "masterpiece, best quality, 1girl, solo, cute, white hair,",
        defaultNegativePrompt = "bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, missing arms, poorly drawn face, bad face, fused face, cloned face, three crus, fused feet, fused thigh, extra crus, ugly fingers, horn, realistic photo, huge eyes, worst face, 2girl, long fingers, disconnected limbs,"
    )

    private fun createAbsoluteRealityModel(baseUrl: String) = StdfModel(
        id = "absolutereality", name = "Absolute Reality (NPU)", description = context.getString(R.string.absolutereality_description),
        baseUrl = baseUrl, files = listOf(
            ModelFile("tokenizer.json", "tokenizer", "xororz/AbsoluteReality/resolve/main/tokenizer.json"),
            ModelFile("clip.mnn", "clip", "xororz/AbsoluteReality/resolve/main/clip_fp16.mnn"),
            ModelFile("vae_encoder.bin", "vae_encoder", "xororz/AnythingV5/resolve/main/vae_encoder_${chipsetModelSuffixes[getDeviceSoc()]}.bin"),
            ModelFile("vae_decoder.bin", "vae_decoder", "xororz/AbsoluteReality/resolve/main/vae_decoder_${chipsetModelSuffixes[getDeviceSoc()]}.bin"),
            ModelFile("unet.bin", "unet", "xororz/AbsoluteReality/resolve/main/unet_${chipsetModelSuffixes[getDeviceSoc()]}.bin")
        ), approximateSize = "1.1GB", useCpuClip = true,
        defaultPrompt = "masterpiece, best quality, ultra-detailed, realistic, 8k, a cat on grass,",
        defaultNegativePrompt = "worst quality, low quality, normal quality, poorly drawn, lowres, low resolution, signature, watermarks, ugly, out of focus, error, blurry, unclear photo, bad photo, unrealistic, semi realistic, pixelated, cartoon, anime, cgi, drawing, 2d, 3d, censored, duplicate,"
    )

    private fun createChilloutMixModel(baseUrl: String) = StdfModel(
        id = "chilloutmix", name = "ChilloutMix (NPU)", description = context.getString(R.string.chilloutmix_description),
        baseUrl = baseUrl, files = listOf(
            ModelFile("tokenizer.json", "tokenizer", "xororz/ChilloutMix/resolve/main/tokenizer.json"),
            ModelFile("clip.mnn", "clip", "xororz/ChilloutMix/resolve/main/clip_fp16.mnn"),
            ModelFile("vae_encoder.bin", "vae_encoder", "xororz/AnythingV5/resolve/main/vae_encoder_${chipsetModelSuffixes[getDeviceSoc()]}.bin"),
            ModelFile("vae_decoder.bin", "vae_decoder", "xororz/ChilloutMix/resolve/main/vae_decoder_${chipsetModelSuffixes[getDeviceSoc()]}.bin"),
            ModelFile("unet.bin", "unet", "xororz/ChilloutMix/resolve/main/unet_${chipsetModelSuffixes[getDeviceSoc()]}.bin")
        ), approximateSize = "1.1GB", useCpuClip = true,
        defaultPrompt = "RAW photo, best quality, realistic, photo-realistic, masterpiece, 1girl, upper body, facing front, portrait,",
        defaultNegativePrompt = "paintings, sketches, worst quality, low quality, normal quality, lowres, monochrome, grayscale, skin spots, acnes, skin blemishes, age spot, bad anatomy, bad hands, bad body, bad proportions, gross proportions, extra fingers, fewer fingers, extra digit, missing fingers, fused fingers, extra arms, missing arms, extra legs, missing legs, extra limbs, mutated hands, poorly drawn hands, poorly drawn face, mutation, deformed, blurry, watermark, white letters, signature, text, error, jpeg artifacts, duplicate, morbid, mutilated, cross-eyed, long neck, ng_deepnegative_v1_75t, easynegative, bad-picture-chill-75v, bad-artist"
    )

    private fun createSD21Model(baseUrl: String) = StdfModel(
        id = "sd21", name = "Stable Diffusion 2.1", description = context.getString(R.string.sd21_description),
        baseUrl = baseUrl, files = listOf(
            ModelFile("tokenizer.json", "tokenizer", "xororz/SD21/resolve/main/tokenizer.json"),
            ModelFile("clip.bin", "clip", "xororz/SD21/resolve/main/clip_${chipsetModelSuffixes[getDeviceSoc()]}.bin"),
            ModelFile("vae_encoder.bin", "vae_encoder", "xororz/AnythingV5/resolve/main/vae_encoder_${chipsetModelSuffixes[getDeviceSoc()]}.bin"),
            ModelFile("vae_decoder.bin", "vae_decoder", "xororz/SD21/resolve/main/vae_decoder_${chipsetModelSuffixes[getDeviceSoc()]}.bin"),
            ModelFile("unet.bin", "unet", "xororz/SD21/resolve/main/unet_${chipsetModelSuffixes[getDeviceSoc()]}.bin")
        ), textEmbeddingSize = 1024, approximateSize = "1.3GB",
        defaultPrompt = "a rabbit on grass,",
        defaultNegativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra digit, fewer fingers, cropped, worst quality, low quality, blur, simple background, mutation, deformed, ugly, duplicate, error, jpeg artifacts, watermark, username, blurry"
    )

    // --- CPU Models ---
    private fun createAnythingV5ModelCPU(baseUrl: String) = StdfModel(
        id = "anythingv5_cpu", name = "Anything V5.0 (CPU)", description = context.getString(R.string.anythingv5_description),
        baseUrl = baseUrl, files = listOf(
            ModelFile("tokenizer.json", "tokenizer", "xororz/AnythingV5/resolve/main/tokenizer.json"),
            ModelFile("clip.mnn", "clip", "xororz/AnythingV5/resolve/main/clip_fp16.mnn"),
            ModelFile("vae_encoder.mnn", "vae_encoder", "xororz/AnythingV5/resolve/main/vae_encoder_fp16.mnn"),
            ModelFile("vae_decoder.mnn", "vae_decoder", "xororz/AnythingV5/resolve/main/vae_decoder_fp16.mnn"),
            ModelFile("unet.mnn", "unet", "xororz/AnythingV5/resolve/main/unet_asym_block32.mnn")
        ), approximateSize = "1.2GB", runOnCpu = true,
        defaultPrompt = "masterpiece, best quality, 1girl, solo, cute, white hair,",
        defaultNegativePrompt = "bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, missing arms, poorly drawn face, bad face, fused face, cloned face, three crus, fused feet, fused thigh, extra crus, ugly fingers, horn, realistic photo, huge eyes, worst face, 2girl, long fingers, disconnected limbs,"
    )

    private fun createQteaMixModelCPU(baseUrl: String) = StdfModel(
        id = "qteamix_cpu", name = "QteaMix (CPU)", description = context.getString(R.string.qteamix_description),
        baseUrl = baseUrl, files = listOf(
            ModelFile("tokenizer.json", "tokenizer", "xororz/AnythingV5/resolve/main/tokenizer.json"),
            ModelFile("clip.mnn", "clip", "xororz/QteaMix/resolve/main/clip_fp16.mnn"),
            ModelFile("vae_encoder.mnn", "vae_encoder", "xororz/AnythingV5/resolve/main/vae_encoder_fp16.mnn"),
            ModelFile("vae_decoder.mnn", "vae_decoder", "xororz/QteaMix/resolve/main/vae_decoder_fp16.mnn"),
            ModelFile("unet.mnn", "unet", "xororz/QteaMix/resolve/main/unet_asym_block32.mnn")
        ), approximateSize = "1.2GB", runOnCpu = true,
        defaultPrompt = "chibi, best quality, 1girl, solo, cute, pink hair,",
        defaultNegativePrompt = "bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, missing arms, poorly drawn face, bad face, fused face, cloned face, three crus, fused feet, fused thigh, extra crus, ugly fingers, horn, realistic photo, huge eyes, worst face, 2girl, long fingers, disconnected limbs,"
    )

    private fun createCuteYukiMixModelCPU(baseUrl: String) = StdfModel(
        id = "cuteyukimix_cpu", name = "CuteYukiMix (CPU)", description = context.getString(R.string.cuteyukimix_description),
        baseUrl = baseUrl, files = listOf(
            ModelFile("tokenizer.json", "tokenizer", "xororz/CuteYukiMix/resolve/main/tokenizer.json"),
            ModelFile("clip.mnn", "clip", "xororz/QteaMix/resolve/main/clip_fp16.mnn"),
            ModelFile("vae_encoder.mnn", "vae_encoder", "xororz/AnythingV5/resolve/main/vae_encoder_fp16.mnn"),
            ModelFile("vae_decoder.mnn", "vae_decoder", "xororz/CuteYukiMix/resolve/main/vae_decoder_fp16.mnn"),
            ModelFile("unet.mnn", "unet", "xororz/CuteYukiMix/resolve/main/unet_asym_block32.mnn")
        ), approximateSize = "1.2GB", runOnCpu = true,
        defaultPrompt = "masterpiece, best quality, 1girl, solo, cute, white hair,",
        defaultNegativePrompt = "bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, missing arms, poorly drawn face, bad face, fused face, cloned face, three crus, fused feet, fused thigh, extra crus, ugly fingers, horn, realistic photo, huge eyes, worst face, 2girl, long fingers, disconnected limbs,"
    )

    private fun createAbsoluteRealityModelCPU(baseUrl: String) = StdfModel(
        id = "absolutereality_cpu", name = "Absolute Reality (CPU)", description = context.getString(R.string.absolutereality_description),
        baseUrl = baseUrl, files = listOf(
            ModelFile("tokenizer.json", "tokenizer", "xororz/AbsoluteReality/resolve/main/tokenizer.json"),
            ModelFile("clip.mnn", "clip", "xororz/AbsoluteReality/resolve/main/clip_fp16.mnn"),
            ModelFile("vae_encoder.mnn", "vae_encoder", "xororz/AnythingV5/resolve/main/vae_encoder_fp16.mnn"),
            ModelFile("vae_decoder.mnn", "vae_decoder", "xororz/AbsoluteReality/resolve/main/vae_decoder_fp16.mnn"),
            ModelFile("unet.mnn", "unet", "xororz/AbsoluteReality/resolve/main/unet_asym_block32.mnn")
        ), approximateSize = "1.2GB", runOnCpu = true,
        defaultPrompt = "masterpiece, best quality, ultra-detailed, realistic, 8k, a cat on grass,",
        defaultNegativePrompt = "worst quality, low quality, normal quality, poorly drawn, lowres, low resolution, signature, watermarks, ugly, out of focus, error, blurry, unclear photo, bad photo, unrealistic, semi realistic, pixelated, cartoon, anime, cgi, drawing, 2d, 3d, censored, duplicate,"
    )

    private fun createChilloutMixModelCPU(baseUrl: String) = StdfModel(
        id = "chilloutmix_cpu", name = "ChilloutMix (CPU)", description = context.getString(R.string.chilloutmix_description),
        baseUrl = baseUrl, files = listOf(
            ModelFile("tokenizer.json", "tokenizer", "xororz/ChilloutMix/resolve/main/tokenizer.json"),
            ModelFile("clip.mnn", "clip", "xororz/ChilloutMix/resolve/main/clip_fp16.mnn"),
            ModelFile("vae_encoder.mnn", "vae_encoder", "xororz/AnythingV5/resolve/main/vae_encoder_fp16.mnn"),
            ModelFile("vae_decoder.mnn", "vae_decoder", "xororz/ChilloutMix/resolve/main/vae_decoder_fp16.mnn"),
            ModelFile("unet.mnn", "unet", "xororz/ChilloutMix/resolve/main/unet_asym_block32.mnn")
        ), approximateSize = "1.2GB", runOnCpu = true,
        defaultPrompt = "RAW photo, best quality, realistic, photo-realistic, masterpiece, 1girl, upper body, facing front, portrait,",
        defaultNegativePrompt = "paintings, sketches, worst quality, low quality, normal quality, lowres, monochrome, grayscale, skin spots, acnes, skin blemishes, age spot, bad anatomy, bad hands, bad body, bad proportions, gross proportions, extra fingers, fewer fingers, extra digit, missing fingers, fused fingers, extra arms, missing arms, extra legs, missing legs, extra limbs, mutated hands, poorly drawn hands, poorly drawn face, mutation, deformed, blurry, watermark, white letters, signature, text, error, jpeg artifacts, duplicate, morbid, mutilated, cross-eyed, long neck, ng_deepnegative_v1_75t, easynegative, bad-picture-chill-75v, bad-artist"
    )
}