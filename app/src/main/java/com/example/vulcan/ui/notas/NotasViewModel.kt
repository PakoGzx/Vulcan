package com.example.vulcan.ui.notas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vulcan.SesionManager
import com.example.vulcan.data.model.Nota
import com.example.vulcan.data.repository.NotasRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotasViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = NotasRepository.get(application)
    private val usuarioId = SesionManager.usuarioId(application)

    val notas: StateFlow<List<Nota>> = repo.notas

    init {
        viewModelScope.launch { repo.cargar(usuarioId) }
    }

    // id == 0 significa nota nueva
    fun guardar(id: Long, titulo: String, contenido: String) {
        viewModelScope.launch {
            if (id == 0L) repo.crear(usuarioId, titulo, contenido)
            else repo.actualizar(usuarioId, id, titulo, contenido)
        }
    }

    fun eliminar(id: Long) {
        viewModelScope.launch { repo.eliminar(usuarioId, id) }
    }
}
