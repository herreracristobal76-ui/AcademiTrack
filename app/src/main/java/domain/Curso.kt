package com.academitrack.app.domain

data class Curso(
    private val idCurso: String,
    private var nombre: String,
    private var codigo: String,
    private var porcentajeAsistenciaMinimo: Double = 75.0,
    private var notaMinimaAprobacion: Double = 4.0,
    private val evaluaciones: MutableList<String> = mutableListOf(),
    private val asistencias: MutableList<String> = mutableListOf()
) : Entidad(idCurso) {

    fun getNombre(): String = nombre
    fun getCodigo(): String = codigo
    fun getPorcentajeAsistenciaMinimo(): Double = porcentajeAsistenciaMinimo
    fun getNotaMinimaAprobacion(): Double = notaMinimaAprobacion
    fun getEvaluaciones(): List<String> = evaluaciones.toList()
    fun getAsistencias(): List<String> = asistencias.toList()

    fun setNombre(nuevoNombre: String) {
        if (nuevoNombre.isNotBlank()) nombre = nuevoNombre
    }

    fun setCodigo(nuevoCodigo: String) {
        if (nuevoCodigo.isNotBlank()) codigo = nuevoCodigo
    }

    fun setPorcentajeAsistenciaMinimo(porcentaje: Double) {
        if (porcentaje in 0.0..100.0) porcentajeAsistenciaMinimo = porcentaje
    }

    fun agregarEvaluacion(idEvaluacion: String) {
        if (!evaluaciones.contains(idEvaluacion)) {
            evaluaciones.add(idEvaluacion)
        }
    }

    fun agregarAsistencia(idAsistencia: String) {
        if (!asistencias.contains(idAsistencia)) {
            asistencias.add(idAsistencia)
        }
    }

    override fun validar(): Boolean {
        return nombre.isNotBlank() &&
                codigo.isNotBlank() &&
                porcentajeAsistenciaMinimo in 0.0..100.0 &&
                notaMinimaAprobacion in 1.0..7.0
    }

    override fun obtenerDescripcion(): String {
        return "Curso: $nombre ($codigo)"
    }

    override fun obtenerResumen(): String {
        return """
            Curso: $nombre
            Código: $codigo
            Evaluaciones: ${evaluaciones.size}
            Asistencia mínima: $porcentajeAsistenciaMinimo%
        """.trimIndent()
    }
}