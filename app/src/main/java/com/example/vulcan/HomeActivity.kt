package com.example.vulcan

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.vulcan.data.model.Nota
import com.example.vulcan.data.repository.NotasRepository
import com.example.vulcan.databinding.ActivityHomeBinding
import com.example.vulcan.databinding.DialogNotaBinding
import com.example.vulcan.ui.chat.ChatUiState
import com.example.vulcan.ui.chat.ChatViewModel
import com.example.vulcan.ui.chat.MensajeAdapter
import com.example.vulcan.ui.notas.NotaAdapter
import com.example.vulcan.ui.notas.NotasViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    companion object {
        private const val CANAL_ID = "canal_general"
        private const val NOTIFICACION_ID = 1
        private const val CODIGO_PERMISO = 100
    }
    private lateinit var binding: ActivityHomeBinding
    private val viewModel: NotasViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()

    private val adapter = NotaAdapter(
        alEditar = { mostrarEditor(it) },
        alEliminar = { confirmarEliminar(it) },
        alFavorita = { viewModel.alternarFavorita(it) }
    )
    private val mensajeAdapter = MensajeAdapter()

    private var chatAbierto = false
    private val cerrarChatAlAtras = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = alternarChat(false)
    }

    private lateinit var auth : FirebaseAuth
    private lateinit var credencialManager: androidx.credentials.CredentialManager
    private var cerrandoSesion = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SesionManager.haySesion(this)) {
            irALogin()
            return
        }
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // La barra de estado y el teclado no tapan nada (el panel sube con el teclado)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { vista, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        binding.rvNotas.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.rvNotas.adapter = adapter

        binding.btnNueva.setOnClickListener { mostrarEditor(null) }
        binding.btnSalir.setOnClickListener { cerrarSesion() }

        auth = FirebaseAuth.getInstance()
        credencialManager = CredentialManager.create(this)

        // Pestañas "Todas" / "Favoritas" (se restaura la pestaña al rotar la pantalla)
        binding.tabsFiltro.getTabAt(if (viewModel.soloFavoritas.value) 1 else 0)?.select()
        binding.tabsFiltro.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.mostrarSoloFavoritas(tab.position == 1)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notasVisibles.collect { notas ->
                    adapter.submitList(notas)
                    binding.tvVacio.text = if (viewModel.soloFavoritas.value)
                        "Aún no tienes notas favoritas.\nToca la estrella de una nota para añadirla."
                    else
                        "Aún no tienes notas.\nCrea una o pídeselo al asistente."
                    binding.tvVacio.visibility = if (notas.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, cerrarChatAlAtras)
        configurarChat()
        if (savedInstanceState?.getBoolean("chat_abierto") == true) alternarChat(true)

        crearCanalNoti()
    }

    // ---------- Notas ----------

    // nota == null crea una nueva; si no, edita la existente
    private fun mostrarEditor(nota: Nota?) {
        val vista = DialogNotaBinding.inflate(layoutInflater)
        vista.etTitulo.setText(nota?.titulo)
        vista.etContenido.setText(nota?.contenido)

        MaterialAlertDialogBuilder(this)
            .setTitle(if (nota == null) {
                "Nueva nota"
            } else {
                "Editar nota"
            })
            .setView(vista.root)
            .setPositiveButton("Guardar") { _, _ ->
                val titulo = vista.etTitulo.text.toString().trim()
                val contenido = vista.etContenido.text.toString().trim()
                if (titulo.isNotEmpty() || contenido.isNotEmpty()) {
                    val id = if (nota != null) nota.id else 0L
                    val tituloFinal = if (titulo.isEmpty()) "Sin título" else titulo
                    viewModel.guardar(id, tituloFinal, contenido)
                    if (id == 0L) mostrarNoti("Una nota ha sido creada con éxito")
                    else mostrarNoti("Una nota ha sido actualizada con éxito")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminar(nota: Nota) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar nota")
            .setMessage("¿Eliminar la nota «${nota.titulo}»?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminar(nota.id)
                mostrarNoti("Una nota ha sido eliminada con éxito")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }



    private fun cerrarSesion() {
        if (cerrandoSesion) return
        cerrandoSesion = true

        auth.signOut()
        SesionManager.cerrar(this)
        NotasRepository.get(this).limpiar()
        chatViewModel.limpiarChat()

        lifecycleScope.launch {
            try {
                credencialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Toast.makeText(
                    this@HomeActivity,
                    "No se pudo limpiar el estado de credenciales",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                irALogin()
            }
        }
    }

    private fun irALogin() {
        val intent = Intent(this@HomeActivity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    // ---------- Asistente Gemini (panel flotante) ----------

    private fun configurarChat() {
        val chat = binding.panelChat
        chat.rvMensajes.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        chat.rvMensajes.adapter = mensajeAdapter

        binding.fabGemini.setOnClickListener { alternarChat(!chatAbierto) }
        chat.btnCerrarChat.setOnClickListener { alternarChat(false) }
        chat.btnEnviar.setOnClickListener {
            chatViewModel.enviar(chat.etMensaje.text.toString())
            chat.etMensaje.text.clear()
        }
        chat.btnLimpiar.setOnClickListener { chatViewModel.limpiarChat() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatViewModel.estado.collect { mostrarChat(it) }
            }
        }
    }

    private fun mostrarChat(estado: ChatUiState) {
        val chat = binding.panelChat
        mensajeAdapter.submitList(estado.mensajes) {
            if (estado.mensajes.isNotEmpty()) {
                chat.rvMensajes.scrollToPosition(estado.mensajes.size - 1)
            }
        }
        chat.tvEscribiendo.visibility = if (estado.cargando) View.VISIBLE else View.GONE
        chat.btnEnviar.isEnabled = !estado.cargando

        estado.error?.let { mensaje ->
            Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG)
                .setAnchorView(binding.fabGemini)
                .setAction("Reintentar") { chatViewModel.reintentar() }
                .show()
            chatViewModel.errorMostrado()
        }
    }

    private fun alternarChat(abrir: Boolean) {
        chatAbierto = abrir
        cerrarChatAlAtras.isEnabled = abrir
        val panel = binding.panelChat.root
        if (abrir) {
            panel.animate().cancel()
            panel.alpha = 0f
            panel.translationY = 48f
            panel.visibility = View.VISIBLE
            panel.animate().alpha(1f).translationY(0f).setDuration(180).start()
            binding.fabGemini.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        } else {
            WindowCompat.getInsetsController(window, binding.root).hide(WindowInsetsCompat.Type.ime())
            panel.animate().cancel()
            panel.animate().alpha(0f).translationY(48f).setDuration(150)
                .withEndAction { if (!chatAbierto) panel.visibility = View.GONE }
                .start()
            binding.fabGemini.setImageResource(R.drawable.ic_gemini)
        }
    }

    // Para que el panel siga abierto al rotar la pantalla
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("chat_abierto", chatAbierto)
    }

    //Metodo para crear el canal de notificacion y ver los permisos
    private fun crearCanalNoti() {
        //Verificar la version de android del dispositivo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            //Crear canal de difusion
            val canal = NotificationChannel(
                CANAL_ID,
                "Notificaciones generales",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            canal.description = "Canal de notificaciones de Vulcan"

            val adminNotificacion = getSystemService(NotificationManager::class.java)

            adminNotificacion.createNotificationChannel(canal)
        }
    }

    //Metodo para construir la notificacion
    @SuppressLint("MissingPermission")
    private fun mostrarNoti(noti : String) {
        if (!validarPermisoNoti()) {
            return
        }
        //Configurar la notificacion
        val notificacion = NotificationCompat.Builder(this, CANAL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Vulcan")
            .setContentText(noti)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        //Mandar la notificacion
        NotificationManagerCompat
            .from(this)
            .notify(NOTIFICACION_ID, notificacion)
    }

    //Validar el permiso de notificaciones
    private fun validarPermisoNoti() : Boolean{
        //Validar la version del dispositivo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
            //Pedir permiso al usuario nuevamente
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                CODIGO_PERMISO
            )

            Toast.makeText(this, "Autoriza el permiso y vuelve a intentar", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}