package com.example.vulcan.ui.chat

import com.example.vulcan.data.model.MensajeChat

data class ChatUiState(
    val mensajes: List<MensajeChat> = emptyList(),
    val cargando: Boolean = false,       // true mientras se espera la respuesta
    val error: String? = null            // mensaje de error pendiente de mostrar
)