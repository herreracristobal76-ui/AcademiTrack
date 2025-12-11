package com.academitrack.app.domain

abstract class Evaluacion(
    private val idEvaluacion: String,
    private var nombre: String,
    private var porcentaje: Double,
    private var fecha: Long,
    private var idCurso: String
) : Entidad(idEvaluacion) {

    var notaObtenida: Double? = null
    var estado: EstadoEvaluacion = EstadoEvaluacion.PENDIENTE

    fun getNombre(): String = nombre
    fun getPorcentaje(): Double = porcentaje
    fun getFecha(): Long = fecha
    fun getIdCurso(): String = idCurso

    fun setNombre(nuevoNombre: String) {
        if (nuevoNombre.isNotBlank()) nombre = nuevoNombre
    }

    fun setPorcentaje(nuevoPorcentaje: Double) {
        if (nuevoPorcentaje in 0.0..100.0) porcentaje = nuevoPorcentaje
    }

    fun setFecha(nuevaFecha: Long) {
        if (nuevaFecha > 0) fecha = nuevaFecha
    }

    fun setNotaObtenida(nota: Double) {
        if (nota in 1.0..7.0) {
            notaObtenida = nota
            estado = EstadoEvaluacion.REALIZADA
        }
    }

    abstract fun calcularNotaPonderada(): Double
    abstract fun calcularPuntosObtenidos(): Double
    abstract fun obtenerTipoEvaluacion(): String

    override fun validar(): Boolean {
        return nombre.isNotBlank() &&
                porcentaje in 0.0..100.0 &&
                fecha > 0 &&
                (notaObtenida == null || notaObtenida!! in 1.0..7.0)
    }

    override fun obtenerDescripcion(): String {
        val notaStr = notaObtenida?.toString() ?: "Pendiente"
        return "$nombre - $porcentaje% - Nota: $notaStr"
    }

    fun estaVencida(): Boolean {
        return fecha < System.currentTimeMillis() && estado == EstadoEvaluacion.PENDIENTE
    }
}

enum class EstadoEvaluacion {
    PENDIENTE, REALIZADA, CANCELADA
}