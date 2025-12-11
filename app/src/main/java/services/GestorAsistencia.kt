package com.academitrack.app.services

import com.academitrack.app.domain.*

class GestorAsistencia {

    private val asistencias = mutableMapOf<String, Asistencia>()

    fun registrarAsistencia(asistencia: Asistencia): Boolean {
        return if (asistencia.validar()) {
            asistencias[asistencia.getId()] = asistencia
            true
        } else false
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
        }
    }
}

data class EstadisticasAsistencia(
    val totalClases: Int,
    val clasesAsistidas: Int,
    val faltas: Int,
    val porcentajeAsistencia: Double
)