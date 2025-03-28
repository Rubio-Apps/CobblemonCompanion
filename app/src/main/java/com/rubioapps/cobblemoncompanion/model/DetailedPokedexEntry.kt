package com.rubioapps.cobblemoncompanion.model

import com.rubioapps.cobblemoncompanion.model.species.SpeciesData

data class DetailedPokedexEntry(
    val userEntry: PokedexEntryDisplay,
    val speciesData: SpeciesData
)