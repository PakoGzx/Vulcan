package com.example.vulcan.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteOpenHelper
import com.example.vulcan.data.model.Nota
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotaDao(private val helper: SQLiteOpenHelper) {

    suspend fun insertar(n: Nota): Long = withContext(Dispatchers.IO) {
        val valores = ContentValues().apply {
            put(NotaTable.COL_USUARIO, n.usuarioId)
            put(NotaTable.COL_TITULO, n.titulo)
            put(NotaTable.COL_CONTENIDO, n.contenido)
            put(NotaTable.COL_CREADA, n.creada)
            put(NotaTable.COL_MODIFICADA, n.modificada)
        }
        helper.writableDatabase.insert(NotaTable.NOMBRE, null, valores)
    }

    suspend fun obtenerTodas(usuarioId: Long): List<Nota> =
        consultar("${NotaTable.COL_USUARIO} = ?", arrayOf(usuarioId.toString()))

    suspend fun obtenerPorId(usuarioId: Long, id: Long): Nota? =
        consultar(
            "${NotaTable.COL_USUARIO} = ? AND ${NotaTable.COL_ID} = ?",
            arrayOf(usuarioId.toString(), id.toString())
        ).firstOrNull()

    suspend fun buscar(usuarioId: Long, texto: String): List<Nota> {
        val patron = "%$texto%"
        return consultar(
            "${NotaTable.COL_USUARIO} = ? AND (${NotaTable.COL_TITULO} LIKE ? OR ${NotaTable.COL_CONTENIDO} LIKE ?)",
            arrayOf(usuarioId.toString(), patron, patron)
        )
    }

    // Solo cambia los campos que no son null; devuelve las filas afectadas
    suspend fun actualizar(usuarioId: Long, id: Long, titulo: String?, contenido: String?): Int =
        withContext(Dispatchers.IO) {
            val valores = ContentValues().apply {
                titulo?.let { put(NotaTable.COL_TITULO, it) }
                contenido?.let { put(NotaTable.COL_CONTENIDO, it) }
                put(NotaTable.COL_MODIFICADA, System.currentTimeMillis())
            }
            helper.writableDatabase.update(
                NotaTable.NOMBRE, valores,
                "${NotaTable.COL_ID} = ? AND ${NotaTable.COL_USUARIO} = ?",
                arrayOf(id.toString(), usuarioId.toString())
            )
        }

    suspend fun eliminar(usuarioId: Long, id: Long): Int = withContext(Dispatchers.IO) {
        helper.writableDatabase.delete(
            NotaTable.NOMBRE,
            "${NotaTable.COL_ID} = ? AND ${NotaTable.COL_USUARIO} = ?",
            arrayOf(id.toString(), usuarioId.toString())
        )
    }

    // Más recientes primero
    private suspend fun consultar(where: String, args: Array<String>): List<Nota> =
        withContext(Dispatchers.IO) {
            val lista = mutableListOf<Nota>()
            helper.readableDatabase.query(
                NotaTable.NOMBRE, null, where, args, null, null,
                "${NotaTable.COL_MODIFICADA} DESC, ${NotaTable.COL_ID} DESC"
            ).use { c ->
                while (c.moveToNext()) {
                    lista.add(
                        Nota(
                            id = c.getLong(c.getColumnIndexOrThrow(NotaTable.COL_ID)),
                            usuarioId = c.getLong(c.getColumnIndexOrThrow(NotaTable.COL_USUARIO)),
                            titulo = c.getString(c.getColumnIndexOrThrow(NotaTable.COL_TITULO)),
                            contenido = c.getString(c.getColumnIndexOrThrow(NotaTable.COL_CONTENIDO)),
                            creada = c.getLong(c.getColumnIndexOrThrow(NotaTable.COL_CREADA)),
                            modificada = c.getLong(c.getColumnIndexOrThrow(NotaTable.COL_MODIFICADA))
                        )
                    )
                }
            }
            lista
        }
}
