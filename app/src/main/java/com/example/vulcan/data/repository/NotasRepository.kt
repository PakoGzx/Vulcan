package com.example.vulcan.data.repository

import android.content.Context
import com.example.vulcan.DatabaseHelper
import com.example.vulcan.data.local.NotaDao
import com.example.vulcan.data.model.Nota
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotasRepository private constructor(private val dao: NotaDao) {

    private val _notas = MutableStateFlow<List<Nota>>(emptyList())
    val notas: StateFlow<List<Nota>> = _notas.asStateFlow()

    suspend fun cargar(usuarioId: Long) {
        _notas.value = dao.obtenerTodas(usuarioId)
    }

    suspend fun crear(usuarioId: Long, titulo: String, contenido: String): Nota {
        val nueva = Nota(usuarioId = usuarioId, titulo = titulo, contenido = contenido)
        val id = dao.insertar(nueva)
        cargar(usuarioId)
        return nueva.copy(id = id)
    }

    suspend fun listar(usuarioId: Long) = dao.obtenerTodas(usuarioId)
    suspend fun leer(usuarioId: Long, id: Long) = dao.obtenerPorId(usuarioId, id)
    suspend fun buscar(usuarioId: Long, texto: String) = dao.buscar(usuarioId, texto)

    suspend fun actualizar(usuarioId: Long, id: Long, titulo: String?, contenido: String?): Boolean {
        val filas = dao.actualizar(usuarioId, id, titulo, contenido)
        if (filas > 0) cargar(usuarioId)
        return filas > 0
    }

    suspend fun eliminar(usuarioId: Long, id: Long): Boolean {
        val filas = dao.eliminar(usuarioId, id)
        if (filas > 0) cargar(usuarioId)
        return filas > 0
    }

    // Al cerrar sesión, para no dejar notas de la cuenta anterior en memoria
    fun limpiar() {
        _notas.value = emptyList()
    }

    companion object {
        @Volatile
        private var instancia: NotasRepository? = null

        fun get(context: Context): NotasRepository =
            instancia ?: synchronized(this) {
                instancia ?: NotasRepository(
                    NotaDao(DatabaseHelper.getInstance(context))
                ).also { instancia = it }
            }
    }
}
