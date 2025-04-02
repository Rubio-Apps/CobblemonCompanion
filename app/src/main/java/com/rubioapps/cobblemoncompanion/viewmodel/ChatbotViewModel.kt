package com.rubioapps.cobblemoncompanion.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubioapps.cobblemoncompanion.model.ChatMessage
import com.rubioapps.cobblemoncompanion.service.GeminiChatService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// Estado de la UI del Chatbot
data class ChatbotUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false // Para futuras llamadas a API, etc.
)

@HiltViewModel
class ChatbotViewModel @Inject constructor(
    private val geminiChatService: GeminiChatService // Solo inyecta el servicio
    // --- PokedexViewModel ELIMINADO del constructor ---
) : ViewModel() {

    private val _uiState = mutableStateOf(ChatbotUiState())
    val uiState: State<ChatbotUiState> = _uiState
    private val TAG = "ChatbotViewModel"

    init {
        addMessage("¡Hola! Soy tu asistente Cobblemon (con tecnología Gemini). Pregúntame algo.", false)
    }

    // --- MODIFICADO: sendMessage AHORA recibe el contexto ---
    fun sendMessage(userText: String, capturedCount: Int?, totalCount: Int?) {
        if (userText.isBlank()) return
        addMessage(userText, true)

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            // --- Usa los parámetros para generar el contexto ---
            val contextPromptPart = generateContextPrompt(capturedCount, totalCount)
            // --- Fin uso de parámetros ---

            val fullPrompt = """
                Eres un asistente experto y amigable para el mod Cobblemon de Minecraft.
                $contextPromptPart
                El usuario dice: "$userText"
                Responde de forma concisa y útil, como si hablaras con un jugador. No uses markdown complejo.
            """.trimIndent()

            val result = geminiChatService.generateResponse(fullPrompt)

            _uiState.value = _uiState.value.copy(isLoading = false)

            result.onSuccess { botResponseText ->
                addMessage(botResponseText, false)
            }.onFailure { exception ->
                Log.e(TAG, "Error getting response from Gemini", exception)
                addMessage("Lo siento, tuve un problema para conectarme (${exception.message}). Inténtalo de nuevo.", false)
            }
        }
    }

    // Añade un mensaje a la lista (sin cambios)
    private fun addMessage(text: String, isUser: Boolean) {
        val newMessage = ChatMessage(text = text, isFromUser = isUser)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + newMessage
        )
    }

    // --- MODIFICADO: generateContextPrompt AHORA recibe los datos ---
    private fun generateContextPrompt(capturedCount: Int?, totalCount: Int?): String {
        return if (capturedCount != null && totalCount != null && totalCount > 0) {
            "Contexto actual del jugador: Ha registrado $capturedCount de $totalCount Pokémon en su Pokedex."
        } else {
            "Contexto actual del jugador: No disponible."
        }
    }
}