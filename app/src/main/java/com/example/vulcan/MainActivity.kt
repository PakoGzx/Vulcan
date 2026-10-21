package com.example.vulcan

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper : DatabaseHelper
    private lateinit var etCorreo : EditText
    private lateinit var etContrasenia : EditText
    private lateinit var BtnLogin : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val tvCrearCuenta = findViewById<TextView>(R.id.tvCrearCuenta)
        etCorreo = findViewById(R.id.etCorreo)
        etContrasenia = findViewById(R.id.etContrasenia)
        BtnLogin = findViewById(R.id.btnLogin)
        dbHelper = DatabaseHelper(this)

        // Crear evento para abrir la actividad de registr
        tvCrearCuenta.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }

    }
}