package com.example.ensenando.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ensenando.data.local.entity.DocenteEstudianteEntity
import com.example.ensenando.databinding.ItemSolicitudBinding

/**
 * Adapter para mostrar solicitudes de estudiantes o docentes
 * Muestra el estado: pendiente, aceptado, rechazado
 * MEJORADO: Muestra nombres reales de docentes/estudiantes
 */
class SolicitudAdapter(
    private val rol: String,
    private var nombresUsuarios: Map<Int, String> = emptyMap(),
    private val onAceptar: ((Int, Int) -> Unit)? = null,
    private val onRechazar: ((Int, Int) -> Unit)? = null
) : ListAdapter<DocenteEstudianteEntity, SolicitudAdapter.SolicitudViewHolder>(SolicitudDiffCallback()) {
    
    /**
     * Actualiza el mapa de nombres de usuarios
     */
    fun updateNombresUsuarios(nombres: Map<Int, String>) {
        nombresUsuarios = nombres
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SolicitudViewHolder {
        val binding = ItemSolicitudBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SolicitudViewHolder(binding, rol, nombresUsuarios, onAceptar, onRechazar)
    }
    
    override fun onBindViewHolder(holder: SolicitudViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class SolicitudViewHolder(
        private val binding: ItemSolicitudBinding,
        private val rol: String,
        private val nombresUsuarios: Map<Int, String>,
        private val onAceptar: ((Int, Int) -> Unit)?,
        private val onRechazar: ((Int, Int) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(solicitud: DocenteEstudianteEntity) {
            // Mostrar información según el rol con nombres reales
            when (rol) {
                "estudiante" -> {
                    // Para estudiantes: mostrar nombre del docente
                    val nombreDocente = nombresUsuarios[solicitud.idDocente] 
                        ?: "Docente ID: ${solicitud.idDocente}"
                    binding.tvDocenteNombre.text = nombreDocente
                }
                "docente" -> {
                    // Para docentes: mostrar nombre del estudiante
                    val nombreEstudiante = nombresUsuarios[solicitud.idEstudiante] 
                        ?: "Estudiante ID: ${solicitud.idEstudiante}"
                    binding.tvDocenteNombre.text = nombreEstudiante
                }
                else -> {
                    binding.tvDocenteNombre.text = "Relación"
                }
            }
            
            // Mostrar estado con color
            when (solicitud.estado.lowercase()) {
                "pendiente" -> {
                    binding.tvEstado.text = "PENDIENTE"
                    binding.tvEstado.setTextColor(android.graphics.Color.parseColor("#FF9800")) // Naranja
                    binding.tvEstado.visibility = android.view.View.VISIBLE
                }
                "aceptado" -> {
                    binding.tvEstado.text = "ACEPTADO"
                    binding.tvEstado.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Verde
                    binding.tvEstado.visibility = android.view.View.VISIBLE
                }
                "rechazado" -> {
                    binding.tvEstado.text = "RECHAZADO"
                    binding.tvEstado.setTextColor(android.graphics.Color.parseColor("#F44336")) // Rojo
                    binding.tvEstado.visibility = android.view.View.VISIBLE
                }
                else -> {
                    binding.tvEstado.visibility = android.view.View.GONE
                }
            }
            
            // Mostrar botones solo para docentes y solo si está pendiente
            if (rol == "docente" && solicitud.estado.lowercase() == "pendiente") {
                binding.btnAceptar.visibility = android.view.View.VISIBLE
                binding.btnRechazar.visibility = android.view.View.VISIBLE
                
                binding.btnAceptar.setOnClickListener {
                    onAceptar?.invoke(solicitud.idDocente, solicitud.idEstudiante)
                }
                
                binding.btnRechazar.setOnClickListener {
                    onRechazar?.invoke(solicitud.idDocente, solicitud.idEstudiante)
                }
            } else {
                binding.btnAceptar.visibility = android.view.View.GONE
                binding.btnRechazar.visibility = android.view.View.GONE
            }
        }
    }
    
    class SolicitudDiffCallback : DiffUtil.ItemCallback<DocenteEstudianteEntity>() {
        override fun areItemsTheSame(oldItem: DocenteEstudianteEntity, newItem: DocenteEstudianteEntity): Boolean {
            return oldItem.idDocente == newItem.idDocente && oldItem.idEstudiante == newItem.idEstudiante
        }
        
        override fun areContentsTheSame(oldItem: DocenteEstudianteEntity, newItem: DocenteEstudianteEntity): Boolean {
            return oldItem == newItem
        }
    }
}

