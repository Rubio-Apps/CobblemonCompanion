package com.rubioapps.cobblemoncompanion.model

data class CobblemonData(
    val uuid: String,
    val starterPrompted: Boolean,
    val starterLocked: Boolean,
    val starterSelected: Boolean,
    val starterUUID: String,
    val advancementData: AdvancementData
)

data class AdvancementData(
    val totalCaptureCount: Int,
    val aspectsCollected: Map<String, List<String>> // Mapa: Nombre del Pokémon -> Lista de Aspectos (e.g., gender, shiny, forma regional)
)