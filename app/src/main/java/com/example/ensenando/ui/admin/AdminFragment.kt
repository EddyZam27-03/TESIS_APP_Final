package com.example.ensenando.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ensenando.databinding.FragmentAdminBinding
import kotlinx.coroutines.launch

class AdminFragment : Fragment() {
    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerViews()
        setupObservers()
        setupClickListeners()
        
        // Cargar datos iniciales
        viewModel.cargarDocentes()
        viewModel.cargarTodosEstudiantes()
        viewModel.cargarRelaciones()
    }
    
    private fun setupRecyclerViews() {
        // RecyclerView de docentes
        val docentesAdapter = DocenteAdminAdapter(
            onVerReporte = { docente ->
                docente.id_usuario?.let { viewModel.generarReporte(it) }
            }
        )
        binding.rvDocentes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDocentes.adapter = docentesAdapter
        
        viewModel.docentes.observe(viewLifecycleOwner) { docentes ->
            docentesAdapter.submitList(docentes)
        }
        
        // RecyclerView de estudiantes
        val estudiantesAdapter = EstudianteAdminAdapter(
            onEstudianteClick = { estudiante ->
                // Mostrar progreso del estudiante
                val idUsuario = estudiante.id_usuario ?: estudiante.id
                if (idUsuario != null) {
                    viewModel.verProgreso(idUsuario)
                }
            },
            onResetClick = { estudiante ->
                // Resetear actividad del estudiante
                val idUsuario = estudiante.id_usuario ?: estudiante.id
                if (idUsuario != null) {
                    lifecycleScope.launch {
                        viewModel.mostrarDialogoReset(idUsuario)
                    }
                }
            },
            onVerReporte = { estudiante ->
                estudiante.id_usuario?.let { viewModel.generarReporte(it) }
            }
        )
        binding.rvEstudiantes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEstudiantes.adapter = estudiantesAdapter
        
        viewModel.estudiantes.observe(viewLifecycleOwner) { estudiantes ->
            estudiantesAdapter.submitList(estudiantes)
        }
        
        // RecyclerView de relaciones
        val relacionesAdapter = RelacionAdminAdapter(
            onEliminar = { relacion ->
                viewModel.eliminarRelacion(relacion.idDocente, relacion.idEstudiante)
            }
        )
        binding.rvRelaciones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRelaciones.adapter = relacionesAdapter
        
        viewModel.relaciones.observe(viewLifecycleOwner) { relaciones ->
            relacionesAdapter.submitList(relaciones)
        }
    }
    
    private fun setupObservers() {
        viewModel.reporteGenerado.observe(viewLifecycleOwner) { result ->
            result.onSuccess { filePath ->
                // Abrir PDF con app externa
                android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        androidx.core.content.FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            java.io.File(filePath)
                        ),
                        "application/pdf"
                    )
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(this)
                }
            }.onFailure { exception ->
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error al generar reporte: ${exception.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        viewModel.relacionEliminada.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                viewModel.cargarRelaciones()
                android.widget.Toast.makeText(
                    requireContext(),
                    "Relación eliminada exitosamente",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }.onFailure { exception ->
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error al eliminar relación: ${exception.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        // Buscar docente (filtro local)
        binding.etBuscarDocente.setOnEditorActionListener { _, _, _ ->
            val busqueda = binding.etBuscarDocente.text.toString()
            if (busqueda.isNotEmpty()) {
                viewModel.buscarDocenteLocal(busqueda)
            } else {
                viewModel.cargarDocentes()
            }
            true
        }
        
        // Buscar estudiante
        binding.etBuscarEstudiante.setOnEditorActionListener { _, _, _ ->
            val busqueda = binding.etBuscarEstudiante.text.toString()
            if (busqueda.isNotEmpty()) {
                viewModel.buscarEstudiante(busqueda)
            } else {
                viewModel.cargarTodosEstudiantes()
            }
            true
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
