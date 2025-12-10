package com.example.ensenando.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ensenando.data.remote.model.UsuarioResponse
import com.example.ensenando.databinding.ItemDocenteAdminBinding

class DocenteAdminAdapter(
    private val onVerReporte: (UsuarioResponse) -> Unit
) : ListAdapter<UsuarioResponse, DocenteAdminAdapter.DocenteViewHolder>(DocenteDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocenteViewHolder {
        val binding = ItemDocenteAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DocenteViewHolder(binding, onVerReporte)
    }
    
    override fun onBindViewHolder(holder: DocenteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class DocenteViewHolder(
        private val binding: ItemDocenteAdminBinding,
        private val onVerReporte: (UsuarioResponse) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(docente: UsuarioResponse) {
            binding.tvDocenteNombre.text = docente.nombre
            binding.tvDocenteCorreo.text = docente.correo
            
            binding.btnVerReporte.setOnClickListener {
                onVerReporte(docente)
            }
        }
    }
    
    class DocenteDiffCallback : DiffUtil.ItemCallback<UsuarioResponse>() {
        override fun areItemsTheSame(oldItem: UsuarioResponse, newItem: UsuarioResponse): Boolean {
            return oldItem.id_usuario == newItem.id_usuario
        }
        
        override fun areContentsTheSame(oldItem: UsuarioResponse, newItem: UsuarioResponse): Boolean {
            return oldItem == newItem
        }
    }
}


