package com.example.ensenando.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ensenando.data.remote.model.UsuarioResponse
import com.example.ensenando.databinding.ItemEstudianteAdminBinding

class EstudianteAdminAdapter(
    private val onEstudianteClick: (UsuarioResponse) -> Unit,
    private val onResetClick: (UsuarioResponse) -> Unit,
    private val onVerReporte: (UsuarioResponse) -> Unit
) : ListAdapter<UsuarioResponse, EstudianteAdminAdapter.EstudianteViewHolder>(EstudianteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EstudianteViewHolder {
        val binding = ItemEstudianteAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EstudianteViewHolder(binding, onEstudianteClick, onResetClick, onVerReporte)
    }

    override fun onBindViewHolder(holder: EstudianteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EstudianteViewHolder(
        private val binding: ItemEstudianteAdminBinding,
        private val onEstudianteClick: (UsuarioResponse) -> Unit,
        private val onResetClick: (UsuarioResponse) -> Unit,
        private val onVerReporte: (UsuarioResponse) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(estudiante: UsuarioResponse) {
            binding.tvEstudianteNombre.text = estudiante.nombre
            binding.tvEstudianteCorreo.text = estudiante.correo

            // Click en el card para ver progreso (acción principal)
            binding.root.setOnClickListener {
                onEstudianteClick(estudiante)
            }

            // Botón Ver Reporte - muestra el reporte del estudiante
            binding.btnVerReporte.setOnClickListener {
                onVerReporte(estudiante)
            }

            // Botón Reset Actividad - resetea toda la actividad del estudiante
            binding.btnResetActividad.setOnClickListener {
                onResetClick(estudiante)
            }
        }
    }

    class EstudianteDiffCallback : DiffUtil.ItemCallback<UsuarioResponse>() {
        override fun areItemsTheSame(oldItem: UsuarioResponse, newItem: UsuarioResponse): Boolean {
            return oldItem.id_usuario == newItem.id_usuario
        }

        override fun areContentsTheSame(oldItem: UsuarioResponse, newItem: UsuarioResponse): Boolean {
            return oldItem == newItem
        }
    }
}