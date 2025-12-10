package com.example.ensenando.data.local.dao

import androidx.room.*
import com.example.ensenando.data.local.entity.DocenteEstudianteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocenteEstudianteDao {
    @Query("SELECT * FROM docenteestudiante WHERE id_estudiante = :idEstudiante")
    fun getSolicitudesByEstudiante(idEstudiante: Int): Flow<List<DocenteEstudianteEntity>>
    
    @Query("SELECT * FROM docenteestudiante WHERE id_docente = :idDocente")
    fun getEstudiantesByDocente(idDocente: Int): Flow<List<DocenteEstudianteEntity>>
    
    @Query("SELECT * FROM docenteestudiante WHERE id_docente = :idDocente AND id_estudiante = :idEstudiante")
    suspend fun getRelacion(idDocente: Int, idEstudiante: Int): DocenteEstudianteEntity?
    
    @Query("SELECT * FROM docenteestudiante WHERE id_estudiante = :idEstudiante AND estado = 'pendiente'")
    fun getSolicitudesPendientes(idEstudiante: Int): Flow<List<DocenteEstudianteEntity>>
    
    @Query("SELECT * FROM docenteestudiante WHERE id_estudiante = :idEstudiante AND estado = 'aceptado'")
    fun getDocentesAceptados(idEstudiante: Int): Flow<List<DocenteEstudianteEntity>>
    
    @Query("SELECT * FROM docenteestudiante WHERE sync_status = 'pending'")
    suspend fun getPendingRelaciones(): List<DocenteEstudianteEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelacion(relacion: DocenteEstudianteEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelaciones(relaciones: List<DocenteEstudianteEntity>)
    
    @Update
    suspend fun updateRelacion(relacion: DocenteEstudianteEntity)
    
    @Query("UPDATE docenteestudiante SET estado = :estado, sync_status = 'pending', last_updated = :timestamp WHERE id_docente = :idDocente AND id_estudiante = :idEstudiante")
    suspend fun updateEstado(idDocente: Int, idEstudiante: Int, estado: String, timestamp: Long)
    
    @Query("UPDATE docenteestudiante SET sync_status = :status WHERE id_docente = :idDocente AND id_estudiante = :idEstudiante")
    suspend fun updateSyncStatus(idDocente: Int, idEstudiante: Int, status: String)
    
    @Delete
    suspend fun deleteRelacion(relacion: DocenteEstudianteEntity)
    
    @Query("DELETE FROM docenteestudiante WHERE id_docente = :idDocente AND id_estudiante = :idEstudiante")
    suspend fun deleteRelacion(idDocente: Int, idEstudiante: Int)
}


