package com.example.vulcan.ui.notas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vulcan.data.model.Nota
import com.example.vulcan.databinding.ItemNotaBinding
import java.text.DateFormat
import java.util.Date

class NotaAdapter(
    private val alEditar: (Nota) -> Unit,
    private val alEliminar: (Nota) -> Unit
) : ListAdapter<Nota, NotaAdapter.NotaViewHolder>(Diff) {

    class NotaViewHolder(val binding: ItemNotaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        NotaViewHolder(ItemNotaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: NotaViewHolder, position: Int) {
        val nota = getItem(position)
        with(holder.binding) {
            tvTitulo.text = nota.titulo
            tvContenido.text = nota.contenido
            tvContenido.visibility = if (nota.contenido.isBlank()) View.GONE else View.VISIBLE
            tvFecha.text = DateFormat
                .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(nota.modificada))
            root.setOnClickListener { alEditar(nota) }
            btnEliminar.setOnClickListener { alEliminar(nota) }
        }
    }

    object Diff : DiffUtil.ItemCallback<Nota>() {
        override fun areItemsTheSame(a: Nota, b: Nota) = a.id == b.id
        override fun areContentsTheSame(a: Nota, b: Nota) = a == b
    }
}
