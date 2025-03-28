package com.rubioapps.cobblemoncompanion

// Objeto para definir las rutas como constantes
object AppDestinations {
    const val POKEDEX_ROUTE = "pokedex"
    const val CHATBOT_ROUTE = "chatbot"
    const val SETTINGS_ROUTE = "settings"
    // Nueva ruta con argumento {pokemonName}
    const val POKEMON_DETAIL_ROUTE = "pokemon_detail/{pokemonName}"
    // Helper para construir la ruta con el nombre
    fun pokemonDetailRoute(pokemonName: String) = "pokemon_detail/$pokemonName"
}