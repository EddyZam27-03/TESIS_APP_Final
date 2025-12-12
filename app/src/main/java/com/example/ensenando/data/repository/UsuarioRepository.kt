package com.example.ensenando.data.repository

import android.content.Context
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.local.dao.UsuarioDao
import com.example.ensenando.data.local.entity.UsuarioEntity
import com.example.ensenando.data.remote.ApiService
import com.example.ensenando.data.remote.model.*
import com.example.ensenando.util.NetworkUtils
import com.example.ensenando.util.SecurityUtils
import kotlinx.coroutines.flow.Flow

class UsuarioRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val apiService: ApiService
) {
    private val usuarioDao: UsuarioDao = database.usuarioDao()

    suspend fun login(correo: String, contrasena: String): Result<UsuarioEntity> {
        return try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return Result.failure(Exception("Se requiere conexión a internet para iniciar sesión"))
            }

            android.util.Log.d("UsuarioRepository", "Intentando login para: $correo")

            val response = apiService.login(LoginRequest(correo, contrasena))

            android.util.Log.d("UsuarioRepository", "Response code: ${response.code()}")
            android.util.Log.d("UsuarioRepository", "Response body: ${response.body()}")
            android.util.Log.d("UsuarioRepository", "Response error: ${response.errorBody()?.string()}")

            if (!response.isSuccessful) {
                val errorMsg = when (response.code()) {
                    401 -> "Credenciales inválidas"
                    404 -> "Servidor no encontrado"
                    500 -> "Error del servidor"
                    else -> "Error de conexión (${response.code()})"
                }
                return Result.failure(Exception(errorMsg))
            }

            val body = response.body()
            if (body == null) {
                android.util.Log.e("UsuarioRepository", "Body es null")
                return Result.failure(Exception("Respuesta vacía del servidor"))
            }

            if (!body.success) {
                android.util.Log.e("UsuarioRepository", "Success es false: ${body.message}")
                return Result.failure(Exception(body.message ?: "Error en login"))
            }

            val usuarioResponse = body.usuario
            if (usuarioResponse == null) {
                android.util.Log.e("UsuarioRepository", "Usuario en respuesta es null")
                return Result.failure(Exception("Datos de usuario no recibidos"))
            }

            // Validar ID
            val idUsuario = usuarioResponse.id ?: usuarioResponse.id_usuario
            if (idUsuario == null || idUsuario <= 0) {
                android.util.Log.e("UsuarioRepository", "ID de usuario inválido: $idUsuario")
                return Result.failure(Exception("ID de usuario inválido"))
            }

            val usuario = UsuarioEntity(
                idUsuario = idUsuario,
                nombre = usuarioResponse.nombre,
                correo = usuarioResponse.correo,
                contrasena = null,
                rol = usuarioResponse.rol,
                fechaRegistro = usuarioResponse.fecha_registro ?: "",
                syncStatus = "synced",
                lastUpdated = System.currentTimeMillis()
            )

            // Guardar en base de datos local
            usuarioDao.insertUsuario(usuario)

            // Guardar en preferencias seguras
            SecurityUtils.saveUserId(context, usuario.idUsuario)
            SecurityUtils.saveUserRol(context, usuario.rol)
            SecurityUtils.saveUserNombre(context, usuario.nombre)
            SecurityUtils.saveUserCorreo(context, usuario.correo)
            body.token?.let { token -> SecurityUtils.saveToken(context, token) }

            android.util.Log.d("UsuarioRepository", "Login exitoso para usuario: ${usuario.nombre}")
            Result.success(usuario)

        } catch (e: com.google.gson.JsonSyntaxException) {
            android.util.Log.e("UsuarioRepository", "Error de JSON", e)
            Result.failure(Exception("Error al procesar respuesta del servidor. Verifique que el backend esté devolviendo JSON válido."))
        } catch (e: java.net.UnknownHostException) {
            android.util.Log.e("UsuarioRepository", "Host no encontrado", e)
            Result.failure(Exception("No se pudo conectar al servidor. Verifique la URL del backend."))
        } catch (e: java.net.SocketTimeoutException) {
            android.util.Log.e("UsuarioRepository", "Timeout", e)
            Result.failure(Exception("Tiempo de espera agotado. Verifique su conexión."))
        } catch (e: Exception) {
            android.util.Log.e("UsuarioRepository", "Error en login", e)
            Result.failure(Exception("Error: ${e.message}"))
        }
    }

    suspend fun register(nombre: String, correo: String, contrasena: String, rol: String? = null): Result<UsuarioEntity> {
        return try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return Result.failure(Exception("Se requiere conexión para registrar"))
            }

            val response = apiService.register(RegisterRequest(nombre, correo, contrasena, rol ?: "estudiante"))
            val body = response.body()

            if (response.isSuccessful && body?.success == true) {
                val usuarioResponse = body.usuario ?: return Result.failure(Exception("Respuesta sin usuario"))

                val idUsuario = usuarioResponse.id ?: usuarioResponse.id_usuario
                if (idUsuario == null || idUsuario <= 0) {
                    return Result.failure(Exception("ID de usuario inválido en respuesta del servidor"))
                }

                val usuario = UsuarioEntity(
                    idUsuario = idUsuario,
                    nombre = usuarioResponse.nombre,
                    correo = usuarioResponse.correo,
                    contrasena = null,
                    rol = usuarioResponse.rol,
                    fechaRegistro = usuarioResponse.fecha_registro ?: "",
                    syncStatus = "synced",
                    lastUpdated = System.currentTimeMillis()
                )

                usuarioDao.insertUsuario(usuario)
                SecurityUtils.saveUserId(context, usuario.idUsuario)
                SecurityUtils.saveUserRol(context, usuario.rol)
                SecurityUtils.saveUserNombre(context, usuario.nombre)
                SecurityUtils.saveUserCorreo(context, usuario.correo)
                body.token?.let { token -> SecurityUtils.saveToken(context, token) }

                Result.success(usuario)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error en registro"))
            }
        } catch (e: Exception) {
            android.util.Log.e("UsuarioRepository", "Error en registro", e)
            Result.failure(Exception("Error: ${e.message}"))
        }
    }

    fun getUsuarioById(id: Int): Flow<UsuarioEntity?> {
        return kotlinx.coroutines.flow.flow {
            emit(usuarioDao.getUsuarioById(id))
        }
    }

    suspend fun getUsuarioByIdSuspend(id: Int): UsuarioEntity? {
        return usuarioDao.getUsuarioById(id)
    }

    fun getAllUsuarios(): Flow<List<UsuarioEntity>> {
        return usuarioDao.getAllUsuarios()
    }
}