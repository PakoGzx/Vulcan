package com.example.vulcan.data.local

object NotaTable {
    const val NOMBRE = "notas"
    const val COL_ID = "id"
    const val COL_USUARIO = "usuario_id"
    const val COL_TITULO = "titulo"
    const val COL_CONTENIDO = "contenido"
    const val COL_CREADA = "creada"
    const val COL_MODIFICADA = "modificada"

    const val SQL_CREAR = """
        CREATE TABLE IF NOT EXISTS $NOMBRE (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_USUARIO INTEGER NOT NULL,
            $COL_TITULO TEXT NOT NULL,
            $COL_CONTENIDO TEXT NOT NULL,
            $COL_CREADA INTEGER NOT NULL,
            $COL_MODIFICADA INTEGER NOT NULL
        )
    """
}
