package com.example.ensenando.data.repository

import android.content.Context
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.remote.ApiService
import com.example.ensenando.data.remote.model.LogrosResponse
import com.example.ensenando.util.NetworkUtils

class LogroRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val apiService: ApiService
) {

    suspend fun getLogrosUsuario(idUsuario: Int): Result<List<LogrosResponse>> {
        return try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                // Intentar cargar desde local si existe implementación
                return Result.success(emptyList())
            }

            val response = apiService.getLogrosUsuarios(idUsuario = idUsuario)
            if (response.isSuccessful) {
                val logros = response.body() ?: emptyList()
                Result.success(logros)
            } else {
                Result.failure(Exception("Error al obtener logros"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verificarYDesbloquearLogros(idUsuario: Int): Result<List<LogrosResponse>> {
        return try {
            // Por ahora retornar lista vacía - implementar lógica de logros en el futuro
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}