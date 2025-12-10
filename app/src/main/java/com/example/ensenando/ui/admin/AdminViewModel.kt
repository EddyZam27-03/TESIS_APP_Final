package com.example.ensenando.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.local.entity.DocenteEstudianteEntity
import com.example.ensenando.data.remote.RetrofitClient
import com.example.ensenando.data.remote.model.ProgresoDetalle
import com.example.ensenando.data.remote.model.UsuarioResponse
import com.example.ensenando.data.repository.DocenteEstudianteRepository
import com.example.ensenando.data.repository.ProgresoRepository
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.apiService
    private val docenteEstudianteRepository = DocenteEstudianteRepository(application, database, apiService)
    private val progresoRepository = ProgresoRepository(application, database, apiService)
    
    private val _estudiantes = MutableLiveData<List<UsuarioResponse>>()
    val estudiantes: LiveData<List<UsuarioResponse>> = _estudiantes
    
    private val _docentes = MutableLiveData<List<UsuarioResponse>>()
    val docentes: LiveData<List<UsuarioResponse>> = _docentes
    
    private val _relaciones = MutableLiveData<List<DocenteEstudianteEntity>>()
    val relaciones: LiveData<List<DocenteEstudianteEntity>> = _relaciones
    
    private val _reporteGenerado = MutableLiveData<Result<String>>()
    val reporteGenerado: LiveData<Result<String>> = _reporteGenerado
    
    private val _relacionEliminada = MutableLiveData<Result<Unit>>()
    val relacionEliminada: LiveData<Result<Unit>> = _relacionEliminada
    
    private val _progresoEstudiante = MutableLiveData<List<ProgresoDetalle>>()
    val progresoEstudiante: LiveData<List<ProgresoDetalle>> = _progresoEstudiante
    
    fun cargarTodosEstudiantes() {
        viewModelScope.launch {
            try {
                val response = apiService.getProgresoUsuarios(idAdmin = SecurityUtils.getUserId(getApplication()))
                if (response.isSuccessful) {
                    val progreso = response.body()?.progreso ?: emptyList()
                    val estudiantes = progreso.map { progresoDetalle ->
                        UsuarioResponse(
                            id_usuario = progresoDetalle.id_usuario,
                            nombre = progresoDetalle.nombre ?: "",
                            correo = progresoDetalle.correo ?: "",
                            rol = "estudiante"
                        )
                    }
                    _estudiantes.value = estudiantes
                }
            } catch (e: Exception) {
                _estudiantes.value = emptyList()
            }
        }
    }
    
    fun cargarDocentes() {
        viewModelScope.launch {
            try {
                val response = apiService.listarDocentes()
                if (response.isSuccessful) {
                    _docentes.value = response.body() ?: emptyList()
                } else {
                    _docentes.value = emptyList()
                }
            } catch (e: Exception) {
                _docentes.value = emptyList()
            }
        }
    }
    
    fun buscarEstudiante(busqueda: String) {
        viewModelScope.launch {
            try {
                val response = apiService.buscarEstudiante(busqueda = busqueda)
                if (response.isSuccessful) {
                    val resultados = response.body() ?: emptyList()
                    val estudiantes = resultados.filter { it.rol == "estudiante" }
                    _estudiantes.value = estudiantes
                }
            } catch (e: Exception) {
                _estudiantes.value = emptyList()
            }
        }
    }
    
    fun buscarDocenteLocal(query: String) {
        val lista = _docentes.value ?: return
        val filtrados = lista.filter { it.nombre.contains(query, ignoreCase = true) || it.correo.contains(query, ignoreCase = true) }
        _docentes.value = filtrados
    }
    
    fun cargarRelaciones() {
        viewModelScope.launch {
            try {
                // Cargar todas las relaciones desde la base de datos local
                // O hacer una llamada especial para admin
                val relaciones = docenteEstudianteRepository.getEstudiantesByDocente(0).first()
                _relaciones.value = relaciones
            } catch (e: Exception) {
                _relaciones.value = emptyList()
            }
        }
    }
    
    fun generarReporte(idUsuario: Int) {
        viewModelScope.launch {
            try {
                val progresos = progresoRepository.getProgresoByUsuario(idUsuario).first()
                val usuario = database.usuarioDao().getUsuarioById(idUsuario)
                    ?: com.example.ensenando.data.local.entity.UsuarioEntity(
                        idUsuario = idUsuario,
                        nombre = _docentes.value?.firstOrNull { it.id_usuario == idUsuario }?.nombre
                            ?: _estudiantes.value?.firstOrNull { it.id_usuario == idUsuario }?.nombre
                            ?: "Usuario $idUsuario",
                        correo = _docentes.value?.firstOrNull { it.id_usuario == idUsuario }?.correo
                            ?: _estudiantes.value?.firstOrNull { it.id_usuario == idUsuario }?.correo
                            ?: "correo@desconocido.com",
                        contrasena = null,
                        rol = _docentes.value?.firstOrNull { it.id_usuario == idUsuario }?.rol
                            ?: _estudiantes.value?.firstOrNull { it.id_usuario == idUsuario }?.rol
                            ?: "desconocido",
                        fechaRegistro = "",
                        syncStatus = "synced",
                        lastUpdated = System.currentTimeMillis()
                    )
                
                val reporte = generarReportePDF(usuario, progresos)
                _reporteGenerado.value = Result.success(reporte)
            } catch (e: Exception) {
                _reporteGenerado.value = Result.failure(e)
            }
        }
    }
    
    fun resetActividad(idUsuario: Int, idGesto: Int) {
        viewModelScope.launch {
            try {
                progresoRepository.updateProgreso(idUsuario, idGesto, 0)
                // Toast removido - el ViewModel no debe mostrar UI directamente
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun verProgreso(idUsuario: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.getProgresoUsuarios(
                    idAdmin = SecurityUtils.getUserId(getApplication()),
                    idEstudiante = idUsuario
                )
                if (response.isSuccessful) {
                    val progreso = response.body()?.progreso ?: emptyList()
                    _progresoEstudiante.value = progreso
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun eliminarRelacion(idDocente: Int, idEstudiante: Int) {
        viewModelScope.launch {
            val result = docenteEstudianteRepository.eliminarRelacion(idDocente, idEstudiante)
            _relacionEliminada.value = result
        }
    }
    
    fun mostrarDialogoReset(idUsuario: Int) {
        // TODO: Implementar diálogo para seleccionar gesto
        // Por ahora, resetear todos los gestos
        viewModelScope.launch {
            val progresos = progresoRepository.getProgresoByUsuario(idUsuario).first()
            progresos.forEach { progreso ->
                progresoRepository.updateProgreso(idUsuario, progreso.idGesto, 0)
            }
        }
    }
    
    private suspend fun generarReportePDF(
        usuario: com.example.ensenando.data.local.entity.UsuarioEntity,
        progresos: List<com.example.ensenando.data.local.entity.UsuarioGestoEntity>
    ): String {
        val context = getApplication<android.app.Application>()
        
        // Obtener nombres de gestos
        val gestosMap = mutableMapOf<Int, String>()
        try {
            val gestosList = database.gestoDao().getAllGestos().first()
            gestosList.forEach { gesto ->
                gestosMap[gesto.idGesto] = gesto.nombre
            }
        } catch (e: Exception) {
            // Si no hay gestos, continuar sin nombres
        }
        
        // Generar PDF usando PdfGenerator
        return com.example.ensenando.util.PdfGenerator.generarReportePDF(
            context,
            usuario,
            progresos,
            gestosMap
        )
    }
}

