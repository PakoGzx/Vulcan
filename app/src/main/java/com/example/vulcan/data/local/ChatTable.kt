package com.example.vulcan.data.local

object ChatTable {
    const val NOMBRE = "mensajes_chat"
    const val COL_ID = "id"
    const val COL_ROL = "rol"
    const val COL_TEXTO = "texto"
    const val COL_FECHA = "fecha"

    const val SQL_CREAR = """
        CREATE TABLE IF NOT EXISTS $NOMBRE (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_ROL TEXT NOT NULL,
            $COL_TEXTO TEXT NOT NULL,
            $COL_FECHA INTEGER NOT NULL
        )
    """
}