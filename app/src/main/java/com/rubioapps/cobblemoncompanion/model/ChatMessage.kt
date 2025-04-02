package com.rubioapps.cobblemoncompanion.model

data class ChatMessage(
    val id: Long = System.currentTimeMillis(), // ID simple basado en tiempo
    val text: String,
    val isFromUser: Boolean
)