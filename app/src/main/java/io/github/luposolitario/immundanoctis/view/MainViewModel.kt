package io.github.luposolitario.immundanoctis.view

import android.llama.cpp.LLamaAndroid
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.luposolitario.immundanoctis.data.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object CharacterID {
    const val HERO = "hero"
    const val DM = "dm"
}

class MainViewModel(private val llamaAndroid: LLamaAndroid = LLamaAndroid.instance()) : ViewModel() {
    private val tag: String? = this::class.simpleName

    // --- PROPRIETÀ PER LA CHAT ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow() // <-- QUESTA È LA PROPRIETÀ CHIAVE

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    // ---

    private val _logMessages = MutableStateFlow<List<String>>(listOf("Initializing..."))
    val logMessages: StateFlow<List<String>> = _logMessages.asStateFlow()

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                llamaAndroid.unload()
            } catch (exc: IllegalStateException) {
                log(exc.message ?: "Error on unload")
            }
        }
    }

    fun sendMessage(text: String) {
        if (_isGenerating.value) return

        val userMessage = ChatMessage(authorId = CharacterID.HERO, text = text)
        _chatMessages.update { it + userMessage }

        viewModelScope.launch {
            _isGenerating.value = true
            _streamingText.value = ""

            llamaAndroid.send(text)
                .catch { error ->
                    Log.e(tag, "send() failed", error)
                    log(error.message ?: "Unknown error during send")
                }
                .onCompletion {
                    if (_streamingText.value.isNotBlank()) {
                        val finalMessage = ChatMessage(authorId = CharacterID.DM, text = _streamingText.value)
                        _chatMessages.update { it + finalMessage }
                    }
                    _streamingText.value = ""
                    _isGenerating.value = false
                }
                .collect { token ->
                    _streamingText.update { it + token }
                }
        }
    }

    fun load(pathToModel: String) {
        viewModelScope.launch {
            try {
                llamaAndroid.load(pathToModel)
                log("Loaded $pathToModel")
            } catch (exc: IllegalStateException) {
                Log.e(tag, "load() failed", exc)
                log(exc.message ?: "Failed to load model")
            }
        }
    }

    fun log(message: String) {
        _logMessages.update { it + message }
    }
}
