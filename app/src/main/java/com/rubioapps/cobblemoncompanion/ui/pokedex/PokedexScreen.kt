package com.rubioapps.cobblemoncompanion.ui.pokedex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rubioapps.cobblemoncompanion.model.DetailedPokedexEntry
import com.rubioapps.cobblemoncompanion.viewmodel.PokedexUiState
import com.rubioapps.cobblemoncompanion.viewmodel.PokedexViewModel
import java.util.Locale

@Composable
fun PokedexScreen(
    uiState: PokedexUiState,
    onPokemonClick: (String) -> Unit,
    viewModel: PokedexViewModel // Recibe el ViewModel
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
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
                    CircularProgressIndicator()
                }
            }
            is PokedexUiState.Success -> {
                if (uiState.selectedGeneration == null) {
                    // Muestra selector de generaciones
                    GenerationSelector(
                        generations = uiState.allEntries.map { it.speciesData.generation }.distinct().sorted(),
                        onGenerationSelected = { gen -> viewModel.selectGeneration(gen) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Selecciona una generación arriba para ver los Pokémon.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Muestra botón para volver y grid de la generación
                    Button(onClick = { viewModel.selectGeneration(null) }) {
                        Text("<< Mostrar Todas las Generaciones")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Generación ${uiState.selectedGeneration}", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.displayedEntries.isEmpty()) {
                        Text("No hay Pokémon listados para la Generación ${uiState.selectedGeneration}.")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = uiState.displayedEntries,
                                key = { detailedEntry -> detailedEntry.speciesData.nationalPokedexNumber }
                            ) { detailedEntry ->
                                PokedexEntryItem(
                                    entry = detailedEntry,
                                    onItemClick = onPokemonClick
                                )
                            }
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
fun GenerationSelector(
    generations: List<Int>,
    onGenerationSelected: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Selecciona una Generación:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(generations) { gen ->
                Button(onClick = { onGenerationSelected(gen) }) {
                    Text("Gen $gen")
                }
            }
        }
    }
}

@Composable
fun PokedexEntryItem(
    entry: DetailedPokedexEntry,
    onItemClick: (String) -> Unit
) {
    val alpha = if (entry.userEntry.captured) 1f else 0.6f
    val context = LocalContext.current
    val imageUrl = "https://cobblemon.tools/pokedex/pokemon/${entry.speciesData.name.lowercase(Locale.ROOT)}/sprite.png"

    Column(
        modifier = Modifier
            .alpha(alpha)
            .clickable { onItemClick(entry.speciesData.name) }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                // Añade placeholders si los tienes en res/drawable
                // .placeholder(R.drawable.placeholder_pokeball)
                // .error(R.drawable.error_image)
                .build(),
            contentDescription = entry.speciesData.name,
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 4.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = entry.speciesData.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
/* TODO Arreglar todo el tema de los previews con datos falsos
// --- Previews Arreglados ---

// ViewModel falso para usar en Previews
class FakePokedexViewModel : PokedexViewModel(
    speciesRepository = FakeSpeciesRepository(), // Necesitas un repo falso
    gson = Gson(), // Puedes usar Gson real
    contentResolver = FakeContentResolver() // Necesitas un ContentResolver falso
) {
    // Sobrescribe o controla el estado como necesites para el preview
}

// Implementaciones falsas muy básicas (solo para que compile el preview)
class FakeSpeciesRepository : SpeciesRepository(AssetManager(), Gson()) { // Cuidado con AssetManager real
    override suspend fun getAllSpecies(): Map<String, SpeciesData> = emptyMap()
    override suspend fun getSpeciesByName(name: String): SpeciesData? = null
}
class FakeContentResolver : ContentResolver(null) { /* Implementación vacía o mínima */ }


@Preview(showBackground = true, name = "Idle State")
@Composable
private fun PokedexScreenPreviewIdle() { // Hacer privados los previews
    CobblemonCompanionTheme {
        // No necesita ViewModel si el estado es Idle o Error
        PokedexScreen(uiState = PokedexUiState.Idle, onPokemonClick = {}, viewModel = FakePokedexViewModel()) // Pasa fake VM
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
private fun PokedexScreenPreviewLoading() {
    CobblemonCompanionTheme {
        PokedexScreen(uiState = PokedexUiState.Loading, onPokemonClick = {}, viewModel = FakePokedexViewModel())
    }
}

@Preview(showBackground = true, name = "Success State - No Gen Selected")
@Composable
private fun PokedexScreenPreviewSuccessNoGen() {
    val fakeStats = Stats(50, 50, 50, 50, 50, 50) // Crear Stats falsos
    val fakeEntries = listOf(
        DetailedPokedexEntry(PokedexEntryDisplay("bulbasaur", true, listOf("aspect1")), SpeciesData("bulbasaur", 1, 1, "grass", "poison", listOf("overgrow"), fakeStats, 45, 0.875f, 0.125f, emptyList(), emptyList())),
        DetailedPokedexEntry(PokedexEntryDisplay("ivysaur", false, emptyList()), SpeciesData("ivysaur", 2, 1, "grass", "poison", listOf("overgrow"), fakeStats, 45, 0.875f, 0.125f, emptyList(), emptyList())),
        DetailedPokedexEntry(PokedexEntryDisplay("charmander", true, emptyList()), SpeciesData("charmander", 4, 1, "fire", null, listOf("blaze"), fakeStats, 45, 0.875f, 0.125f, emptyList(), emptyList()))
    )
    CobblemonCompanionTheme {
        PokedexScreen(
            uiState = PokedexUiState.Success(
                allEntries = fakeEntries,
                displayedEntries = fakeEntries, // Muestra todo inicialmente
                selectedGeneration = null
            ),
            onPokemonClick = {},
            viewModel = FakePokedexViewModel() // Pasa fake VM
        )
    }
}

@Preview(showBackground = true, name = "Success State - Gen 1 Selected")
@Composable
private fun PokedexScreenPreviewSuccessGen1() {
    val fakeStats = Stats(50, 50, 50, 50, 50, 50)
    val fakeAllEntries = listOf(
        DetailedPokedexEntry(PokedexEntryDisplay("bulbasaur", true, listOf("aspect1")), SpeciesData("bulbasaur", 1, 1, "grass", "poison", listOf("overgrow"), fakeStats, 45, 0.875f, 0.125f, emptyList(), emptyList())),
        DetailedPokedexEntry(PokedexEntryDisplay("ivysaur", false, emptyList()), SpeciesData("ivysaur", 2, 1, "grass", "poison", listOf("overgrow"), fakeStats, 45, 0.875f, 0.125f, emptyList(), emptyList())),
        DetailedPokedexEntry(PokedexEntryDisplay("cyndaquil", true, emptyList()), SpeciesData("cyndaquil", 155, 2, "fire", null, listOf("blaze"), fakeStats, 45, 0.875f, 0.125f, emptyList(), emptyList())) // Gen 2
    )
    val fakeDisplayedEntries = fakeAllEntries.filter { it.speciesData.generation == 1 } // Filtra para Gen 1
    CobblemonCompanionTheme {
        PokedexScreen(
            uiState = PokedexUiState.Success(
                allEntries = fakeAllEntries,
                displayedEntries = fakeDisplayedEntries, // Muestra solo Gen 1
                selectedGeneration = 1
            ),
            onPokemonClick = {},
            viewModel = FakePokedexViewModel()
        )
    }
}

@Preview(showBackground = true, name = "Error State")
@Composable
private fun PokedexScreenPreviewError() {
    CobblemonCompanionTheme {
        PokedexScreen(uiState = PokedexUiState.Error("Failed to load data."), onPokemonClick = {}, viewModel = FakePokedexViewModel())
    }
}

// Nota: Los previews con clases Fake pueden requerir ajustes dependiendo de tu configuración exacta.
// Y necesitarás importar las clases Stats, AssetManager, ContentResolver etc.

*/