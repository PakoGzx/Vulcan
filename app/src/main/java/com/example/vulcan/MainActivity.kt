package com.example.vulcan

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvCrearCuenta : TextView
    private lateinit var dbHelper : DatabaseHelper
    private lateinit var etCorreo : EditText
    private lateinit var etContrasenia : EditText
    private lateinit var btnLogin : TextView
    private lateinit var auth : FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var btnGoogle : ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya hay una sesión iniciada, salta directo a las notas
        if (SesionManager.haySesion(this)) {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        tvCrearCuenta = findViewById(R.id.tvCrearCuenta)
        etCorreo = findViewById(R.id.etCorreo)
        etContrasenia = findViewById(R.id.etContrasenia)
        btnLogin = findViewById(R.id.btnLogin)
        dbHelper = DatabaseHelper.getInstance(this)

        btnGoogle = findViewById(R.id.btnGoogle)
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        // Crear evento para abrir la actividad de registr
        tvCrearCuenta.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            iniciarSesion()
        }

        btnGoogle.setOnClickListener {
            iniciarSesionGoogle()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    //METODO PARA INICIAR SESION
    private fun iniciarSesion() {
        val correo = etCorreo.text.toString().trim()
        val pass = etContrasenia.text.toString()

        if(correo.isEmpty() || pass.isEmpty()){
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val usuarioId = dbHelper.iniciarSesion(correo, pass)

        if(usuarioId != null){
            SesionManager.guardar(this, usuarioId)
            Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }else{
            Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
        }
    }

    // 3. MÉTODO PARA AUTENTICAR FIREBASE
    private fun autenticarFirebase(idToken: String){

        val credencialFirebase = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credencialFirebase).addOnCompleteListener(this) {

                inicio ->

            //validar que el inicio de sesion del usuario haya sido correcto
            if (inicio.isSuccessful) {
                //Crear una variable que guarde la informacion del usuario
                val usuario = auth.currentUser
                val usuarioIdLocal = dbHelper.obtenerOCrearUsuarioGoogle(
                    correo = usuario?.email,
                    nombre = usuario?.displayName
                )

                if (usuarioIdLocal != null) {
                    SesionManager.guardar(this, usuarioIdLocal)
                    Toast.makeText(
                        this,
                        "Bienvenido ${usuario?.displayName ?: usuario?.email}",
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(this, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    btnGoogle.isEnabled = true
                    Toast.makeText(
                        this,
                        "No se pudo vincular la cuenta de Google en local",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                btnGoogle.isEnabled = true
                // Guarda el detalle del error de Firebase en Logcat
                Log.e("VulcanGoogle", "Firebase: ${inicio.exception?.message}", inicio.exception)
                Toast.makeText(this, "Error al iniciar con Firebase", Toast.LENGTH_SHORT).show()
            }
        }

    }

    // 4. MÉTODO PARA PROCESAR LA CREDENCIAL
    private fun procesarCredencial(credential: Credential){

        if(credential is CustomCredential && credential.type ==
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL){

            try {
                //Crear una variable que permite identificar la credencial del usuario
                val credencialGoogle = GoogleIdTokenCredential.createFrom(
                    credential.data
                )

                autenticarFirebase(credencialGoogle.idToken)

            } catch (e : GoogleIdTokenParsingException){
                btnGoogle.isEnabled = true
                Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
            }
        } else {
            btnGoogle.isEnabled = true
            Toast.makeText(this, "No se recibió una credencial de Google válida", Toast.LENGTH_SHORT).show()
        }
    }

    // 5. MÉTODO PARA INICIAR SESIÓN
    private fun iniciarSesionGoogle(){
        btnGoogle.isEnabled = false

        val inicioGoogle = GetSignInWithGoogleOption.Builder(
            getString(R.string.default_web_client_id)).build()

        val solicitud = GetCredentialRequest.Builder()
            .addCredentialOption(inicioGoogle)
            .build()

        lifecycleScope.launch {
            try {

                //Verificar si las credenciales del usuario pueden ser utilizadas
                val resultado = credentialManager.getCredential(
                    context = this@MainActivity,
                    request = solicitud
                )

                //Procesar credenciales para ver si el suuario entre o no entra
                procesarCredencial(resultado.credential)

            } catch (e : GetCredentialException) {
                btnGoogle.isEnabled = true
                // Muestra el tipo de error y guarda el detalle en Logcat
                Log.e("VulcanGoogle", "Tipo: ${e::class.java.simpleName} - ${e.message}", e)
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e::class.java.simpleName}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}