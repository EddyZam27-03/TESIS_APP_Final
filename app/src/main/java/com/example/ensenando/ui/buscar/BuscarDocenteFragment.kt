package com.example.ensenando.ui.buscar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ensenando.databinding.FragmentBuscarDocenteBinding
import kotlinx.coroutines.launch

class BuscarDocenteFragment : Fragment() {
    private var _binding: FragmentBuscarDocenteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BuscarDocenteViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBuscarDocenteBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // Cargar docentes al iniciar
        viewModel.cargarTodosDocentes()
    }
    
    private fun setupRecyclerView() {
        val adapter = DocenteAdapter(
            onDocenteClick = { docente ->
                // Enviar solicitud al docente
                val idDocente = docente.id_usuario ?: docente.id
                if (idDocente != null) {
                    viewModel.enviarSolicitud(idDocente)
                }
            }
        )
        binding.rvDocentes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDocentes.adapter = adapter
        
        viewModel.docentes.observe(viewLifecycleOwner) { docentes ->
            adapter.submitList(docentes)
            binding.tvEmpty.visibility = if (docentes.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    private fun setupObservers() {
        viewModel.solicitudEnviada.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Solicitud enviada exitosamente",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }.onFailure { exception ->
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error al enviar solicitud: ${exception.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // Mostrar/ocultar loading si existe en el layout
            // Por ahora solo deshabilitar botón
            binding.btnBuscar.isEnabled = !isLoading
        }
    }
    
    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        // Buscar docente - filtrado en tiempo real
        binding.etBusqueda.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val busqueda = s?.toString() ?: ""
                if (busqueda.isNotEmpty()) {
                    viewModel.buscarDocente(busqueda)
                } else {
                    viewModel.cargarTodosDocentes()
                }
            }
        })
        
        binding.etBusqueda.setOnEditorActionListener { _, _, _ ->
            val busqueda = binding.etBusqueda.text.toString()
            if (busqueda.isNotEmpty()) {
                viewModel.buscarDocente(busqueda)
            } else {
                viewModel.cargarTodosDocentes()
            }
            true
        }
        
        binding.btnBuscar.setOnClickListener {
            val busqueda = binding.etBusqueda.text.toString()
            if (busqueda.isNotEmpty()) {
                viewModel.buscarDocente(busqueda)
            } else {
                viewModel.cargarTodosDocentes()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
