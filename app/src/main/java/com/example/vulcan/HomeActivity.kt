package com.example.vulcan

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: NotasViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()

    private val adapter = NotaAdapter(
        alEditar = { mostrarEditor(it) },
        alEliminar = { confirmarEliminar(it) }
    )
    private val mensajeAdapter = MensajeAdapter()

    private var chatAbierto = false
    private val cerrarChatAlAtras = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = alternarChat(false)
    }

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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notas.collect { notas ->
                    adapter.submitList(notas)
                    binding.tvVacio.visibility = if (notas.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, cerrarChatAlAtras)
        configurarChat()
        if (savedInstanceState?.getBoolean("chat_abierto") == true) alternarChat(true)
    }

    // ---------- Notas ----------

    // nota == null crea una nueva; si no, edita la existente
    private fun mostrarEditor(nota: Nota?) {
        val vista = DialogNotaBinding.inflate(layoutInflater)
        vista.etTitulo.setText(nota?.titulo)
        vista.etContenido.setText(nota?.contenido)

        MaterialAlertDialogBuilder(this)
            .setTitle(if (nota == null) "Nueva nota" else "Editar nota")
            .setView(vista.root)
            .setPositiveButton("Guardar") { _, _ ->
                val titulo = vista.etTitulo.text.toString().trim()
                val contenido = vista.etContenido.text.toString().trim()
                if (titulo.isNotEmpty() || contenido.isNotEmpty()) {
                    viewModel.guardar(nota?.id ?: 0L, titulo.ifEmpty { "Sin título" }, contenido)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminar(nota: Nota) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar nota")
            .setMessage("¿Eliminar la nota «${nota.titulo}»?")
            .setPositiveButton("Eliminar") { _, _ -> viewModel.eliminar(nota.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cerrarSesion() {
        SesionManager.cerrar(this)
        NotasRepository.get(this).limpiar()
        irALogin()
    }

    private fun irALogin() {
        startActivity(Intent(this, MainActivity::class.java))
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
}
