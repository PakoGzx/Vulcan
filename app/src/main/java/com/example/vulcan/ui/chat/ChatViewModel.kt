package com.example.vulcan.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vulcan.DatabaseHelper
import com.example.vulcan.data.local.ChatDao
import com.example.vulcan.data.model.MensajeChat
import com.example.vulcan.data.repository.ChatRepository
import com.example.vulcan.data.repository.ResultadoChat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ChatRepository(ChatDao(DatabaseHelper(application)))

    private val _estado = MutableStateFlow(ChatUiState())
    val estado: StateFlow<ChatUiState> = _estado.asStateFlow()

    init {
        cargarHistorial()
    }

    // Al abrir la pantalla, recupera la conversación guardada en SQLite
    private fun cargarHistorial() {
        viewModelScope.launch {
            val historial = repo.obtenerHistorial()
            _estado.update { it.copy(mensajes = historial) }
        }
    }

    // El usuario envía una pregunta
    fun enviar(texto: String) {
        val limpio = texto.trim()
        if (limpio.isEmpty() || _estado.value.cargando) return

        // Se marca "cargando" de inmediato para ignorar toques repetidos
        _estado.update { it.copy(cargando = true, error = null) }

        viewModelScope.launch {
            val guardado = repo.guardarMensaje(
                MensajeChat(rol = MensajeChat.ROL_USUARIO, texto = limpio)
            )
            _estado.update { it.copy(mensajes = it.mensajes + guardado) }
            pedirRespuesta()
        }
    }

    // Vuelve a pedir respuesta para la última pregunta (tras un error)
    fun reintentar() {
        val actual = _estado.value
        if (actual.cargando || actual.mensajes.lastOrNull()?.rol != MensajeChat.ROL_USUARIO) return

        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch { pedirRespuesta() }
    }

    // Llama a la API con el historial actual y procesa el resultado
    private suspend fun pedirRespuesta() {
        when (val resultado = repo.enviarMensaje(_estado.value.mensajes)) {
            is ResultadoChat.Exito -> {
                val guardado = repo.guardarMensaje(
                    MensajeChat(rol = MensajeChat.ROL_MODELO, texto = resultado.texto)
                )
                _estado.update { it.copy(mensajes = it.mensajes + guardado, cargando = false) }
            }
            is ResultadoChat.Error -> {
                _estado.update { it.copy(cargando = false, error = resultado.mensaje) }
            }
        }
    }

    fun limpiarChat() {
        viewModelScope.launch {
            repo.limpiarHistorial()
            _estado.update { ChatUiState() }
        }
    }

    // La pantalla lo llama después de mostrar el error, para no repetirlo al rotar
    fun errorMostrado() {
        _estado.update { it.copy(error = null) }
    }
}