package com.example.vulcan.data.model

data class MensajeChat(
    val id: Long = 0,                                   // lo asignará SQLite
    val rol: String,                                    // "user" o "model"
    val texto: String,
    val fecha: Long = System.currentTimeMillis()        // marca de tiempo
) {
    companion object {
        const val ROL_USUARIO = "user"
        const val ROL_MODELO = "model"
    }
}