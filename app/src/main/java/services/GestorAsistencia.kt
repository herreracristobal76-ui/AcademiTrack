package com.academitrack.app.services

import com.academitrack.app.domain.*

class GestorAsistencia {

    private val asistencias = mutableMapOf<String, Asistencia>()


    fun registrarOActualizar(idCurso: String, fecha: Long, estado: EstadoAsistencia) {

        val existente = asistencias.values.find {
            it.getIdCurso() == idCurso && it.getFecha() == fecha
        }

        if (existente != null) {

            existente.setEstado(estado)
        } else {

            val nueva = Asistencia(
                idAsistencia = "asist_${System.currentTimeMillis()}",
                idCurso = idCurso,
                fecha = fecha,
                estado = estado
            )
            asistencias[nueva.getId()] = nueva
        }
    }

    fun registrarAsistencia(asistencia: Asistencia): Boolean {
        return if (asistencia.validar()) {
            asistencias[asistencia.getId()] = asistencia
            true
        } else false
    }

    fun eliminarAsistencia(idAsistencia: String): Boolean {
        return asistencias.remove(idAsistencia) != null
    }

    fun actualizarEstadoAsistencia(idAsistencia: String, nuevoEstado: EstadoAsistencia): Boolean {
        val asistencia = asistencias[idAsistencia] ?: return false
        asistencia.setEstado(nuevoEstado)
        return true
    }

    fun calcularPorcentajeAsistencia(idCurso: String): Double {
        val asistenciasCurso = asistencias.values.filter {
            it.getIdCurso() == idCurso && it.cuentaParaPorcentaje()
        }

        if (asistenciasCurso.isEmpty()) return 100.0

        val clasesValidas = asistenciasCurso.size
        val clasesAsistidas = asistenciasCurso.count { it.esAsistenciaValida() }

        return (clasesAsistidas.toDouble() / clasesValidas.toDouble()) * 100.0
    }

    fun obtenerEstadisticas(idCurso: String): EstadisticasAsistencia {
        val asistenciasCurso = asistencias.values.filter { it.getIdCurso() == idCurso }

        val totalClases = asistenciasCurso.count { it.cuentaParaPorcentaje() }
        val asistidas = asistenciasCurso.count { it.esAsistenciaValida() }
        val faltas = asistenciasCurso.count { it.esFalta() }

        val porcentaje = if (totalClases > 0) {
            (asistidas.toDouble() / totalClases.toDouble()) * 100.0
        } else 100.0

        return EstadisticasAsistencia(
            totalClases = totalClases,
            clasesAsistidas = asistidas,
            faltas = faltas,
            porcentajeAsistencia = porcentaje
        )
    }

    fun obtenerFaltas(idCurso: String): List<Asistencia> {
        return asistencias.values.filter {
            it.getIdCurso() == idCurso && it.esFalta()
        }.sortedByDescending { it.getFecha() }
    }

    fun obtenerAsistenciasPorCurso(idCurso: String): List<Asistencia> {
        return asistencias.values.filter {
            it.getIdCurso() == idCurso
        }.sortedByDescending { it.getFecha() }
    }

    fun obtenerAsistenciasPorFecha(idCurso: String, fechaInicio: Long, fechaFin: Long): List<Asistencia> {
        return asistencias.values.filter {
            (idCurso.isEmpty() || it.getIdCurso() == idCurso) &&
                    it.getFecha() >= fechaInicio &&
                    it.getFecha() <= fechaFin
        }.sortedBy { it.getFecha() }
    }

    fun obtenerTodasAsistencias(): Map<String, Asistencia> {
        return asistencias.toMap()
    }

    fun cargarAsistencias(asistenciasCargadas: Map<String, Asistencia>) {
        asistencias.clear()
        asistencias.putAll(asistenciasCargadas)
    }
}

data class EstadisticasAsistencia(
    val totalClases: Int,
    val clasesAsistidas: Int,
    val faltas: Int,
    val porcentajeAsistencia: Double
)