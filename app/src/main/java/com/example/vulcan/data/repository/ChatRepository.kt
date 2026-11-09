package com.example.vulcan.data.repository

import android.util.Log
import com.example.vulcan.data.local.ChatDao
import com.example.vulcan.data.model.MensajeChat
import com.example.vulcan.data.remote.GeminiApi
import com.example.vulcan.data.remote.RetrofitClient
import com.example.vulcan.data.remote.model.Content
import com.example.vulcan.data.remote.model.GeminiRequest
import com.example.vulcan.data.remote.model.Part
import com.example.vulcan.data.remote.model.SystemInstruction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

class ChatRepository(
    private val dao: ChatDao,
    private val api: GeminiApi = RetrofitClient.api
) {

    companion object {
        private const val MAX_MENSAJES = 20     // cuántos mensajes recientes se envían
        private const val MAX_INTENTOS = 3      // intentos ante error temporal
        private const val INSTRUCCION_SISTEMA =
            "Eres un asistente integrado en Vulcan, una aplicación de notas. " +
                    "Responde de forma breve, clara y en español."
    }

    // Método principal: recibe el historial y devuelve éxito o error
    suspend fun enviarMensaje(historial: List<MensajeChat>): ResultadoChat {
        val peticion = construirPeticion(historial)
        return try {
            val respuesta = conReintentos {
                api.generar(RetrofitClient.MODELO, peticion)
            }
            val texto = respuesta.candidates
                ?.firstOrNull()?.content?.parts
                ?.mapNotNull { it.text }
                ?.joinToString("")

            if (texto.isNullOrBlank()) {
                ResultadoChat.Error("El asistente no pudo generar una respuesta. Intenta reformular tu pregunta.")
            } else {
                ResultadoChat.Exito(texto.trim())
            }
        } catch (e: CancellationException) {
            throw e                                   // no atrapar cancelaciones de corrutinas
        } catch (e: HttpException) {
            ResultadoChat.Error(mensajeParaCodigo(e.code()))
        } catch (e: IOException) {
            Log.e("VulcanChat", "Fallo de red: ${e::class.java.simpleName} - ${e.message}", e)
            ResultadoChat.Error("No hay conexión a internet. Revisa tu red e intenta de nuevo.")
        } catch (e: Exception) {
            ResultadoChat.Error("Ocurrió un error inesperado. Intenta de nuevo.")
        }
    }

    // Convierte nuestro historial al formato de Gemini
    private fun construirPeticion(historial: List<MensajeChat>): GeminiRequest {
        val recientes = historial
            .takeLast(MAX_MENSAJES)
            .dropWhile { it.rol != MensajeChat.ROL_USUARIO }   // debe empezar con un mensaje del usuario

        return GeminiRequest(
            contents = recientes.map {
                Content(role = it.rol, parts = listOf(Part(it.texto)))
            },
            systemInstruction = SystemInstruction(listOf(Part(INSTRUCCION_SISTEMA)))
        )
    }

    suspend fun obtenerHistorial(): List<MensajeChat> = dao.obtenerTodos()

    suspend fun guardarMensaje(mensaje: MensajeChat): MensajeChat {
        val id = dao.insertar(mensaje)
        return mensaje.copy(id = id)
    }

    suspend fun limpiarHistorial() {
        dao.borrarTodos()
    }

    // Reintenta con espera creciente (1 s, 2 s...) solo ante 503 o 429
    private suspend fun <T> conReintentos(bloque: suspend () -> T): T {
        var espera = 1000L
        repeat(MAX_INTENTOS - 1) {
            try {
                return bloque()
            } catch (e: HttpException) {
                if (e.code() != 503 && e.code() != 429) throw e
            }
            delay(espera)
            espera *= 2
        }
        return bloque()      // último intento: si falla, el error sube
    }

    private fun mensajeParaCodigo(codigo: Int): String = when (codigo) {
        400 -> "La solicitud no es válida. Revisa la configuración del modelo."
        401, 403 -> "La clave de la API no es válida o no tiene permiso."
        404 -> "El modelo de IA configurado no existe."
        429 -> "Demasiadas consultas seguidas. Espera un momento e intenta de nuevo."
        500, 503 -> "El asistente está ocupado. Intenta de nuevo en un momento."
        else -> "Error del servidor (código $codigo)."
    }
}