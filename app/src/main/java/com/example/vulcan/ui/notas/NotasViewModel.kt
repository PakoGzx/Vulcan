package com.example.vulcan.ui.notas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vulcan.SesionManager
import com.example.vulcan.data.model.Nota
import com.example.vulcan.data.repository.NotasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotasViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = NotasRepository.get(application)
    private val usuarioId = SesionManager.usuarioId(application)

    // false = pestaña "Todas", true = pestaña "Favoritas"
    private val _soloFavoritas = MutableStateFlow(false)
    val soloFavoritas: StateFlow<Boolean> = _soloFavoritas.asStateFlow()

    // Lo que realmente se muestra: todas las notas o solo las favoritas
    val notasVisibles: StateFlow<List<Nota>> =
        combine(repo.notas, _soloFavoritas) { notas, soloFav ->
            if (soloFav) notas.filter { it.favorita } else notas
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { repo.cargar(usuarioId) }
    }

    fun mostrarSoloFavoritas(valor: Boolean) {
        _soloFavoritas.value = valor
    }

    // id == 0 significa nota nueva
    fun guardar(id: Long, titulo: String, contenido: String) {
        viewModelScope.launch {
            if (id == 0L) repo.crear(usuarioId, titulo, contenido)
            else repo.actualizar(usuarioId, id, titulo, contenido)
        }
    }

    fun alternarFavorita(nota: Nota) {
        viewModelScope.launch { repo.cambiarFavorita(usuarioId, nota.id, !nota.favorita) }
    }

    fun eliminar(id: Long) {
        viewModelScope.launch { repo.eliminar(usuarioId, id) }
    }
}