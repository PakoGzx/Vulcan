package com.example.vulcan.data.remote.model

// Un fragmento de texto: { "text": "hola" }
data class Part(val text: String)

// Un mensaje: { "role": "user", "parts": [ ... ] }
data class Content(
    val role: String,          // "user" o "model"
    val parts: List<Part>
)

// Instrucciones del sistema (no lleva "role")
data class SystemInstruction(val parts: List<Part>)

// Lo que ENVIAMOS
data class GeminiRequest(
    val contents: List<Content>,
    val systemInstruction: SystemInstruction? = null
)

// Lo que RECIBIMOS
data class Candidate(val content: Content?)

data class GeminiResponse(val candidates: List<Candidate>?)