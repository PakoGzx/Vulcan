package com.example.vulcan.data.remote

import com.example.vulcan.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // Cámbialo por el modelo que verificaste en AI Studio / Postman
    const val MODELO = "gemini-3.7-flash"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader("x-goog-api-key")   // evita imprimir tu clave en Logcat
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val peticion = chain.request().newBuilder()
                .addHeader("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
                .build()
            chain.proceed(peticion)
        }
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)   // la IA puede tardar en responder
        .build()

    val api: GeminiApi = Retrofit.Builder()
        .baseUrl(BASE_URL)                    // debe terminar en "/"
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeminiApi::class.java)
}