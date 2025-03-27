package com.rubioapps.cobblemoncompanion.ui.pokedex

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rubioapps.cobblemoncompanion.ui.theme.CobblemonCompanionTheme
import com.rubioapps.cobblemoncompanion.viewmodel.DetailedPokedexEntry
import com.rubioapps.cobblemoncompanion.viewmodel.PokedexUiState // Importar el estado

@Composable
fun PokedexScreen(uiState: PokedexUiState) { // Recibe el estado del ViewModel
    Column(
        modifier = Modifier.padding(16.dp).fillMaxSize()
    ) {
        Text(
            text = "Pokedex",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        when (uiState) {
            is PokedexUiState.Idle -> {
                Text("Importa tu archivo JSON para ver tu Pokedex")
            }
            is PokedexUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator() // Muestra un indicador de carga
                }
            }
            is PokedexUiState.Success -> {
                Text("Total Registros: ${uiState.entries.size}") // Mostrar total de detallados
                if (uiState.entries.isEmpty()) {
                    Text("No se encontraron datos de Pokémon o especies.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp) // Espacio entre items
                    ) {
                        items(uiState.entries) { detailedEntry ->
                            PokedexEntryItem(entry = detailedEntry) // Pasa el DetailedPokedexEntry
                        }
                    }
                }
            }
            is PokedexUiState.Error -> {
                Text(
                    text = "Error: ${uiState.message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun PokedexEntryItem(entry: DetailedPokedexEntry) {
    val alpha = if (entry.userEntry.captured) 1f else 0.6f // Menos opaco si no está capturado

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha) // Aplica transparencia si no está capturado
    ) {
        Row( // Usamos Row para poner imagen y texto lado a lado
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder para la imagen (Necesitarás una URL o recurso)
            // AsyncImage(
            //     model = "URL_DE_LA_IMAGEN/${entry.speciesData.name}.png", // Construye la URL
            //     contentDescription = entry.speciesData.name,
            //     modifier = Modifier.size(64.dp).padding(end = 8.dp)
            // )

            // Indicador simple de captura
            Text(
                text = if (entry.userEntry.captured) "✔️" else "❓",
                modifier = Modifier.padding(end = 8.dp)
            )


            Column(modifier = Modifier.weight(1f)) { // Column para el texto
                Text(
                    text = "#${entry.speciesData.nationalPokedexNumber} ${entry.speciesData.name.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text("Tipo(s): ${entry.speciesData.primaryType}", style = MaterialTheme.typography.bodyMedium)
                    entry.speciesData.secondaryType?.let {
                        Text(", $it", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (entry.userEntry.captured && entry.userEntry.aspects.isNotEmpty()) {
                    Text("Aspectos: ${entry.userEntry.aspects.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                }
                // Aquí puedes añadir más detalles
            }
        }
    }
}

@Preview(showBackground = true, name = "Idle State")
@Composable
fun PokedexScreenPreviewIdle() {
    CobblemonCompanionTheme { // Asegúrate de envolver en tu tema
        PokedexScreen(uiState = PokedexUiState.Idle)
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
fun PokedexScreenPreviewLoading() {
    CobblemonCompanionTheme {
        PokedexScreen(uiState = PokedexUiState.Loading)
    }
}