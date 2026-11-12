package com.example.vulcan

import android.content.Context

object SesionManager {
    private const val PREFS = "vulcan_sesion"
    private const val KEY_USUARIO = "usuario_id"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun guardar(c: Context, usuarioId: Long) =
        prefs(c).edit().putLong(KEY_USUARIO, usuarioId).apply()

    fun usuarioId(c: Context): Long = prefs(c).getLong(KEY_USUARIO, -1L)

    fun haySesion(c: Context) = usuarioId(c) != -1L

    fun cerrar(c: Context) = prefs(c).edit().clear().apply()
}
