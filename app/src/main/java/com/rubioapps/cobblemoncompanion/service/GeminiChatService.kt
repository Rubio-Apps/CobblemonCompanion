package com.rubioapps.cobblemoncompanion.service

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.asTextOrNull
import com.google.ai.client.generativeai.type.generationConfig
import com.rubioapps.cobblemoncompanion.BuildConfig // Para acceder a la clave API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Para que Hilt gestione una única instancia
class GeminiChatService @Inject constructor() {

    private val TAG = "GeminiChatService"

    // Configuración opcional del modelo (puedes ajustarla)
    private val generationConfig = generationConfig {
        temperature = 0.7f // Controla la aleatoriedad (0=determinista, 1=más creativo)
        topK = 40        // Considera las 40 palabras más probables
        topP = 0.95f       // Considera palabras hasta sumar 95% de probabilidad
        maxOutputTokens = 1024 // Límite de longitud de la respuesta
        // stopSequences = listOf("...") // Palabras que detienen la generación
    }

    // Inicializa el modelo Gemini
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash-latest", // Modelo eficiente para chat rápido
        // modelName = "gemini-pro", // Modelo más general
        apiKey = BuildConfig.GEMINI_API_KEY, // Obtiene la clave desde BuildConfig
        generationConfig = generationConfig
        // safetySettings = ... // Puedes añadir configuración de seguridad
    )

    suspend fun generateResponse(prompt: String): Result<String> {
        Log.d(TAG, "Sending prompt to Gemini: \"$prompt\"")
        return try {
            val response = generativeModel.generateContent(prompt)
            val responseText = response.text // Intenta obtener el texto directamente
            if (responseText != null) {
                Log.d(TAG, "Received response from Gemini: \"$responseText\"")
                Result.success(responseText)
            } else {
                val fullResponse = response.candidates.joinToString { it.content.parts.joinToString { p -> p.asTextOrNull() ?: "" } } ?: "No text content found in response."
                Log.w(TAG, "Gemini response text was null. Full response parts: $fullResponse")
                // Intenta obtenerlo de las partes si response.text es null
                val textFromParts = response.candidates.firstOrNull()?.content?.parts?.joinToString("") { it.asTextOrNull() ?: "" }
                if (!textFromParts.isNullOrBlank()) {
                    Result.success(textFromParts)
                } else {
                    Result.failure(Exception("Respuesta de Gemini vacía o sin texto: $fullResponse"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error interacting with Gemini API", e)
            Result.failure(e) // Envuelve la excepción en un Result.failure
        }
    }
}

// Result es una clase integrada en Kotlin para manejar éxito o fallo
// Ejemplo de uso:
// val result = geminiChatService.generateResponse("Hola")
// result.onSuccess { text -> println("Éxito: $text") }
// result.onFailure { exception -> println("Error: ${exception.message}") }