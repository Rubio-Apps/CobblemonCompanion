package com.rubioapps.cobblemoncompanion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Icono de atrás
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle // Para obtener argumentos de navegación
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.rubioapps.cobblemoncompanion.model.species.SpeciesData
import com.rubioapps.cobblemoncompanion.repository.SpeciesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

// ViewModel específico para la pantalla de detalle (opcional pero recomendado)
@HiltViewModel
class PokemonDetailViewModel @Inject constructor(
    private val speciesRepository: SpeciesRepository,
    savedStateHandle: SavedStateHandle // Para obtener el argumento pokemonName
) : ViewModel() {

    // Obtiene el nombre del Pokémon del argumento de navegación
    private val pokemonName: String = checkNotNull(savedStateHandle["pokemonName"])

    private val _speciesData = mutableStateOf<SpeciesData?>(null)
    val speciesData: State<SpeciesData?> = _speciesData

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    init {
        loadPokemonDetails()
    }

    private fun loadPokemonDetails() {
        viewModelScope.launch {
            _isLoading.value = true
            // Podríamos buscar en los datos ya cargados por PokedexViewModel si fuera accesible,
            // pero buscar directamente en el repo es más encapsulado para esta pantalla.
            _speciesData.value = speciesRepository.getSpeciesByName(pokemonName)
            _isLoading.value = false
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class) // Para TopAppBar
@Composable
fun PokemonDetailScreen(
    viewModel: PokemonDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit // Lambda para volver atrás
) {
    val speciesData by viewModel.speciesData
    val isLoading by viewModel.isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(speciesData?.name?.replaceFirstChar { it.uppercase() } ?: "Cargando...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp), // Padding adicional para el contenido
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (speciesData != null) {
                // Muestra los detalles del Pokémon
                PokemonDetailsContent(species = speciesData!!)
            } else {
                Text("No se encontraron datos para este Pokémon.")
            }
        }
    }
}

@Composable
fun PokemonDetailsContent(species: SpeciesData) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Imagen grande
        AsyncImage(
            model = "https://cobblemon.tools/pokedex/pokemon/${species.name.lowercase(Locale.ROOT)}/sprite.png",
            contentDescription = species.name,
            modifier = Modifier.size(200.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "#${species.nationalPokedexNumber} ${species.name.replaceFirstChar { it.uppercase() }}",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Text("Tipo(s): ${species.primaryType}", style = MaterialTheme.typography.bodyLarge)
            species.secondaryType?.let {
                Text(", $it", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Aquí puedes añadir más secciones: Estadísticas, Habilidades, Evoluciones, Descripción...
        Text("Estadísticas Base:", style = MaterialTheme.typography.titleMedium)
        Text("HP: ${species.baseStats.hp}")
        Text("Ataque: ${species.baseStats.attack}")
        Text("Defensa: ${species.baseStats.defense}")
        Text("At. Especial: ${species.baseStats.specialAttack}")
        Text("Def. Especial: ${species.baseStats.specialDefense}")
        Text("Velocidad: ${species.baseStats.speed}")

        // ... más detalles ...
    }
}