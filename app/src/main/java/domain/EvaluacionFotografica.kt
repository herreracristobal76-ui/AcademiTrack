package com.academitrack.app.domain

data class EvaluacionManual(
    private val id: String,
    private val nombreEval: String,
    private val porcentajeEval: Double,
    private val fechaEval: Long,
    private val idCursoEval: String,
    private var notaMaxima: Double = 7.0,
    private var descripcion: String = ""
) : Evaluacion(id, nombreEval, porcentajeEval, fechaEval, idCursoEval) {

    fun getNotaMaxima(): Double = notaMaxima
    fun getDescripcion(): String = descripcion

    fun setNotaMaxima(maxima: Double) {
        if (maxima in 1.0..7.0) notaMaxima = maxima
    }

    override fun calcularNotaPonderada(): Double {
        return (notaObtenida ?: 0.0) * (getPorcentaje() / 100.0)
    }

    override fun calcularPuntosObtenidos(): Double {
        return ((notaObtenida ?: 0.0) / notaMaxima) * getPorcentaje()
    }

    override fun obtenerTipoEvaluacion(): String {
        return "Evaluación Manual"
    }

    override fun obtenerResumen(): String {
        return """
            ${obtenerTipoEvaluacion()}
            Nombre: ${getNombre()}
            Porcentaje: ${getPorcentaje()}%
            Nota: ${notaObtenida ?: "Pendiente"}/$notaMaxima
        """.trimIndent()
    }
}