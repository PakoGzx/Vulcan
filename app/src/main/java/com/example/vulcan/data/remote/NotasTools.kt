package com.example.vulcan.data.remote

import com.example.vulcan.data.remote.model.FunctionDeclaration
import com.example.vulcan.data.remote.model.Tool
import com.google.gson.JsonParser

// Catálogo de funciones que Gemini puede pedir. La descripción es lo que el modelo lee
// para decidir cuándo usar cada una.
object NotasTools {

    private fun fn(nombre: String, descripcion: String, parametros: String? = null) =
        FunctionDeclaration(
            name = nombre,
            description = descripcion,
            parameters = parametros?.let { JsonParser.parseString(it).asJsonObject }
        )

    val declaraciones = listOf(
        Tool(
            listOf(
                fn(
                    "crear_nota",
                    "Crea una nota nueva para el usuario.",
                    """{"type":"object","properties":{"titulo":{"type":"string"},"contenido":{"type":"string"}},"required":["titulo","contenido"]}"""
                ),
                fn(
                    "listar_notas",
                    "Lista todas las notas del usuario con id, título y una vista previa."
                ),
                fn(
                    "buscar_notas",
                    "Busca notas cuyo título o contenido contenga el texto indicado.",
                    """{"type":"object","properties":{"texto":{"type":"string"}},"required":["texto"]}"""
                ),
                fn(
                    "leer_nota",
                    "Devuelve el contenido completo de una nota.",
                    """{"type":"object","properties":{"id":{"type":"integer"}},"required":["id"]}"""
                ),
                fn(
                    "actualizar_nota",
                    "Modifica una nota existente. Envía solo los campos que cambian. Si envías contenido, reemplaza el texto completo, así que incluye la versión final entera.",
                    """{"type":"object","properties":{"id":{"type":"integer"},"titulo":{"type":"string"},"contenido":{"type":"string"}},"required":["id"]}"""
                ),
                fn(
                    "eliminar_nota",
                    "Elimina una nota de forma permanente.",
                    """{"type":"object","properties":{"id":{"type":"integer"}},"required":["id"]}"""
                )
            )
        )
    )
}
