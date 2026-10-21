package com.example.vulcan

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "Vulcan.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_USUARIOS = "usuarios"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NOMBRE = "nombre"
        private const val COLUMN_CORREO = "correo"
        private const val COLUMN_PASSWORD = "password"
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
    }

    //METODO PARA ACTUALIZAR LA BASE DE DATOS
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USUARIOS")
        onCreate(db)
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

}