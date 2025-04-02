package com.rubioapps.cobblemoncompanion.viewmodel //

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubioapps.cobblemoncompanion.model.ChatMessage // Asegúrate de importar tu modelo ChatMessage
import com.rubioapps.cobblemoncompanion.service.GeminiChatService // Importa tu servicio Gemini
// PokedexUiState ya está definida en este mismo paquete (en PokedexViewModel.kt)
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Locale // Para capitalize
import javax.inject.Inject

// Estado de la UI del Chatbot
data class ChatbotUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ChatbotViewModel @Inject constructor(
    private val geminiChatService: GeminiChatService // Solo inyecta el servicio Gemini
) : ViewModel() {

    private val _uiState = mutableStateOf(ChatbotUiState())
    val uiState: State<ChatbotUiState> = _uiState
    private val TAG = "ChatbotViewModel" // Para logs

    init {
        // Mensaje inicial
        addMessage("¡Hola! Soy tu asistente Cobblemon (con tecnología Gemini). Pregúntame algo.", false)
    }

    /**
     * Envía un mensaje del usuario, genera el contexto desde el estado de Pokedex proporcionado,
     * y obtiene una respuesta del servicio Gemini.
     *
     * @param userText El texto introducido por el usuario.
     * @param currentPokedexState El estado actual de PokedexUiState para generar contexto.
     */
    fun sendMessage(userText: String, currentPokedexState: PokedexUiState?) {
        if (userText.isBlank()) return
        addMessage(userText, true) // Muestra el mensaje del usuario inmediatamente

        _uiState.value = _uiState.value.copy(isLoading = true) // Muestra indicador de carga

        viewModelScope.launch {
            // Genera el contexto justo antes de la llamada, usando el estado más reciente pasado
            val contextPromptPart = generateContextPrompt(currentPokedexState)
            // Log.d(TAG, "[SendMessageCoroutine] Generated contextPromptPart: '$contextPromptPart'") // Log opcional

            // Construye el prompt completo para Gemini
            val fullPrompt = """
                Eres un asistente experto y amigable para el mod Cobblemon de Minecraft. Tu objetivo principal es responder las preguntas del usuario sobre el juego basándote en la información proporcionada.

                --- INICIO CONTEXTO DEL JUGADOR ---
                $contextPromptPart
                --- FIN CONTEXTO DEL JUGADOR ---

                El usuario pregunta: "$userText"

                Responde de forma directa a la pregunta del usuario. Si la pregunta se relaciona con el contexto proporcionado (como el número de Pokémon capturados), asegúrate de usar esa información en tu respuesta. Sé conciso y amigable. No uses markdown complejo en la respuesta.
            """.trimIndent()

            Log.d(TAG, "[SendMessage] Sending full prompt to Gemini...") // Log del prompt
            // Log.v(TAG, fullPrompt) // Descomenta para ver el prompt completo si es necesario

            // Llama al servicio Gemini
            val result = geminiChatService.generateResponse(fullPrompt)

            _uiState.value = _uiState.value.copy(isLoading = false) // Oculta indicador de carga

            // Procesa el resultado de Gemini
            result.onSuccess { botResponseText ->
                addMessage(botResponseText, false) // Muestra la respuesta del bot
            }.onFailure { exception ->
                Log.e(TAG, "Error getting response from Gemini", exception)
                addMessage("Lo siento, hubo un problema al generar la respuesta (${exception.message}). Inténtalo de nuevo.", false)
            }
        }
    }

    // Añade un mensaje a la lista de mensajes del estado de la UI
    private fun addMessage(text: String, isUser: Boolean) {
        val newMessage = ChatMessage(text = text, isFromUser = isUser)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + newMessage
        )
    }

    /**
     * Genera una cadena de texto describiendo el contexto del jugador basado
     * en el estado actual de la Pokedex.
     *
     * @param pokedexState El estado actual de PokedexUiState.
     * @return Una cadena de texto formateada para el prompt de Gemini.
     */
    private fun generateContextPrompt(pokedexState: PokedexUiState?): String {
        // Log.d(TAG, "[GenerateContext] Generating context based on state type: ${pokedexState?.let { it::class.simpleName } ?: "NULL"}") // Log opcional
        return when (pokedexState) {
            is PokedexUiState.Success -> {
                val totalCaptured = pokedexState.allEntries.count { it.userEntry.captured }
                val totalSpecies = pokedexState.allEntries.size
                // Obtiene una muestra de Pokémon capturados recientemente (por número Pokedex)
                val capturedPokemonSample = pokedexState.allEntries
                    .filter { it.userEntry.captured }
                    .takeLast(5) // Toma los últimos 5 de la lista ordenada
                    .joinToString(", ") { it.speciesData.name.capitalize() }

                // Construye el texto del contexto
                var contextString = "Estado actual: $totalCaptured de $totalSpecies Pokémon registrados."
                if (capturedPokemonSample.isNotBlank()) {
                    contextString += " Algunos registros recientes: $capturedPokemonSample."
                }
                // Log.d(TAG, "[GenerateContext] State is Success. Calculated context: '$contextString'") // Log opcional
                contextString
            }
            is PokedexUiState.Loading -> "Estado actual: Cargando datos de Pokedex..."
            is PokedexUiState.Error -> "Estado actual: Error al cargar datos de Pokedex (${pokedexState.message})."
            else -> "Estado actual: Datos de Pokedex no disponibles (Importa tu archivo JSON en Ajustes)." // Idle o Null
        }
    }
}

// Helper para capitalizar (colócalo en un archivo Util.kt si prefieres)
fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}