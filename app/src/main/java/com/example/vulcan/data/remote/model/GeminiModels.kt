package com.example.vulcan.data.remote.model

import com.google.gson.JsonObject

// Un fragmento de texto: { "text": "hola" }
data class Part(val text: String? = null)

//Llama a las funciones de la clase NotasTools
data class FunctionCall(val name: String, val args: JsonObject? = null)
data class FunctionResponse(val name: String, val response: JsonObject)

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
    val systemInstruction: SystemInstruction? = null
)

// Lo que RECIBIMOS
data class Candidate(val content: Content?)

data class GeminiResponse(val candidates: List<Candidate>?)