package com.academitrack.app.services

import com.academitrack.app.domain.*

class GestorNotas {
    private val evaluaciones = mutableMapOf<String, Evaluacion>()

    fun calcularPromedioActual(idCurso: String): Double {
        val evalCurso = evaluaciones.values.filter { it.getIdCurso() == idCurso && it.notaObtenida != null }
        if (evalCurso.isEmpty()) return 0.0
        var suma = 0.0
        var peso = 0.0
        evalCurso.forEach {
            suma += it.calcularNotaPonderada()
            peso += it.getPorcentaje()
        }
        return if (peso > 0) suma / (peso / 100.0) else 0.0
    }

    fun calcularPorcentajeTotal(idCurso: String): Double = evaluaciones.values.filter { it.getIdCurso() == idCurso }.sumOf { it.getPorcentaje() }
    fun calcularPuntosAcumulados(idCurso: String): Double = evaluaciones.values.filter { it.getIdCurso() == idCurso && it.notaObtenida != null }.sumOf { it.calcularNotaPonderada() }
    fun calcularPorcentajeEvaluado(idCurso: String): Double = evaluaciones.values.filter { it.getIdCurso() == idCurso && it.notaObtenida != null }.sumOf { it.getPorcentaje() }

    fun calcularNotaNecesaria(idCurso: String, notaObjetivo: Double, curso: Curso): ResultadoProyeccion {
        val evalCurso = evaluaciones.values.filter { it.getIdCurso() == idCurso }
        val realizadas = evalCurso.filter { it.notaObtenida != null }
        val pendientes = evalCurso.filter { it.notaObtenida == null }
        var ptsActuales = realizadas.sumOf { it.calcularPuntosObtenidos() }
        var porcRestante = pendientes.sumOf { it.getPorcentaje() }

        if (porcRestante == 0.0) return ResultadoProyeccion(calcularPromedioActual(idCurso), 0.0, ptsActuales >= (notaObjetivo * 100.0 / 7.0), "Curso finalizado")

        val ptsObjetivo = (notaObjetivo / 7.0) * 100.0
        val ptsNecesarios = ptsObjetivo - ptsActuales
        val notaNecesaria = if (porcRestante > 0) (ptsNecesarios / porcRestante) * 7.0 else 0.0
        val esAlcanzable = notaNecesaria <= 7.0
        val mensaje = if (esAlcanzable) "Necesitas un ${String.format("%.1f", notaNecesaria)} en el ${porcRestante.toInt()}% restante" else "Difícil alcanzar un $notaObjetivo"

        return ResultadoProyeccion(calcularPromedioActual(idCurso), notaNecesaria, esAlcanzable, mensaje, porcRestante)
    }

    fun agregarEvaluacion(evaluacion: Evaluacion): Boolean {
        return if (evaluacion.validar()) {
            evaluaciones[evaluacion.getId()] = evaluacion
            true
        } else false
    }

    // ESTAS DOS FUNCIONES SON CRÍTICAS PARA QUE NO TE DE ERROR
    fun eliminarEvaluacion(idEvaluacion: String): Boolean {
        return evaluaciones.remove(idEvaluacion) != null
    }

    fun actualizarEvaluacion(evaluacionActualizada: Evaluacion): Boolean {
        return if (evaluacionActualizada.validar()) {
            // Simplemente reemplazamos la entrada existente con la actualizada, usando su ID
            evaluaciones[evaluacionActualizada.getId()] = evaluacionActualizada
            true
        } else false
    }
    fun obtenerTodasEvaluaciones(): Map<String, Evaluacion> {
        return evaluaciones.toMap()
    }

    fun obtenerEvaluacionesPorCurso(idCurso: String): List<Evaluacion> {
        return evaluaciones.values.filter { it.getIdCurso() == idCurso }
    }
}

data class ResultadoProyeccion(val promedioActual: Double, val notaNecesaria: Double, val esAlcanzable: Boolean, val mensaje: String, val porcentajeRestante: Double = 0.0)