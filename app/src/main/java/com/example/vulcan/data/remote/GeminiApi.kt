package com.example.vulcan.data.remote

import com.example.vulcan.data.remote.model.GeminiRequest
import com.example.vulcan.data.remote.model.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface GeminiApi {

    // POST v1beta/models/{modelo}:generateContent
    @POST("v1beta/models/{modelo}:generateContent")
    suspend fun generar(
        @Path("modelo") modelo: String,      // reemplaza {modelo} en la URL
        @Body body: GeminiRequest            // se convierte a JSON
    ): GeminiResponse
}