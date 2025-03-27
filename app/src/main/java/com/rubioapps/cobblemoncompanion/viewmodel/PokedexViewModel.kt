package com.rubioapps.cobblemoncompanion.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.rubioapps.cobblemoncompanion.model.CobblemonData
import com.rubioapps.cobblemoncompanion.model.PokedexEntryDisplay
import com.rubioapps.cobblemoncompanion.repository.SpeciesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import android.content.ContentResolver // Importar ContentResolver
import com.rubioapps.cobblemoncompanion.model.species.SpeciesData


// Estados posibles para la UI de la Pokedex
sealed interface PokedexUiState {
    object Idle : PokedexUiState // Estado inicial o tras error no crítico
    object Loading : PokedexUiState
    data class Success(val entries: List<DetailedPokedexEntry>) : PokedexUiState
    data class Error(val message: String) : PokedexUiState
}

// Nueva data class para combinar datos
data class DetailedPokedexEntry(
    val userEntry: PokedexEntryDisplay,
    val speciesData: SpeciesData
)

@HiltViewModel
class PokedexViewModel @Inject constructor(
    private val speciesRepository: SpeciesRepository,
    private val gson: Gson, // Necesitamos Gson para parsear el JSON del usuario
    private val contentResolver: ContentResolver // Necesitamos ContentResolver para leer el URI
) : ViewModel() {

    // Estado interno mutable
    private val _pokedexState = mutableStateOf<PokedexUiState>(PokedexUiState.Idle)
    // Estado público inmutable que la UI observará
    val pokedexState: State<PokedexUiState> = _pokedexState

    // Datos del usuario (si se han cargado)
    private var userData: CobblemonData? = null

    init {
        // Carga todas las especies al iniciar el ViewModel
        loadInitialPokedex()
    }

    // Carga la Pokedex inicial (todas las especies, ninguna marcada como capturada)
    private fun loadInitialPokedex() {
        viewModelScope.launch {
            _pokedexState.value = PokedexUiState.Loading
            try {
                val allSpeciesData = speciesRepository.getAllSpecies()
                if (allSpeciesData.isEmpty()) {
                    _pokedexState.value = PokedexUiState.Error("No se pudieron cargar los datos de las especies.")
                    return@launch
                }

                // Crea entradas detalladas, inicialmente todas como "no capturadas"
                val initialEntries = allSpeciesData.values.map { species ->
                    DetailedPokedexEntry(
                        userEntry = PokedexEntryDisplay(species.name, false, emptyList()), // Marcado como NO capturado
                        speciesData = species
                    )
                }.sortedBy { it.speciesData.nationalPokedexNumber } // Ordenar por número de Pokedex

                Log.d("ViewModel", "Initial Pokedex loaded with ${initialEntries.size} species.")
                _pokedexState.value = PokedexUiState.Success(initialEntries)

                // Si ya teníamos datos de usuario, los aplicamos
                userData?.let { applyUserDataToPokedex(it, initialEntries) }

            } catch (e: Exception) {
                Log.e("ViewModel", "Error loading initial pokedex", e)
                _pokedexState.value = PokedexUiState.Error("Error al cargar datos iniciales: ${e.localizedMessage}")
            }
        }
    }

    // Procesa el JSON del usuario y actualiza la Pokedex existente
    fun processUserJson(uri: Uri) {
        viewModelScope.launch {
            // Mantenemos el estado actual o ponemos Loading si estaba Idle
            val currentState = _pokedexState.value
            if (currentState is PokedexUiState.Idle || currentState is PokedexUiState.Error) {
                _pokedexState.value = PokedexUiState.Loading
            } else if (currentState is PokedexUiState.Success) {
                // Podríamos mostrar un indicador sobre la lista existente
            }


            try {
                // 1. Leer y parsear el JSON del usuario
                val loadedUserData = readAndParseUserJson(uri)
                if (loadedUserData == null) {
                    // Podríamos volver al estado anterior o mostrar un error temporal
                    if (currentState is PokedexUiState.Success) _pokedexState.value = currentState // Revertir si falla
                    else _pokedexState.value = PokedexUiState.Error("No se pudo leer o parsear el archivo.")
                    return@launch
                }
                userData = loadedUserData // Guarda los datos del usuario
                Log.d("ViewModel", "User data loaded: ${userData?.advancementData?.totalCaptureCount} captures.")


                // 2. Si la carga inicial ya terminó (Success), aplica los datos del usuario
                if (currentState is PokedexUiState.Success) {
                    applyUserDataToPokedex(loadedUserData, currentState.entries)
                } else {
                    // Si la carga inicial aún no ha terminado, los datos se aplicarán
                    // cuando loadInitialPokedex termine (ver llamada al final de loadInitialPokedex)
                    Log.d("ViewModel", "Initial load not complete, user data will be applied later.")
                    // Podríamos forzar la recarga inicial si el estado era Error/Idle aquí
                    if (currentState is PokedexUiState.Idle || currentState is PokedexUiState.Error){
                        loadInitialPokedex() // Asegura que la carga inicial se ejecute
                    }
                }

            } catch (e: Exception) {
                Log.e("ViewModel", "Error processing user JSON", e)
                // Vuelve al estado anterior o muestra error
                if (currentState is PokedexUiState.Success) _pokedexState.value = currentState
                else _pokedexState.value = PokedexUiState.Error("Error al procesar archivo: ${e.localizedMessage}")
            }
        }
    }

    // Aplica los datos del usuario a una lista existente de DetailedPokedexEntry
    private fun applyUserDataToPokedex(userData: CobblemonData, currentEntries: List<DetailedPokedexEntry>) {
        Log.d("ViewModel", "Applying user data to ${currentEntries.size} entries.")
        val userCapturedMap = userData.advancementData.aspectsCollected.mapKeys {
            it.key.substringAfter("cobblemon:") // Mapa con nombres limpios como clave
        }

        val updatedEntries = currentEntries.map { detailedEntry ->
            val cleanName = detailedEntry.speciesData.name.lowercase()
            val userAspects = userCapturedMap[cleanName]

            if (userAspects != null) {
                // Pokémon encontrado en los datos del usuario: marcar como capturado y añadir aspectos
                detailedEntry.copy(
                    userEntry = detailedEntry.userEntry.copy(captured = true, aspects = userAspects)
                )
            } else {
                // Pokémon no encontrado: asegurarse de que esté marcado como no capturado
                // (ya debería estarlo por la carga inicial, pero por seguridad)
                if (detailedEntry.userEntry.captured) {
                    detailedEntry.copy(
                        userEntry = detailedEntry.userEntry.copy(captured = false, aspects = emptyList())
                    )
                } else {
                    detailedEntry // Sin cambios
                }
            }
        }
        Log.d("ViewModel", "User data applied. State updated.")
        _pokedexState.value = PokedexUiState.Success(updatedEntries)
    }


    // --- readAndParseUserJson sin cambios ---
    private suspend fun readAndParseUserJson(uri: Uri): CobblemonData? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val reader = BufferedReader(InputStreamReader(inputStream))
            gson.fromJson(reader, CobblemonData::class.java)
        } catch (e: Exception) {
            Log.e("ViewModel", "Failed to read/parse user JSON from URI", e)
            null
        }
    }
}