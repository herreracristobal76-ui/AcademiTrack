package com.academitrack.app.services

import com.academitrack.app.domain.*

class GestorNotas {

    private val evaluaciones = mutableMapOf<String, Evaluacion>()

    fun calcularPromedioActual(idCurso: String): Double {
        val evaluacionesCurso = evaluaciones.values.filter {
            it.getIdCurso() == idCurso && it.notaObtenida != null
        }

        if (evaluacionesCurso.isEmpty()) return 0.0

        var sumaPonderada = 0.0
        var porcentajeAcumulado = 0.0

        evaluacionesCurso.forEach { eval ->
            sumaPonderada += eval.calcularNotaPonderada()
            porcentajeAcumulado += eval.getPorcentaje()
        }

        return if (porcentajeAcumulado > 0) {
            (sumaPonderada / porcentajeAcumulado) * 100.0 / 100.0
        } else 0.0
    }

    fun calcularNotaNecesaria(
        idCurso: String,
        notaObjetivo: Double,
        curso: Curso
    ): ResultadoProyeccion {
        val evaluacionesCurso = evaluaciones.values.filter { it.getIdCurso() == idCurso }
        val realizadas = evaluacionesCurso.filter { it.notaObtenida != null }
        val pendientes = evaluacionesCurso.filter { it.notaObtenida == null }

        var puntosActuales = 0.0
        realizadas.forEach { eval ->
            puntosActuales += eval.calcularPuntosObtenidos()
        }

        var porcentajeRestante = 0.0
        pendientes.forEach { eval ->
            porcentajeRestante += eval.getPorcentaje()
        }

        if (porcentajeRestante == 0.0) {
            return ResultadoProyeccion(
                promedioActual = calcularPromedioActual(idCurso),
                notaNecesaria = 0.0,
                esAlcanzable = puntosActuales >= (notaObjetivo * 100.0 / 7.0),
                mensaje = "No hay evaluaciones pendientes"
            )
        }

        val puntosObjetivo = (notaObjetivo / 7.0) * 100.0
        val puntosNecesarios = puntosObjetivo - puntosActuales
        val notaNecesaria = if (porcentajeRestante > 0) {
            (puntosNecesarios / porcentajeRestante) * 7.0
        } else 0.0

        val esAlcanzable = notaNecesaria <= 7.0
        val mensaje = when {
            esAlcanzable && notaNecesaria > 0 ->
                "Necesitas sacar ${String.format("%.2f", notaNecesaria)} en las evaluaciones restantes"
            esAlcanzable && notaNecesaria <= 0 ->
                "¡Ya aprobaste el curso!"
            else ->
                "No es posible alcanzar la nota $notaObjetivo"
        }

        return ResultadoProyeccion(
            promedioActual = calcularPromedioActual(idCurso),
            notaNecesaria = notaNecesaria,
            esAlcanzable = esAlcanzable,
            mensaje = mensaje,
            porcentajeRestante = porcentajeRestante
        )
    }

    fun agregarEvaluacion(evaluacion: Evaluacion): Boolean {
        return if (evaluacion.validar()) {
            evaluaciones[evaluacion.getId()] = evaluacion
            true
        } else false
    }

    fun obtenerEvaluacionesPorCurso(idCurso: String): List<Evaluacion> {
        return evaluaciones.values.filter { it.getIdCurso() == idCurso }
    }
}

data class ResultadoProyeccion(
    val promedioActual: Double,
    val notaNecesaria: Double,
    val esAlcanzable: Boolean,
    val mensaje: String,
    val porcentajeRestante: Double = 0.0
)