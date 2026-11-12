package com.example.vulcan.data.repository

import com.example.vulcan.data.model.Nota
import com.example.vulcan.data.remote.model.FunctionCall
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Traduce cada llamada de Gemini a una operación sobre las notas del usuario.
// El usuarioId viene de la sesión, NUNCA de lo que diga el modelo.
class NotasToolExecutor(
    private val repo: NotasRepository,
    private val usuarioId: Long
) {
    private val formatoFecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    suspend fun ejecutar(llamada: FunctionCall): JsonObject {
        val a = llamada.args ?: JsonObject()
        return try {
            when (llamada.name) {
                "crear_nota" -> crear(a)
                "listar_notas" -> listar()
                "buscar_notas" -> buscar(a)
                "leer_nota" -> leer(a)
                "actualizar_nota" -> actualizar(a)
                "eliminar_nota" -> eliminar(a)
                else -> fallo("Herramienta desconocida: ${llamada.name}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fallo("Error interno: ${e.message}")
        }
    }

    private suspend fun crear(a: JsonObject): JsonObject {
        val titulo = a.texto("titulo")?.trim()
        val contenido = a.texto("contenido")
        if (titulo.isNullOrEmpty() || contenido == null) return fallo("Faltan titulo o contenido.")
        val nota = repo.crear(usuarioId, titulo, contenido)
        return ok { addProperty("id", nota.id); addProperty("titulo", nota.titulo) }
    }

    private suspend fun listar(): JsonObject {
        val notas = repo.listar(usuarioId)
        return ok { add("notas", resumen(notas)); addProperty("total", notas.size) }
    }

    private suspend fun buscar(a: JsonObject): JsonObject {
        val texto = a.texto("texto")?.trim()
        if (texto.isNullOrEmpty()) return fallo("Falta el texto a buscar.")
        val notas = repo.buscar(usuarioId, texto)
        return ok { add("notas", resumen(notas)); addProperty("total", notas.size) }
    }

    private suspend fun leer(a: JsonObject): JsonObject {
        val id = a.entero("id") ?: return fallo("Falta el id.")
        val nota = repo.leer(usuarioId, id) ?: return fallo("No existe una nota con id $id.")
        return ok { add("nota", nota.aJson(completa = true)) }
    }

    private suspend fun actualizar(a: JsonObject): JsonObject {
        val id = a.entero("id") ?: return fallo("Falta el id.")
        val titulo = a.texto("titulo")
        val contenido = a.texto("contenido")
        if (titulo == null && contenido == null) return fallo("No se indicó ningún cambio.")
        return if (repo.actualizar(usuarioId, id, titulo, contenido)) ok { addProperty("id", id) }
        else fallo("No existe una nota con id $id.")
    }

    private suspend fun eliminar(a: JsonObject): JsonObject {
        val id = a.entero("id") ?: return fallo("Falta el id.")
        return if (repo.eliminar(usuarioId, id)) ok { addProperty("id", id) }
        else fallo("No existe una nota con id $id.")
    }

    // --- utilidades ---
    private fun JsonObject.texto(clave: String): String? =
        get(clave)?.takeIf { it.isJsonPrimitive }?.asString

    private fun JsonObject.entero(clave: String): Long? =
        get(clave)?.takeIf { it.isJsonPrimitive }?.asString?.toDoubleOrNull()?.toLong()

    private fun ok(bloque: JsonObject.() -> Unit) =
        JsonObject().apply { addProperty("estado", "ok"); bloque() }

    private fun fallo(mensaje: String) =
        JsonObject().apply { addProperty("estado", "error"); addProperty("mensaje", mensaje) }

    private fun resumen(notas: List<Nota>) =
        JsonArray().apply { notas.forEach { add(it.aJson(completa = false)) } }

    private fun Nota.aJson(completa: Boolean): JsonObject {
        val nota = this
        return JsonObject().apply {
            addProperty("id", nota.id)
            addProperty("titulo", nota.titulo)
            if (completa) addProperty("contenido", nota.contenido)
            else addProperty("vista_previa", nota.contenido.take(80))
            addProperty("modificada", formatoFecha.format(Date(nota.modificada)))
        }
    }
}
