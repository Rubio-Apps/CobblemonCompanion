package com.rubioapps.cobblemoncompanion.model.species

import com.google.gson.annotations.SerializedName

// Archivo: SpeciesData.kt
data class SpeciesData(
    val name: String,
    val nationalPokedexNumber: Int,
    val generation: Int,
    val primaryType: String,
    val secondaryType: String?, // Puede ser nulo
    val abilities: List<String>, // O una clase Ability si tiene más datos
    val baseStats: Stats,
    val catchRate: Int,
    val maleRatio: Float,
    val femaleRatio: Float,
    // val experienceGroup: String, // Añade los campos que necesites
    val evolutions: List<Evolution>?, // Puede ser nulo o vacío
    val spawnBiomes: List<String>?, // Puede ser nulo o vacío
    // Añade otros campos relevantes: forms, moves, drops, etc.
)

// Archivo: Stats.kt
data class Stats(
    val hp: Int,
    @SerializedName("atk") val attack: Int, // Usa SerializedName si el nombre JSON es diferente
    @SerializedName("def") val defense: Int,
    @SerializedName("spa") val specialAttack: Int,
    @SerializedName("spd") val specialDefense: Int,
    @SerializedName("spe") val speed: Int
)

// Archivo: Evolution.kt
data class Evolution(
    val level: Int?, // Puede evolucionar por nivel
    val item: String?, // Puede evolucionar por item
    val anObjectBabyPokemon: String?,
    // ...otros criterios de evolución
    val result: String // El Pokémon resultante
    // ...otros detalles de la evolución
)
