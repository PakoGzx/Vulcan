package com.example.vulcan

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RegistroActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegistrar: Button

    private lateinit var dbHelper: DatabaseHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro)

        etNombre = findViewById(R.id.etNombre)
        etCorreo = findViewById(R.id.etCorreo)
        etPassword = findViewById(R.id.etPassword)
        btnRegistrar = findViewById(R.id.btnRegistrar)

        dbHelper = DatabaseHelper(this)

        //Crear el evento para registrar un usuario
        btnRegistrar.setOnClickListener {
            registrarUsuario()
        }
    }

    //METODO PARA REGISTRAR UN USUARIO
    private fun registrarUsuario(){

        //Obtener la informacion que escribe el usuario
        val nombre = etNombre.text.toString().trim()
        val correo = etCorreo.text.toString().trim()
        val password = etPassword.text.toString().trim()

        //Validar que los campos no esten vacios
        if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val resultado = dbHelper.registrarUsuario(nombre, correo, password)

        if (resultado){
            // Si el resultado es true
            Toast.makeText(this, "Registro Existoso", Toast.LENGTH_SHORT).show()
            finish()
        }else{
            // Si es false
            Toast.makeText(this, "Registro Fallido", Toast.LENGTH_SHORT).show()
        }
    }
}