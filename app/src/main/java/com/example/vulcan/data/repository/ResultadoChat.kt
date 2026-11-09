package com.example.vulcan.data.repository

sealed class ResultadoChat {
    data class Exito(val texto: String) : ResultadoChat()
    data class Error(val mensaje: String) : ResultadoChat()
}