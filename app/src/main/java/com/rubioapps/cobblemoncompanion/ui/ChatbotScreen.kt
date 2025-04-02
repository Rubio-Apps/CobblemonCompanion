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
import androidx.compose.material3.CircularProgressIndicator // Para el loading
import com.rubioapps.cobblemoncompanion.viewmodel.PokedexViewModel // Importar PokedexViewModel
import com.rubioapps.cobblemoncompanion.viewmodel.PokedexUiState // Importar PokedexUiState

@Composable
fun ChatbotScreen(
    viewModel: ChatbotViewModel = hiltViewModel() // Obtiene el ChatbotViewModel
) {
    // --- Obtener PokedexViewModel para leer su estado ---
    val pokedexViewModel: PokedexViewModel = hiltViewModel()
    val pokedexState by pokedexViewModel.pokedexState
    // --- Fin obtener PokedexViewModel ---

    val uiState by viewModel.uiState
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()


    // Hacer scroll al final cuando se añaden mensajes
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                ChatMessageBubble(message = message)
            }
            if (uiState.isLoading) {
                item { /* ... Indicador de carga ... */ }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Escribe tu pregunta...") },
                modifier = Modifier.weight(1f),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    // --- Extraer contexto y pasar a sendMessage ---
                    var capturedCount: Int? = null
                    var totalCount: Int? = null
                    if (pokedexState is PokedexUiState.Success) {
                        val successState = pokedexState as PokedexUiState.Success
                        capturedCount = successState.allEntries.count { it.userEntry.captured }
                        totalCount = successState.allEntries.size
                    }
                    viewModel.sendMessage(inputText, capturedCount, totalCount)
                    // --- Fin extraer contexto ---
                    inputText = ""
                },
                enabled = inputText.isNotBlank() && !uiState.isLoading
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Enviar mensaje")
            }
        }
    }
}


// Composable para mostrar una burbuja de mensaje
@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val backgroundColor = if (message.isFromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier
            .fillMaxWidth() // Ocupa todo el ancho para alinear la burbuja
            .padding(
                start = if (message.isFromUser) 40.dp else 0.dp, // Margen izquierdo si es del usuario
                end = if (message.isFromUser) 0.dp else 40.dp // Margen derecho si es del bot
            )
    ) {
        Text(
            text = message.text,
            modifier = Modifier
                .align(alignment) // Alinea la burbuja
                .clip(
                    RoundedCornerShape( // Esquinas redondeadas
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isFromUser) 16.dp else 0.dp,
                        bottomEnd = if (message.isFromUser) 0.dp else 16.dp
                    )
                )
                .background(backgroundColor)
                .padding(horizontal = 12.dp, vertical = 8.dp) // Padding interno
        )
    }
}