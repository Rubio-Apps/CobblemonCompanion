package com.rubioapps.cobblemoncompanion.repository

import android.content.res.AssetManager
import android.util.Log
import com.google.gson.Gson
import com.rubioapps.cobblemoncompanion.model.species.SpeciesData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeciesRepository @Inject constructor(
    private val assetManager: AssetManager,
    private val gson: Gson
) {

    // Cache para guardar los datos una vez cargados (opcional pero recomendado)
    private var speciesCache: Map<String, SpeciesData>? = null

    suspend fun getAllSpecies(): Map<String, SpeciesData> {
        if (speciesCache != null) {
            Log.d("SpeciesRepository", "Returning cached species data.")
            return speciesCache!!
        }
        Log.d("SpeciesRepository", "Loading species data from assets...")
        return withContext(Dispatchers.IO) {
            val loadedSpecies = mutableMapOf<String, SpeciesData>()
            try {
                val generationFolders: List<String> = assetManager.list("species")?.let { folders ->
                    folders.filter { it.startsWith("gen") } // filter devuelve List<String>
                } ?: emptyList() // Usa emptyList() para coincidir con el tipo de filter

                Log.d("SpeciesRepository", "Found generation folders: ${generationFolders.joinToString()}")

                if (generationFolders.isEmpty()) {
                    Log.w("SpeciesRepository", "No generation folders found starting with 'gen' in assets/species.")
                }

                for (genFolder in generationFolders) { // Ahora 'generationFolders' es un List<String> iterable
                    val generationNumber = genFolder.removePrefix("gen").toIntOrNull()
                    if (generationNumber == null) {
                        Log.w("SpeciesRepository", "Could not parse generation number from folder: $genFolder")
                        continue
                    }

                    val speciesFiles = assetManager.list("species/$genFolder") ?: continue // OK: list devuelve Array<String>?
                    Log.d("SpeciesRepository", "Processing folder $genFolder, found ${speciesFiles.size} files.")

                    for (fileName in speciesFiles) { // OK: speciesFiles es Array<String> (si no es null)
                        if (fileName.endsWith(".json")) {
                            val filePath = "species/$genFolder/$fileName"
                            try {
                                val jsonString = assetManager.open(filePath).bufferedReader().use { it.readText() }
                                // Parsear SIN la generación primero
                                val speciesDataFromJson = gson.fromJson(jsonString, SpeciesData::class.java)

                                // Crear la instancia final CON la generación usando .copy()
                                // Asegúrate de que SpeciesData tiene el campo 'generation' y un valor por defecto o es nullable si no
                                val speciesData = speciesDataFromJson.copy(generation = generationNumber)

                                val speciesName = fileName.removeSuffix(".json")
                                loadedSpecies[speciesName.lowercase()] = speciesData // Guardar con clave en minúsculas
                            } catch (e: Exception) {
                                Log.e("SpeciesRepository", "Error reading/parsing file: $filePath", e)
                            }
                        }
                    }
                }
                Log.d("SpeciesRepository", "Finished loading. Total species loaded: ${loadedSpecies.size}")
                speciesCache = loadedSpecies // Guardar en caché
                loadedSpecies // Devolver el mapa
            } catch (e: IOException) {
                Log.e("SpeciesRepository", "Error listing assets directory", e)
                emptyMap() // Devuelve mapa vacío en caso de error de IO
            } catch (e: Exception) {
                Log.e("SpeciesRepository", "Unexpected error during species loading", e)
                emptyMap() // Devuelve mapa vacío en caso de otro error
            }
        }
    }

    suspend fun getSpeciesByName(name: String): SpeciesData? {
        val allSpecies = getAllSpecies() // Asegura que la caché esté cargada o se cargue
        // Busca usando la clave en minúsculas
        return allSpecies[name.lowercase()]
    }
}