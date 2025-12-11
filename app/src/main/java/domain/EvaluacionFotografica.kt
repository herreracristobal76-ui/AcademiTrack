package com.academitrack.app.domain

data class EvaluacionFotografica(
    private val id: String,
    private val nombreEval: String,
    private val porcentajeEval: Double,
    private val fechaEval: Long,
    private val idCursoEval: String,
    private var rutaImagen: String,
    private var confianzaIA: Double = 0.0,
    private var datosExtraidosIA: String = ""
) : Evaluacion(id, nombreEval, porcentajeEval, fechaEval, idCursoEval) {

    fun getRutaImagen(): String = rutaImagen
    fun getConfianzaIA(): Double = confianzaIA

    fun setConfianzaIA(confianza: Double) {
        if (confianza in 0.0..100.0) confianzaIA = confianza
    }

    override fun calcularNotaPonderada(): Double {
        return (notaObtenida ?: 0.0) * (getPorcentaje() / 100.0)
    }

    override fun calcularPuntosObtenidos(): Double {
        return ((notaObtenida ?: 0.0) / 7.0) * getPorcentaje()
    }

    override fun obtenerTipoEvaluacion(): String {
        return "Evaluación Fotográfica (IA)"
    }

    override fun obtenerResumen(): String {
        return """
            ${obtenerTipoEvaluacion()}
            Nombre: ${getNombre()}
            Porcentaje: ${getPorcentaje()}%
            Nota: ${notaObtenida ?: "Pendiente"}
            Confianza IA: $confianzaIA%
        """.trimIndent()
    }

    fun requiereVerificacionManual(): Boolean {
        return confianzaIA < 80.0
    }
}