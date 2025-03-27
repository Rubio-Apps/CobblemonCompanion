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
            return speciesCache!!
        }
        return withContext(Dispatchers.IO) { // Ejecuta en hilo de IO
            val loadedSpecies = mutableMapOf<String, SpeciesData>()
            try {
                // Asume que las carpetas de generación están directamente dentro de 'species'
                val generationFolders = assetManager.list("species") ?: emptyArray()
                for (genFolder in generationFolders) {
                    val speciesFiles = assetManager.list("species/$genFolder") ?: continue
                    for (fileName in speciesFiles) {
                        if (fileName.endsWith(".json")) {
                            val filePath = "species/$genFolder/$fileName"
                            try {
                                val jsonString = assetManager.open(filePath).bufferedReader().use { it.readText() }
                                val speciesData = gson.fromJson(jsonString, SpeciesData::class.java)
                                // Usamos el nombre limpio (sin .json) como clave
                                val speciesName = fileName.removeSuffix(".json")
                                loadedSpecies[speciesName] = speciesData
                            } catch (e: IOException) {
                                Log.e("SpeciesRepository", "Error reading file: $filePath", e)
                            } catch (e: Exception) {
                                Log.e("SpeciesRepository", "Error parsing file: $filePath", e)
                            }
                        }
                    }
                }
                Log.d("SpeciesRepository", "Loaded ${loadedSpecies.size} species.")
                speciesCache = loadedSpecies // Guarda en caché
                loadedSpecies
            } catch (e: IOException) {
                Log.e("SpeciesRepository", "Error listing assets", e)
                emptyMap() // Devuelve mapa vacío en caso de error
            }
        }
    }

    suspend fun getSpeciesByName(name: String): SpeciesData? {
        val allSpecies = getAllSpecies() // Asegura que la caché esté cargada
        // Busca ignorando mayúsculas/minúsculas por si acaso
        return allSpecies[name.lowercase()]
    }
}