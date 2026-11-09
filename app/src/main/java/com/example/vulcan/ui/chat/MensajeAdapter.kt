package com.example.vulcan.ui.chat

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vulcan.R
import com.example.vulcan.data.model.MensajeChat
import com.example.vulcan.databinding.ItemMensajeBinding

class MensajeAdapter :
    ListAdapter<MensajeChat, MensajeAdapter.MensajeViewHolder>(Diff) {

    // Guarda las referencias a las vistas de una burbuja
    class MensajeViewHolder(val binding: ItemMensajeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MensajeViewHolder {
        val binding = ItemMensajeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MensajeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MensajeViewHolder, position: Int) {
        val mensaje = getItem(position)
        val tv = holder.binding.tvMensaje
        tv.text = mensaje.texto

        val params = tv.layoutParams as FrameLayout.LayoutParams
        if (mensaje.rol == MensajeChat.ROL_USUARIO) {
            params.gravity = Gravity.END                       // derecha
            tv.setBackgroundResource(R.drawable.bg_burbuja_usuario)
            tv.setTextColor(Color.WHITE)
        } else {
            params.gravity = Gravity.START                     // izquierda
            tv.setBackgroundResource(R.drawable.bg_burbuja_modelo)
            tv.setTextColor(Color.BLACK)
        }
        tv.layoutParams = params
    }

    // Compara listas para actualizar solo lo que cambió
    object Diff : DiffUtil.ItemCallback<MensajeChat>() {
        override fun areItemsTheSame(a: MensajeChat, b: MensajeChat) = a.id == b.id
        override fun areContentsTheSame(a: MensajeChat, b: MensajeChat) = a == b
    }
}