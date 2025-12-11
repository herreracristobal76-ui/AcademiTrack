package com.academitrack.app.domain

data class Asistencia(
    private val idAsistencia: String,
    private val idCurso: String,
    private var fecha: Long,
    private var estado: EstadoAsistencia,
    private var observacion: String = ""
) : Entidad(idAsistencia) {

    fun getIdCurso(): String = idCurso
    fun getFecha(): Long = fecha
    fun getEstado(): EstadoAsistencia = estado
    fun getObservacion(): String = observacion

    fun setEstado(nuevoEstado: EstadoAsistencia) {
        estado = nuevoEstado
    }

    override fun validar(): Boolean {
        return idCurso.isNotBlank() && fecha > 0
    }

    override fun obtenerDescripcion(): String {
        return "Asistencia - ${estado.descripcion}"
    }

    fun esAsistenciaValida(): Boolean = estado == EstadoAsistencia.PRESENTE
    fun esFalta(): Boolean = estado == EstadoAsistencia.AUSENTE
    fun cuentaParaPorcentaje(): Boolean = estado != EstadoAsistencia.CLASE_CANCELADA

    fun obtenerColorCalendario(): String {
        return when (estado) {
            EstadoAsistencia.PRESENTE -> "#4CAF50"
            EstadoAsistencia.AUSENTE -> "#F44336"
            EstadoAsistencia.CLASE_CANCELADA -> "#FFC107"
            EstadoAsistencia.JUSTIFICADO -> "#2196F3"
        }
    }
}

enum class EstadoAsistencia(val descripcion: String) {
    PRESENTE("Asistió"),
    AUSENTE("Faltó"),
    CLASE_CANCELADA("Clase Cancelada"),
    JUSTIFICADO("Falta Justificada")
}