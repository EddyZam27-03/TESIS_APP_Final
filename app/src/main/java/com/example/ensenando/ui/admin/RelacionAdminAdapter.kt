package com.example.ensenando.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ensenando.data.local.entity.DocenteEstudianteEntity
import com.example.ensenando.databinding.ItemRelacionAdminBinding

class RelacionAdminAdapter(
    private val onEliminar: (DocenteEstudianteEntity) -> Unit
) : ListAdapter<DocenteEstudianteEntity, RelacionAdminAdapter.RelacionViewHolder>(RelacionDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelacionViewHolder {
        val binding = ItemRelacionAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RelacionViewHolder(binding, onEliminar)
    }
    
    override fun onBindViewHolder(holder: RelacionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class RelacionViewHolder(
        private val binding: ItemRelacionAdminBinding,
        private val onEliminar: (DocenteEstudianteEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(relacion: DocenteEstudianteEntity) {
            binding.tvDocenteNombre.text = "Docente ID: ${relacion.idDocente}"
            binding.tvEstudianteNombre.text = "Estudiante ID: ${relacion.idEstudiante}"
            binding.tvEstado.text = "Estado: ${relacion.estado}"
            
            binding.btnEliminar.setOnClickListener {
                onEliminar(relacion)
            }
        }
    }
    
    class RelacionDiffCallback : DiffUtil.ItemCallback<DocenteEstudianteEntity>() {
        override fun areItemsTheSame(oldItem: DocenteEstudianteEntity, newItem: DocenteEstudianteEntity): Boolean {
            return oldItem.idDocente == newItem.idDocente && oldItem.idEstudiante == newItem.idEstudiante
        }
        
        override fun areContentsTheSame(oldItem: DocenteEstudianteEntity, newItem: DocenteEstudianteEntity): Boolean {
            return oldItem == newItem
        }
    }
}


