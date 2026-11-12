package com.example.vulcan.data.model

data class Nota(
    val id: Long = 0,
    val usuarioId: Long,
    val titulo: String,
    val contenido: String,
    val creada: Long = System.currentTimeMillis(),
    val modificada: Long = creada
)
