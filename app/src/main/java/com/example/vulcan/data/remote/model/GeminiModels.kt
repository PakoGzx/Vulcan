package com.example.vulcan.data.remote.model

import com.google.gson.JsonObject

// Gemini pide ejecutar una función: { "name": "crear_nota", "args": {...} }
data class FunctionCall(val name: String, val args: JsonObject? = null)

// Lo que le devolvemos a Gemini después de ejecutarla
data class FunctionResponse(val name: String, val response: JsonObject)

// Un fragmento del mensaje: texto, llamada a función o respuesta de función
data class Part(
    val text: String? = null,
    val functionCall: FunctionCall? = null,
    val functionResponse: FunctionResponse? = null,
    val thoughtSignature: String? = null   // Gemini 3 lo exige al devolver llamadas a funciones
)

// Un mensaje: { "role": "user", "parts": [ ... ] }
data class Content(
    val role: String,          // "user" o "model"
    val parts: List<Part>
)

// Instrucciones del sistema (no lleva "role")
data class SystemInstruction(val parts: List<Part>)

// Declaración de las herramientas que Gemini puede pedir
data class Tool(val functionDeclarations: List<FunctionDeclaration>)
data class FunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: JsonObject? = null
)

// Lo que ENVIAMOS
data class GeminiRequest(
    val contents: List<Content>,
    val systemInstruction: SystemInstruction? = null,
    val tools: List<Tool>? = null
)

// Lo que RECIBIMOS
data class Candidate(val content: Content?)

data class GeminiResponse(val candidates: List<Candidate>?)