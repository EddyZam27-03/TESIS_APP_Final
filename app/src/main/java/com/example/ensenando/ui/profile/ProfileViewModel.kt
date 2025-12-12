package com.example.ensenando.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.local.entity.DocenteEstudianteEntity
import com.example.ensenando.data.local.entity.UsuarioEntity
import com.example.ensenando.data.repository.DocenteEstudianteRepository
import com.example.ensenando.data.repository.ProgresoRepository
import com.example.ensenando.data.repository.UsuarioRepository
import com.example.ensenando.data.repository.LogroTracker
import com.example.ensenando.data.remote.RetrofitClient
import com.example.ensenando.data.repository.LogroRepository
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.apiService
    private val usuarioRepository = UsuarioRepository(application, database, apiService)
    private val progresoRepository = ProgresoRepository(application, database, apiService)
    private val docenteEstudianteRepository = DocenteEstudianteRepository(application, database, apiService)
    
    private val _usuario = MutableLiveData<UsuarioEntity?>()
    val usuario: LiveData<UsuarioEntity?> = _usuario
    
    private val _solicitudes = MutableLiveData<List<DocenteEstudianteEntity>>()
    val solicitudes: LiveData<List<DocenteEstudianteEntity>> = _solicitudes
    
    // Mapa de nombres de usuarios para mostrar en solicitudes
    private val _nombresUsuarios = MutableLiveData<Map<Int, String>>()
    val nombresUsuarios: LiveData<Map<Int, String>> = _nombresUsuarios
    
    private val _reporteGenerado = MutableLiveData<Result<String>>()
    val reporteGenerado: LiveData<Result<String>> = _reporteGenerado
    
    init {
        loadUsuario()
        loadSolicitudes()
        loadNombresUsuarios()
    }
    
    /**
     * Carga los nombres de todos los usuarios para mostrar en solicitudes
     */
    private fun loadNombresUsuarios() {
        viewModelScope.launch {
            try {
                val usuarios = usuarioRepository.getAllUsuarios().first()
                val nombresMap = usuarios.associate { it.idUsuario to it.nombre }
                _nombresUsuarios.value = nombresMap
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error al cargar nombres de usuarios", e)
                _nombresUsuarios.value = emptyMap()
            }
        }
    }
    
    private fun loadUsuario() {
        viewModelScope.launch {
            val idUsuario = SecurityUtils.getUserId(getApplication())
            if (idUsuario != -1) {
                val usuario = usuarioRepository.getUsuarioByIdSuspend(idUsuario)
                _usuario.value = usuario
            }
        }
    }
    
    private fun loadSolicitudes() {
        viewModelScope.launch {
            val idUsuario = SecurityUtils.getUserId(getApplication())
            val rol = SecurityUtils.getUserRol(getApplication())
            
            if (idUsuario != -1) {
                try {
                    when (rol) {
                        "estudiante" -> {
                            val response = apiService.consultarSolicitudEstudiante(idEstudiante = idUsuario)
                            val body = response.body()
                            if (response.isSuccessful && body?.success == true) {
                                val solicitudesResponse = body.solicitudes ?: emptyList()
                                val relaciones = solicitudesResponse.map { solicitud ->
                                    DocenteEstudianteEntity(
                                        idDocente = solicitud.id_docente,
                                        idEstudiante = solicitud.id_estudiante,
                                        estado = solicitud.estado,
                                        syncStatus = "synced",
                                        lastUpdated = System.currentTimeMillis()
                                    )
                                }
                                _solicitudes.value = relaciones
                            } else {
                                // Fallback: cargar desde local
                                val solicitudesList = docenteEstudianteRepository.getSolicitudesPendientes(idUsuario).first()
                                _solicitudes.value = solicitudesList
                            }
                        }
                        "docente" -> {
                            val response = apiService.listarSolicitudesDocente(idDocente = idUsuario)
                            val body = response.body()
                            if (response.isSuccessful && body?.success == true) {
                                val solicitudesResponse = body.solicitudes ?: emptyList()
                                val relaciones = solicitudesResponse.map { solicitud ->
                                    DocenteEstudianteEntity(
                                        idDocente = solicitud.id_docente,
                                        idEstudiante = solicitud.id_estudiante,
                                        estado = solicitud.estado,
                                        syncStatus = "synced",
                                        lastUpdated = System.currentTimeMillis()
                                    )
                                }
                                _solicitudes.value = relaciones
                            } else {
                                // Fallback: cargar desde local
                                val estudiantesList = docenteEstudianteRepository.getEstudiantesByDocente(idUsuario).first()
                                _solicitudes.value = estudiantesList
                            }
                        }
                        "administrador" -> {
                            // Administrador puede ver todas las relaciones
                            val relacionesList = docenteEstudianteRepository.getEstudiantesByDocente(idUsuario).first()
                            _solicitudes.value = relacionesList
                        }
                    }
                } catch (e: Exception) {
                    // Fallback: cargar desde local
                    when (rol) {
                        "estudiante" -> {
                            val solicitudesList = docenteEstudianteRepository.getSolicitudesPendientes(idUsuario).first()
                            _solicitudes.value = solicitudesList
                        }
                        else -> {
                            val estudiantesList = docenteEstudianteRepository.getEstudiantesByDocente(idUsuario).first()
                            _solicitudes.value = estudiantesList
                        }
                    }
                }
            }
        }
    }
    
    fun aceptarSolicitud(idDocente: Int, idEstudiante: Int) {
        viewModelScope.launch {
            val result = docenteEstudianteRepository.actualizarSolicitud(idDocente, idEstudiante, "aceptado")
            if (result.isSuccess) {
                loadSolicitudes()
            }
        }
    }
    
    fun rechazarSolicitud(idDocente: Int, idEstudiante: Int) {
        viewModelScope.launch {
            val result = docenteEstudianteRepository.actualizarSolicitud(idDocente, idEstudiante, "rechazado")
            if (result.isSuccess) {
                loadSolicitudes()
            }
        }
    }
    
    fun eliminarRelacion(idDocente: Int, idEstudiante: Int) {
        viewModelScope.launch {
            val result = docenteEstudianteRepository.eliminarRelacion(idDocente, idEstudiante)
            if (result.isSuccess) {
                loadSolicitudes()
            }
        }
    }
    
    fun generarReporte(idUsuario: Int, formato: String = "pdf") {
        viewModelScope.launch {
            try {
                // Por ahora, generar reporte localmente desde datos locales
                // TODO: Implementar endpoint de reporte si existe
                val progresos = progresoRepository.getProgresoByUsuario(idUsuario).first()
                val usuario = usuarioRepository.getUsuarioByIdSuspend(idUsuario)
                
                if (usuario != null) {
                    // Generar reporte simple (texto por ahora)
                    val reporte = generarReporteLocal(usuario, progresos)
                    // Marcar reporte generado para logros
                    LogroTracker.marcarReporteGenerado(
                        getApplication<Application>().getSharedPreferences("logros_prefs", android.content.Context.MODE_PRIVATE)
                    )
                    _reporteGenerado.value = Result.success(reporte)
                } else {
                    _reporteGenerado.value = Result.failure(Exception("Usuario no encontrado"))
                }
            } catch (e: Exception) {
                _reporteGenerado.value = Result.failure(e)
            }
        }
    }
    
    private suspend fun generarReporteLocal(usuario: com.example.ensenando.data.local.entity.UsuarioEntity, progresos: List<com.example.ensenando.data.local.entity.UsuarioGestoEntity>): String {
        val context = getApplication<Application>()
        
        // Obtener nombres de gestos y gestos completos
        val gestosMap = mutableMapOf<Int, String>()
        val gestosCompletosMap = mutableMapOf<Int, com.example.ensenando.data.local.entity.GestoEntity>()
        try {
            val gestosList = database.gestoDao().getAllGestos().first()
            gestosList.forEach { gesto ->
                gestosMap[gesto.idGesto] = gesto.nombre
                gestosCompletosMap[gesto.idGesto] = gesto
            }
        } catch (e: Exception) {
            // Si no hay gestos, continuar sin nombres
        }
        
        // Generar PDF usando PdfGenerator mejorado
        return com.example.ensenando.util.PdfGenerator.generarReportePDF(
            context,
            usuario,
            progresos,
            gestosMap,
            gestosCompletosMap
        )
    }
    
    fun resetActividad(idUsuario: Int, idGesto: Int) {
        viewModelScope.launch {
            try {
                // Resetear localmente
                progresoRepository.updateProgreso(idUsuario, idGesto, 0)
                loadUsuario()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // ✅ FIX: Método para cerrar sesión
    fun logout() {
        SecurityUtils.clearAll(getApplication())
    }
}

