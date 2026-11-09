package com.example.vulcan.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteOpenHelper
import com.example.vulcan.data.model.MensajeChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatDao(private val helper: SQLiteOpenHelper) {

    // Guarda un mensaje y devuelve el id asignado (-1 si falló)
    suspend fun insertar(mensaje: MensajeChat): Long = withContext(Dispatchers.IO) {
        val valores = ContentValues().apply {
            put(ChatTable.COL_ROL, mensaje.rol)
            put(ChatTable.COL_TEXTO, mensaje.texto)
            put(ChatTable.COL_FECHA, mensaje.fecha)
        }
        helper.writableDatabase.insert(ChatTable.NOMBRE, null, valores)
    }

    // Lee todo el historial, del más antiguo al más reciente
    suspend fun obtenerTodos(): List<MensajeChat> = withContext(Dispatchers.IO) {
        val lista = mutableListOf<MensajeChat>()
        helper.readableDatabase.query(
            ChatTable.NOMBRE,
            null, null, null, null, null,
            "${ChatTable.COL_FECHA} ASC, ${ChatTable.COL_ID} ASC"
        ).use { cursor ->
            val iId = cursor.getColumnIndexOrThrow(ChatTable.COL_ID)
            val iRol = cursor.getColumnIndexOrThrow(ChatTable.COL_ROL)
            val iTexto = cursor.getColumnIndexOrThrow(ChatTable.COL_TEXTO)
            val iFecha = cursor.getColumnIndexOrThrow(ChatTable.COL_FECHA)
            while (cursor.moveToNext()) {
                lista.add(
                    MensajeChat(
                        id = cursor.getLong(iId),
                        rol = cursor.getString(iRol),
                        texto = cursor.getString(iTexto),
                        fecha = cursor.getLong(iFecha)
                    )
                )
            }
        }
        lista
    }

    // Borra toda la conversación; devuelve cuántas filas se eliminaron
    suspend fun borrarTodos(): Int = withContext(Dispatchers.IO) {
        helper.writableDatabase.delete(ChatTable.NOMBRE, null, null)
    }
}