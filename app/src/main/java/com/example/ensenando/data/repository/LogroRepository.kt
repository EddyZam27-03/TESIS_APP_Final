package com.example.ensenando.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.ensenando.data.local.AppDatabase
import com.example.ensenando.data.local.entity.UsuarioLogroEntity
import com.example.ensenando.data.remote.ApiService
import com.example.ensenando.data.remote.model.LogrosResponse
import com.example.ensenando.util.NetworkUtils
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class LogroRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val apiService: ApiService
) {

    /**
     * Métricas necesarias para evaluar condiciones de logros
     */
    private data class LogroMetrics(
        val totalGestos: Int,
        val completados: Int,
        val completadosUltimos30: Int,
        val porcentajeTotal: Int,
        val consecutivosCompletos: Int,
        val consecutivosCorrectos10: Boolean,
        val maxPorcentaje: Int,
        val promedioUltimos7: Double,
        val promedioPrevios7: Double,
        val streakDias: Int,
        val regresoTras7Dias: Boolean,
        val relacionesTotales: Int,
        val relacionesAceptadas: Int,
        val reporteReciente: Boolean
    )

    /**
     * Definición de cada logro y su condición
     */
    private val definicionesLogros: List<LogroDefinicion> = listOf(
        LogroDefinicion(
            id = 1,
            titulo = "Primer Paso",
            descripcion = "Has completado tu primera actividad dentro de la app.",
            condicion = { m -> m.completados >= 1 }
        ),
        LogroDefinicion(
            id = 2,
            titulo = "Explorador Inicial",
            descripcion = "Navegaste por todas las secciones principales por primera vez.",
            // Heurística: tiene progreso y ha cargado logros (esta función se llama)
            condicion = { m -> m.completados >= 1 }
        ),
        LogroDefinicion(
            id = 3,
            titulo = "Constancia 1 día",
            descripcion = "Iniciaste sesión y trabajaste en la app durante un día consecutivo.",
            condicion = { m -> m.streakDias >= 1 }
        ),
        LogroDefinicion(
            id = 4,
            titulo = "Aprendiz del Mes",
            descripcion = "Completaste 10 actividades en un mes.",
            condicion = { m -> m.completadosUltimos30 >= 10 }
        ),
        LogroDefinicion(
            id = 5,
            titulo = "Ritmo Constante",
            descripcion = "Realizaste 5 actividades seguidas sin fallar.",
            condicion = { m -> m.consecutivosCompletos >= 5 }
        ),
        LogroDefinicion(
            id = 6,
            titulo = "Dominio Inicial",
            descripcion = "Completaste el 25% del contenido disponible.",
            condicion = { m -> m.porcentajeTotal >= 25 }
        ),
        LogroDefinicion(
            id = 7,
            titulo = "Maestro en Progreso",
            descripcion = "Obtuviste 10 resultados correctos consecutivos.",
            condicion = { m -> m.consecutivosCorrectos10 }
        ),
        LogroDefinicion(
            id = 8,
            titulo = "Perfeccionista",
            descripcion = "Lograste un 100% en una actividad completa.",
            condicion = { m -> m.maxPorcentaje == 100 }
        ),
        LogroDefinicion(
            id = 9,
            titulo = "Superación Personal",
            descripcion = "Mejoraste tu rendimiento respecto a la semana anterior.",
            condicion = { m -> m.promedioPrevios7 > 0 && m.promedioUltimos7 > m.promedioPrevios7 }
        ),
        LogroDefinicion(
            id = 10,
            titulo = "Rutina Semanal",
            descripcion = "Usaste la app 7 días seguidos.",
            condicion = { m -> m.streakDias >= 7 }
        ),
        LogroDefinicion(
            id = 11,
            titulo = "Rutina Mensual",
            descripcion = "Usaste la app 30 días consecutivos.",
            condicion = { m -> m.streakDias >= 30 }
        ),
        LogroDefinicion(
            id = 12,
            titulo = "Vuelta a la Acción",
            descripcion = "Regresaste después de una semana sin actividad.",
            condicion = { m -> m.regresoTras7Dias }
        ),
        LogroDefinicion(
            id = 13,
            titulo = "Participante Activo",
            descripcion = "Enviando tu primera solicitud o interacción con tutor/docente.",
            condicion = { m -> m.relacionesTotales > 0 }
        ),
        LogroDefinicion(
            id = 14,
            titulo = "Apoyo a la Comunidad",
            descripcion = "Ayudaste a otro usuario o completaste una tarea colaborativa.",
            condicion = { m -> m.relacionesAceptadas > 0 }
        ),
        LogroDefinicion(
            id = 15,
            titulo = "Reporte Perfecto",
            descripcion = "Enviaste todos tus reportes correctamente durante una semana.",
            condicion = { m -> m.reporteReciente }
        ),
        LogroDefinicion(
            id = 16,
            titulo = "Nivel Intermedio Alcanzado",
            descripcion = "Has completado todos los contenidos del nivel básico.",
            condicion = { m -> m.porcentajeTotal >= 50 }
        ),
        LogroDefinicion(
            id = 17,
            titulo = "Nivel Avanzado Alcanzado",
            descripcion = "Has completado todos los contenidos del nivel intermedio.",
            condicion = { m -> m.porcentajeTotal >= 75 }
        ),
        LogroDefinicion(
            id = 18,
            titulo = "Dominio Total",
            descripcion = "Completaste el 100% del contenido académico de la app.",
            condicion = { m -> m.porcentajeTotal >= 100 }
        )
    )

    private val logroPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("logros_prefs", Context.MODE_PRIVATE)
    }

    suspend fun getLogrosUsuario(idUsuario: Int): Result<List<LogrosResponse>> {
        return try {
            val logrosDesdeApi: List<LogrosResponse> = if (NetworkUtils.isNetworkAvailable(context)) {
                val response = apiService.getLogrosUsuarios(idUsuario = idUsuario)
                if (response.isSuccessful) {
                    response.body() ?: emptyList()
                } else emptyList()
            } else emptyList()

            val logrosCompletos = mergeConLocal(logrosDesdeApi, idUsuario)
            Result.success(logrosCompletos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verificarYDesbloquearLogros(idUsuario: Int): Result<List<LogrosResponse>> {
        return try {
            val metrics = calcularMetricas(idUsuario)
            val yaDesbloqueados = database.usuarioLogroDao().getLogrosByUsuario(idUsuario).first().map { it.idLogro }.toSet()
            val nuevos = mutableListOf<LogrosResponse>()

            definicionesLogros.forEach { definicion ->
                val cumple = definicion.condicion(metrics)
                if (cumple && !yaDesbloqueados.contains(definicion.id)) {
                    // Registrar en base local
                    database.usuarioLogroDao().insertUsuarioLogro(
                        UsuarioLogroEntity(
                            idUsuario = idUsuario,
                            idLogro = definicion.id,
                            fechaObtenido = fechaAhora()
                        )
                    )
                    nuevos.add(definicion.toResponse(idUsuario))
                }
            }
            Result.success(nuevos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calcula métricas a partir de progreso, relaciones y preferencias locales.
     */
    private suspend fun calcularMetricas(idUsuario: Int): LogroMetrics {
        val now = System.currentTimeMillis()
        val diaMs = 24 * 60 * 60 * 1000L
        val hace7 = now - 7 * diaMs
        val hace14 = now - 14 * diaMs
        val hace30 = now - 30 * diaMs

        val gestos = database.gestoDao().getAllGestos().first()
        val progresos = database.usuarioGestoDao().getProgresoByUsuario(idUsuario).first()
        val totalGestos = gestos.size

        val completados = progresos.count { it.porcentaje >= 80 || it.estado.equals("aprendido", true) }
        val completadosUltimos30 = progresos.count { (it.porcentaje >= 80 || it.estado.equals("aprendido", true)) && it.lastUpdated >= hace30 }
        val porcentajeTotal = if (totalGestos > 0) (completados * 100 / totalGestos) else 0

        val completadosOrdenados = progresos.filter { it.porcentaje >= 80 || it.estado.equals("aprendido", true) }
            .sortedByDescending { it.lastUpdated }
        val consecutivosCompletos = completadosOrdenados.size
        val consecutivosCorrectos10 = consecutivosCompletos >= 10

        val maxPorcentaje = progresos.maxOfOrNull { it.porcentaje } ?: 0
        val promedioUltimos7 = progresos.filter { it.lastUpdated >= hace7 }.map { it.porcentaje }.averageOrNull()
        val promedioPrevios7 = progresos.filter { it.lastUpdated in hace14 until hace7 }.map { it.porcentaje }.averageOrNull()

        val streakInfo = LogroTracker.updateUsoYObtenerStreak(logroPrefs)

        val relacionesEstudiante = database.docenteEstudianteDao().getSolicitudesByEstudiante(idUsuario).first()
        val relacionesDocente = database.docenteEstudianteDao().getEstudiantesByDocente(idUsuario).first()
        val todasRelaciones = relacionesEstudiante + relacionesDocente
        val relacionesTotales = todasRelaciones.size
        val relacionesAceptadas = todasRelaciones.count { it.estado.equals("aceptado", true) }

        val reporteReciente = LogroTracker.reporteGeneradoRecientemente(logroPrefs, now, 7)

        return LogroMetrics(
            totalGestos = totalGestos,
            completados = completados,
            completadosUltimos30 = completadosUltimos30,
            porcentajeTotal = porcentajeTotal,
            consecutivosCompletos = consecutivosCompletos,
            consecutivosCorrectos10 = consecutivosCorrectos10,
            maxPorcentaje = maxPorcentaje,
            promedioUltimos7 = promedioUltimos7,
            promedioPrevios7 = promedioPrevios7,
            streakDias = streakInfo.streak,
            regresoTras7Dias = streakInfo.regresoTras7Dias,
            relacionesTotales = relacionesTotales,
            relacionesAceptadas = relacionesAceptadas,
            reporteReciente = reporteReciente
        )
    }

    /**
     * Combina la respuesta de API con el estado local (desbloqueos guardados).
     * Si la API viene vacía, usa las definiciones locales como catálogo.
     */
    private suspend fun mergeConLocal(logrosApi: List<LogrosResponse>, idUsuario: Int): List<LogrosResponse> {
        val locales = database.usuarioLogroDao().getLogrosByUsuario(idUsuario).first().associateBy { it.idLogro }

        val base = if (logrosApi.isNotEmpty()) logrosApi else definicionesLogros.map { it.toResponse(idUsuario) }

        return base.map { logro ->
            val id = logro.id_logro ?: logro.id ?: -1
            val local = locales[id]
            if (local != null) {
                logro.copy(
                    desbloqueado = true,
                    fechaDesbloqueo = local.fechaObtenido,
                    fecha_obtenido = local.fechaObtenido,
                    porcentajeAvance = logro.porcentajeAvance ?: 100
                )
            } else logro
        }
    }

    private fun fechaAhora(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun List<Int>.averageOrNull(): Double {
        return if (isEmpty()) 0.0 else this.average()
    }

    private data class LogroDefinicion(
        val id: Int,
        val titulo: String,
        val descripcion: String,
        val condicion: (LogroMetrics) -> Boolean
    ) {
        fun toResponse(idUsuario: Int): LogrosResponse =
            LogrosResponse(
                id = id,
                id_logro = id,
                id_usuario = idUsuario,
                titulo = titulo,
                nombre = titulo,
                descripcion = descripcion,
                desbloqueado = true,
                porcentajeAvance = 100,
                fechaDesbloqueo = null,
                fecha_obtenido = null
            )
    }
}

/**
 * Utilidad para rastrear uso diario y eventos (reporte generado).
 */
object LogroTracker {
    private const val KEY_LAST_USE = "last_use"
    private const val KEY_STREAK = "streak_days"
    private const val KEY_LAST_REPORT = "last_report"

    data class StreakInfo(val streak: Int, val regresoTras7Dias: Boolean)

    fun updateUsoYObtenerStreak(prefs: SharedPreferences): StreakInfo {
        val hoy = LocalDate.now(ZoneId.systemDefault())
        val lastUseEpoch = prefs.getLong(KEY_LAST_USE, 0L)
        val lastUseDate = if (lastUseEpoch > 0) Instant.ofEpochMilli(lastUseEpoch).atZone(ZoneId.systemDefault()).toLocalDate() else null

        var streak = prefs.getInt(KEY_STREAK, 0)
        var regresoTras7Dias = false

        if (lastUseDate == null) {
            streak = 1
        } else {
            val diff = java.time.temporal.ChronoUnit.DAYS.between(lastUseDate, hoy).toInt()
            when {
                diff == 0 -> {
                    // mismo día, se mantiene streak
                }
                diff == 1 -> streak += 1
                diff > 1 -> {
                    regresoTras7Dias = diff >= 7
                    streak = 1
                }
            }
        }

        prefs.edit()
            .putLong(KEY_LAST_USE, hoy.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
            .putInt(KEY_STREAK, streak)
            .apply()

        return StreakInfo(streak = streak, regresoTras7Dias = regresoTras7Dias)
    }

    fun marcarReporteGenerado(prefs: SharedPreferences) {
        val ahora = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_REPORT, ahora).apply()
    }

    fun reporteGeneradoRecientemente(prefs: SharedPreferences, now: Long, dias: Int): Boolean {
        val last = prefs.getLong(KEY_LAST_REPORT, 0L)
        if (last == 0L) return false
        val limite = now - dias * 24 * 60 * 60 * 1000L
        return last >= limite
    }
}