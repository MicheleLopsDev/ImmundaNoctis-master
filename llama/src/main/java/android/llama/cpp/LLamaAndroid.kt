package android.llama.cpp

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class LLamaAndroid private constructor() { // Costruttore privato per forzare il singleton
    private val tag: String? = this::class.simpleName
    var nlen: Int = 4096


    @Volatile
    private var isLoad: Boolean = false

    private val runLoop: CoroutineDispatcher = Executors.newSingleThreadExecutor {
        thread(start = false, name = "Llm-RunLoop") {
            Log.d(tag, "Dedicated thread for native code: ${Thread.currentThread().name}")
            System.loadLibrary("llama-android")
            log_to_android()
            backend_init(false)
            Log.d(tag, system_info())
            it.run()
        }.apply {
            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, exception: Throwable ->
                Log.e(tag, "Unhandled exception", exception)
            }
        }
    }.asCoroutineDispatcher()



    private external fun log_to_android()
    private external fun load_model(filename: String): Long
    private external fun free_model(model: Long)
    private external fun new_context(model: Long): Long
    private external fun free_context(context: Long)
    private external fun backend_init(numa: Boolean)
    private external fun backend_free()
    private external fun new_batch(nTokens: Int, embd: Int, nSeqMax: Int): Long
    private external fun free_batch(batch: Long)

    // Sostituisci la vecchia dichiarazione di new_sampler con questa
    private external fun new_sampler(
        temperature: Float,
        repeat_penalty: Float,
        top_k: Int,
        top_p: Float
    ): Long
    private external fun free_sampler(sampler: Long)
    private external fun bench_model(context: Long, model: Long, batch: Long, pp: Int, tg: Int, pl: Int, nr: Int): String
    private external fun system_info(): String
    private external fun completion_init(context: Long, batch: Long, text: String, formatChat: Boolean, nLen: Int): Int
    private external fun completion_loop(context: Long, batch: Long, sampler: Long, nLen: Int, ncur: IntVar): String?
    private external fun kv_cache_clear(context: Long)

    suspend fun load(
        pathToModel: String,
        temperature: Float,
        repeatPenalty: Float,
        topK: Int,
        topP: Float
    ) {
        withContext(runLoop) {
            if (!isLoad) {
                val model = load_model(pathToModel)
                if (model == 0L)  throw IllegalStateException("load_model() failed")

                val context = new_context(model)
                if (context == 0L) throw IllegalStateException("new_context() failed")

                val batch = new_batch(512, 0, 1)
                if (batch == 0L) throw IllegalStateException("new_batch() failed")

                // Adesso usa i parametri passati dall'esterno!
                val sampler = new_sampler(temperature, repeatPenalty, topK, topP)
                if (sampler == 0L) throw IllegalStateException("new_sampler() failed")

                Log.i(tag, "Loaded model $pathToModel")
                state = State.Loaded(model, context, batch, sampler)
                isLoad = true
            }
        }
    }

    suspend fun unload() {
        withContext(runLoop) {
            if (state is State.Loaded) {
                val loadedState = state as State.Loaded
                free_context(loadedState.context)
                free_model(loadedState.model)
                free_batch(loadedState.batch)
                free_sampler(loadedState.sampler);
                isLoad = false
                state = State.Idle
            }
        }
    }

    // Aggiungi questa nuova funzione pubblica alla classe LLamaAndroid
    suspend fun setSamplingParams(
        temperature: Float,
        repeatPenalty: Float,
        topK: Int,
        topP: Float
    ) {
        withContext(runLoop) {
            val currentState = state
            if (currentState is State.Loaded) {
                // Liberiamo il vecchio campionatore per evitare memory leak
                free_sampler(currentState.sampler)

                // Creiamo quello nuovo con i parametri corretti passati dalla UI
                val newSampler = new_sampler(temperature, repeatPenalty, topK, topP)
                if (newSampler == 0L) throw IllegalStateException("new_sampler() with params failed")

                // Aggiorniamo lo stato con il nuovo campionatore
                state = currentState.copy(sampler = newSampler)
                Log.i(tag, "Sampling parameters updated: temp=$temperature, penalty=$repeatPenalty")
            } else {
                Log.w(tag, "Model not loaded, cannot set sampling params.")
            }
        }
    }

    fun send(message: String, formatChat: Boolean = false): Flow<String> = flow {
        when (val currentState = state) { // Usa la variabile di stato globale
            is State.Loaded -> {
                val ncur = IntVar(completion_init(currentState.context, currentState.batch, message, formatChat, nlen))
                while (ncur.value <= nlen) {
                    val str = completion_loop(currentState.context, currentState.batch, currentState.sampler, nlen, ncur)
                    if (str == null) break
                    emit(str)
                }
                kv_cache_clear(currentState.context)
            }
            else -> {}
        }
    }.flowOn(runLoop)

    companion object {
        // --- LOGICA DELLO STATO SPOSTATA QUI (STATICA) ---

        sealed interface State {
            data object Idle: State
            data class Loaded(val model: Long, val context: Long, val batch: Long, val sampler: Long): State
        }

        // Variabile di stato "statica", condivisa e sicura per i thread
        @Volatile
        private var state: State = State.Idle

        // Proprietà pubblica per leggere lo stato in modo sicuro
        val currentState: State
            get() = state

        // --- FINE LOGICA STATO ---

        private class IntVar(value: Int) {
            @Volatile
            var value: Int = value
                private set

            fun inc() {
                synchronized(this) {
                    value += 1
                }
            }
        }

        private val _instance: LLamaAndroid = LLamaAndroid()
        fun instance(): LLamaAndroid = _instance
    }
}
