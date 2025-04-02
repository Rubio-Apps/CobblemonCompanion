package com.rubioapps.cobblemoncompanion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rubioapps.cobblemoncompanion.model.ChatMessage
import com.rubioapps.cobblemoncompanion.viewmodel.ChatbotViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.CircularProgressIndicator
import com.rubioapps.cobblemoncompanion.viewmodel.PokedexViewModel

@Composable
fun ChatbotScreen(
    // Recibe la instancia de PokedexViewModel del NavHost
    pokedexViewModel: PokedexViewModel,
    // Obtiene su propio ChatbotViewModel usando Hilt
    viewModel: ChatbotViewModel = hiltViewModel()
) {
    // Lee el estado de Pokedex reactivamente desde el ViewModel pasado
    val pokedexState by pokedexViewModel.pokedexState

    // Lee el estado del Chatbot reactivamente
    val uiState by viewModel.uiState
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Efecto para hacer scroll automático al final cuando llegan nuevos mensajes
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch { // Asegúrate de lanzar corutina para animateScrollToItem
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Lista de mensajes
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f) // Ocupa el espacio disponible
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), // Espacio entre burbujas
            contentPadding = PaddingValues(bottom = 8.dp) // Espacio al final
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                ChatMessageBubble(message = message)
            }
            // Muestra el indicador de carga si el ViewModel está ocupado
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // Fila para el campo de texto y el botón de enviar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Escribe tu pregunta...") },
                modifier = Modifier.weight(1f), // Ocupa el espacio restante
                maxLines = 3 // Permite hasta 3 líneas antes de hacer scroll
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        // Pasa el texto y el ESTADO ACTUAL de Pokedex al ViewModel
                        viewModel.sendMessage(inputText, pokedexState) // Llama a la versión correcta
                        inputText = "" // Limpia el campo
                    }
                },
                // El botón se deshabilita si no hay texto o si el bot está cargando
                enabled = inputText.isNotBlank() && !uiState.isLoading
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Enviar mensaje")
            }
        }
    }
}

// Composable para la burbuja de chat (sin cambios)
@Composable
fun ChatMessageBubble(message: ChatMessage) {
    // ... (código de la burbuja sin cambios) ...
    val backgroundColor = if (message.isFromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (message.isFromUser) 40.dp else 0.dp,
                end = if (message.isFromUser) 0.dp else 40.dp
            )
    ) {
        Text(
            text = message.text,
            modifier = Modifier
                .align(alignment)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isFromUser) 16.dp else 0.dp,
                        bottomEnd = if (message.isFromUser) 0.dp else 16.dp
                    )
                )
                .background(backgroundColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}