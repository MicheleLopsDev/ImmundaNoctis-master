package io.github.luposolitario.stdf

import android.content.res.AssetManager
import android.graphics.Bitmap

class StdfEngine {

    /**
     * Carica i modelli dalla cartella assets dell'app.
     * Restituisce true se il caricamento ha successo, altrimenti false.
     * @param assetManager L'AssetManager dell'applicazione, necessario per accedere ai file in assets.
     * @param modelDir La sottocartella in assets dove si trovano i modelli (es. "models").
     */
    external fun loadModel(assetManager: AssetManager, modelDir: String): Boolean

    /**
     * Genera un'immagine a partire da un prompt.
     * Restituisce un oggetto Bitmap se ha successo, altrimenti null.
     * @param prompt Il prompt positivo.
     * @param negativePrompt Il prompt negativo.
     * @param steps Il numero di step di diffusione.
     * @param seed Il seed per la generazione casuale.
     */
    external fun generateImage(prompt: String, negativePrompt: String, steps: Int, seed: Int): Bitmap?

    /**
     * Rilascia i modelli dalla memoria.
     */
    external fun unloadModel()

    companion object {
        // Carica la nostra libreria nativa C++
        init {
            System.loadLibrary("stable_diffusion_core")
        }
    }
}