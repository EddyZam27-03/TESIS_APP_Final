package com.example.ensenando.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ensenando.databinding.FragmentProfileBinding
import com.example.ensenando.ui.welcome.WelcomeActivity
import com.example.ensenando.util.SecurityUtils
import java.io.File

/**
 * Fragment para mostrar perfil del usuario
 * - Muestra información del usuario
 * - Muestra solicitudes (solo estudiantes)
 * - Permite descargar reporte PDF
 * - Permite cerrar sesión
 */
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        setupClickListeners()
        setupRecyclerView()
    }
    
    private fun setupObservers() {
        // Observar usuario
        viewModel.usuario.observe(viewLifecycleOwner) { usuario ->
            usuario?.let { usuarioEntity: com.example.ensenando.data.local.entity.UsuarioEntity ->
                binding.tvNombre.text = usuarioEntity.nombre
                binding.tvCorreo.text = usuarioEntity.correo
                binding.tvRol.text = usuarioEntity.rol.uppercase()
            }
        }
        
        // Observar solicitudes
        viewModel.solicitudes.observe(viewLifecycleOwner) { solicitudes ->
            val rol = SecurityUtils.getUserRol(requireContext())
            if (solicitudes.isNotEmpty()) {
                binding.tvSolicitudesTitle.visibility = View.VISIBLE
                binding.rvSolicitudes.visibility = View.VISIBLE
            } else {
                binding.tvSolicitudesTitle.visibility = View.GONE
                binding.rvSolicitudes.visibility = View.GONE
            }
        }
        
        // Observar reporte generado
        viewModel.reporteGenerado.observe(viewLifecycleOwner) { result ->
            result.onSuccess { filePath ->
                // ✅ MEJORADO: Mostrar reporte en pantalla y permitir descarga
                mostrarReporteEnPantalla(filePath)
            }.onFailure { exception ->
                android.util.Log.e("ProfileFragment", "Error al generar reporte", exception)
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error al generar reporte: ${exception.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun setupClickListeners() {
        // Botón Ver Logros
        binding.btnLogros.setOnClickListener {
            findNavController().navigate(com.example.ensenando.R.id.logrosFragment)
        }
        
        // Botón Buscar Docente (solo para estudiantes)
        val rol = SecurityUtils.getUserRol(requireContext())
        if (rol == "estudiante") {
            binding.btnBuscarDocente.visibility = View.VISIBLE
            binding.btnBuscarDocente.setOnClickListener {
                findNavController().navigate(com.example.ensenando.R.id.buscarDocenteFragment)
            }
        }
        
        // Botón Ver/Generar Reporte
        binding.btnDescargarReporte.text = "Ver Reporte" // Cambiar texto
        binding.btnDescargarReporte.setOnClickListener {
            val idUsuario = SecurityUtils.getUserId(requireContext())
            if (idUsuario != -1) {
                viewModel.generarReporte(idUsuario)
            }
        }
        
        // Botón Cerrar Sesión
        binding.btnCerrarSesion.setOnClickListener {
            viewModel.logout()
            val intent = Intent(requireContext(), WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
        
        // Configurar toolbar
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
    
    private fun setupRecyclerView() {
        val rol = SecurityUtils.getUserRol(requireContext())
        val adapter = SolicitudAdapter(
            rol = rol,
            nombresUsuarios = emptyMap(), // Se actualizará cuando se carguen los nombres
            onAceptar = { idDocente, idEstudiante ->
                viewModel.aceptarSolicitud(idDocente, idEstudiante)
                android.widget.Toast.makeText(
                    requireContext(),
                    "Solicitud aceptada",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            onRechazar = { idDocente, idEstudiante ->
                viewModel.rechazarSolicitud(idDocente, idEstudiante)
                android.widget.Toast.makeText(
                    requireContext(),
                    "Solicitud rechazada",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        )
        binding.rvSolicitudes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSolicitudes.adapter = adapter
        
        viewModel.solicitudes.observe(viewLifecycleOwner) { solicitudes ->
            adapter.submitList(solicitudes)
        }
        
        // Actualizar nombres cuando se carguen
        viewModel.nombresUsuarios.observe(viewLifecycleOwner) { nombres ->
            adapter.updateNombresUsuarios(nombres)
        }
    }
    
    /**
     * ✅ NUEVO: Muestra el reporte en pantalla usando DialogFragment
     */
    private fun mostrarReporteEnPantalla(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "El archivo del reporte no existe",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return
            }
            
            // Crear un DialogFragment para mostrar el PDF
            val dialog = ReporteDialogFragment.newInstance(filePath)
            dialog.show(parentFragmentManager, "ReporteDialog")
            
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "Error al mostrar reporte", e)
            android.widget.Toast.makeText(
                requireContext(),
                "Error al mostrar reporte: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

