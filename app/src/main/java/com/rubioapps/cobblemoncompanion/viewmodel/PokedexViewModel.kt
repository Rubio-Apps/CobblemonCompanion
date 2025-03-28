package com.rubioapps.cobblemoncompanion.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.rubioapps.cobblemoncompanion.model.CobblemonData
import com.rubioapps.cobblemoncompanion.model.DetailedPokedexEntry // Asegúrate que está en el paquete model
import com.rubioapps.cobblemoncompanion.model.PokedexEntryDisplay
import com.rubioapps.cobblemoncompanion.repository.SpeciesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

// Estados posibles para la UI de la Pokedex (fuera de la clase ViewModel)
sealed interface PokedexUiState {
    data object Idle : PokedexUiState
    data object Loading : PokedexUiState
    data class Success(
        val allEntries: List<DetailedPokedexEntry>,
        val displayedEntries: List<DetailedPokedexEntry>,
        val selectedGeneration: Int? = null
    ) : PokedexUiState
    data class Error(val message: String) : PokedexUiState
}

@HiltViewModel
class PokedexViewModel @Inject constructor(
    private val speciesRepository: SpeciesRepository,
    private val gson: Gson,
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val _pokedexState = mutableStateOf<PokedexUiState>(PokedexUiState.Idle)
    val pokedexState: State<PokedexUiState> = _pokedexState

    private var userData: CobblemonData? = null
    private var initialLoadCompleted = false
    private val tag = "PokedexViewModel" // Tag para logs de error/warning

    init {
        // Carga inicial solo una vez
        if (!initialLoadCompleted) {
            loadInitialPokedex()
        }
    }

    private fun loadInitialPokedex() {
        if (initialLoadCompleted) {
            // Log.w(TAG, "loadInitialPokedex called again, but already completed. Ignoring.") // Log opcional
            return
        }
        viewModelScope.launch {
            _pokedexState.value = PokedexUiState.Loading
            try {
                val allSpeciesData = speciesRepository.getAllSpecies()
                if (allSpeciesData.isEmpty()) {
                    Log.e(tag, "Failed to load species data from repository.")
                    _pokedexState.value = PokedexUiState.Error("No se pudieron cargar los datos de las especies.")
                    initialLoadCompleted = true // Marcar completado para no reintentar
                    return@launch
                }

                val initialAllEntries = allSpeciesData.values.map { species ->
                    DetailedPokedexEntry(
                        userEntry = PokedexEntryDisplay(species.name, false, emptyList()),
                        speciesData = species
                    )
                }.sortedBy { it.speciesData.nationalPokedexNumber }

                val initialState = PokedexUiState.Success(
                    allEntries = initialAllEntries,
                    displayedEntries = initialAllEntries,
                    selectedGeneration = null
                )
                initialLoadCompleted = true
                _pokedexState.value = initialState

                // Aplica datos de usuario si existían previamente
                userData?.let { applyUserDataToPokedex(it) }

            } catch (e: Exception) {
                Log.e(tag, "Error during initial pokedex load", e)
                _pokedexState.value = PokedexUiState.Error("Error carga inicial: ${e.localizedMessage}")
                initialLoadCompleted = true
            }
        }
    }

    fun selectGeneration(generation: Int?) {
        val currentState = _pokedexState.value
        if (currentState is PokedexUiState.Success) {
            val filteredEntries = if (generation == null) {
                currentState.allEntries
            } else {
                currentState.allEntries.filter { it.speciesData.generation == generation }
            }
            _pokedexState.value = currentState.copy(
                displayedEntries = filteredEntries,
                selectedGeneration = generation
            )
        } else {
            Log.w(tag, "Cannot select generation, state is not Success.")
        }
    }

    fun processUserJson(uri: Uri) {
        viewModelScope.launch {
            val currentState = _pokedexState.value
            if (currentState is PokedexUiState.Idle || currentState is PokedexUiState.Error) {
                _pokedexState.value = PokedexUiState.Loading
            }

            try {
                val loadedUserData = readAndParseUserJson(uri)
                if (loadedUserData == null) {
                    Log.w(tag,"Failed to read or parse user JSON from URI.")
                    // No cambia el estado si falla, el usuario puede reintentar
                    // Podrías mostrar un mensaje temporal (Snackbar) si quieres
                    if (currentState is PokedexUiState.Success) {
                        _pokedexState.value = currentState // Asegura que se mantiene el estado Success
                    } else {
                        _pokedexState.value = PokedexUiState.Error("No se pudo leer o parsear el archivo.")
                    }
                    return@launch
                }
                userData = loadedUserData
                applyUserDataToPokedex(loadedUserData)

            } catch (e: Exception) {
                Log.e(tag, "Error processing user JSON", e)
                if (currentState is PokedexUiState.Success) {
                    _pokedexState.value = currentState // Mantiene estado si falla después de carga inicial
                } else {
                    _pokedexState.value = PokedexUiState.Error("Error al procesar archivo: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun applyUserDataToPokedex(newUserData: CobblemonData) {
        val currentState = _pokedexState.value
        if (currentState !is PokedexUiState.Success) {
            // Log.w(TAG, "[ApplyUserData] State is not Success, saving data for later.") // Log opcional
            this.userData = newUserData
            return
        }

        val userCapturedMap = newUserData.advancementData.aspectsCollected.mapKeys {
            it.key.substringAfter("cobblemon:").lowercase().trim()
        }

        val updatedAllEntries = currentState.allEntries.map { detailedEntry ->
            val cleanName = detailedEntry.speciesData.name.lowercase()
            val userAspects = userCapturedMap[cleanName]
            val newCapturedStatus = userAspects != null
            val newAspects = userAspects ?: emptyList()

            if (detailedEntry.userEntry.captured != newCapturedStatus || detailedEntry.userEntry.aspects != newAspects) {
                detailedEntry.copy(
                    userEntry = detailedEntry.userEntry.copy(
                        captured = newCapturedStatus,
                        aspects = newAspects
                    )
                )
            } else {
                detailedEntry
            }
        }

        val updatedDisplayedEntries = if (currentState.selectedGeneration == null) {
            updatedAllEntries
        } else {
            updatedAllEntries.filter { it.speciesData.generation == currentState.selectedGeneration }
        }

        val newState = PokedexUiState.Success(
            allEntries = updatedAllEntries,
            displayedEntries = updatedDisplayedEntries,
            selectedGeneration = currentState.selectedGeneration
        )
        _pokedexState.value = newState
    }

    private fun readAndParseUserJson(uri: Uri): CobblemonData? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            BufferedReader(InputStreamReader(inputStream)).use { reader -> // Usa 'use' para cerrar automáticamente
                gson.fromJson(reader, CobblemonData::class.java)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to read/parse user JSON from URI", e)
            null
        }
    }
}