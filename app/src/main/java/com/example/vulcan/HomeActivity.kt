package com.example.vulcan

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vulcan.databinding.ActivityHomeBinding
import com.example.vulcan.ui.chat.ChatUiState
import com.example.vulcan.ui.chat.ChatViewModel
import com.example.vulcan.ui.chat.MensajeAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: ChatViewModel by viewModels()
    private val adapter = MensajeAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Evita que la barra de estado o el teclado tapen la pantalla
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { vista, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        binding.rvMensajes.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true          // los mensajes se acomodan desde abajo
        }
        binding.rvMensajes.adapter = adapter

        binding.btnEnviar.setOnClickListener {
            viewModel.enviar(binding.etMensaje.text.toString())
            binding.etMensaje.text.clear()
        }
        binding.btnLimpiar.setOnClickListener { viewModel.limpiarChat() }

        // Observa el estado mientras la pantalla está visible
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estado.collect { mostrar(it) }
            }
        }
    }

    private fun mostrar(estado: ChatUiState) {
        adapter.submitList(estado.mensajes) {
            if (estado.mensajes.isNotEmpty()) {
                binding.rvMensajes.scrollToPosition(estado.mensajes.size - 1)
            }
        }
        binding.tvEscribiendo.visibility = if (estado.cargando) View.VISIBLE else View.GONE
        binding.btnEnviar.isEnabled = !estado.cargando

        estado.error?.let { mensaje ->
            Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG)
                .setAction("Reintentar") { viewModel.reintentar() }
                .show()
            viewModel.errorMostrado()
        }
    }
}