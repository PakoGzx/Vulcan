package com.example.vulcan

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.vulcan.data.local.ChatTable

class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "Vulcan.db"
        private const val DATABASE_VERSION = 2
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
    }

    //METODO PARA ACTUALIZAR LA BASE DE DATOS
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db?.execSQL(ChatTable.SQL_CREAR)             // ③ crea la tabla en bases ya existentes
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
    fun iniciarSesion(correo: String, password: String): Boolean {
        val db = readableDatabase
        val query_sql = """
            SELECT * FROM $TABLE_USUARIOS
            WHERE $COLUMN_CORREO = ? 
            AND $COLUMN_PASSWORD = ?
        """.trimIndent()
        val cursor = db.rawQuery(
            query_sql, arrayOf(correo, password)
        )

        // Contar el numero de resultados
        val usuarioEncontrado = cursor.count > 0

        cursor.close()

        return usuarioEncontrado
    }
}