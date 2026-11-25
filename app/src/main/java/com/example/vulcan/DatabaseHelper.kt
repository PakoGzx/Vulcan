package com.example.vulcan

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.vulcan.data.local.ChatTable
import com.example.vulcan.data.local.NotaTable

class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "Vulcan.db"
        private const val DATABASE_VERSION = 4
        private const val TABLE_USUARIOS = "usuarios"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NOMBRE = "nombre"
        private const val COLUMN_CORREO = "correo"
        private const val COLUMN_PASSWORD = "password"

        @Volatile
        private var instancia: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper =
            instancia ?: synchronized(this) {
                instancia ?: DatabaseHelper(context.applicationContext).also { instancia = it }
            }
    }

    //METODO PARA CREAR LA BASE DE DATOS Y SUS TABLAS
    override fun onCreate(db: SQLiteDatabase?) {
        val crearTablaUsaurios = """ 
                CREATE TABLE $TABLE_USUARIOS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NOMBRE TEXT NOT NULL,
                $COLUMN_CORREO TEXT NOT NULL UNIQUE,
                $COLUMN_PASSWORD TEXT NOT NULL
                )
        """.trimIndent()
        db?.execSQL(crearTablaUsaurios)
        db?.execSQL(ChatTable.SQL_CREAR)
        db?.execSQL(NotaTable.SQL_CREAR)
    }

    //METODO PARA ACTUALIZAR LA BASE DE DATOS
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db?.execSQL(ChatTable.SQL_CREAR)             // crea la tabla en bases ya existentes
        }
        if (oldVersion < 3) {
            if (oldVersion >= 2) {
                // El historial de chat existente pasa a ser por usuario (los mensajes viejos quedan en 0)
                db?.execSQL(
                    "ALTER TABLE ${ChatTable.NOMBRE} ADD COLUMN ${ChatTable.COL_USUARIO} INTEGER NOT NULL DEFAULT 0"
                )
            }
            db?.execSQL(NotaTable.SQL_CREAR)
        }
        if (oldVersion == 3) {
            // La tabla notas ya existía sin la columna favorita
            db?.execSQL(
                "ALTER TABLE ${NotaTable.NOMBRE} ADD COLUMN ${NotaTable.COL_FAVORITA} INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    //METODO PARA REGISTRAR UN USUARIO (INSERTAR)
    fun registrarUsuario(nombre: String, correo: String, password: String): Boolean {

        //Crear variable para dar permisos de escritura en la BD
        val db = writableDatabase

        //Crear variable para guardar los datos del usuario
        val datos = ContentValues().apply {
            put(COLUMN_NOMBRE, nombre)
            put(COLUMN_CORREO, correo)
            put(COLUMN_PASSWORD, password)
        }
        val resultado = db.insert(TABLE_USUARIOS, null, datos)

        return resultado != -1L
    }

    //METODO PARA LA CONSULTA DE USUARIOS
    fun iniciarSesion(correo: String, password: String): Long? {
        val emailLimpio = correo.trim().lowercase()
        if (password == "__GOOGLE_AUTH__") return null

        val sql = "SELECT $COLUMN_ID FROM $TABLE_USUARIOS WHERE LOWER($COLUMN_CORREO) = ? AND $COLUMN_PASSWORD = ?"
        return readableDatabase.rawQuery(sql, arrayOf(emailLimpio, password)).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }

    fun obtenerOCrearUsuarioGoogle(correo: String?, nombre: String?): Long? {
        val emailLimpio = correo?.trim()?.lowercase()
        if (emailLimpio.isNullOrEmpty()) return null

        val db = writableDatabase
        db.rawQuery(
            "SELECT id FROM usuarios WHERE LOWER(correo) = ? LIMIT 1",
            arrayOf(emailLimpio)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow("id"))
            }
        }
        val values = ContentValues().apply {
            put(COLUMN_CORREO, emailLimpio)
            put(COLUMN_NOMBRE, nombre?.takeIf { it.isNotBlank() } ?: emailLimpio.substringBefore("@"))
            //Valor especial para identificar a los usuarios de google
            put(COLUMN_PASSWORD, "__GOOGLE_AUTH__")
        }

        val nuevoId = db.insert("usuarios", null, values)
        return if (nuevoId != -1L) nuevoId else null
    }
}