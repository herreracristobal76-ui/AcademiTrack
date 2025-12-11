package com.academitrack.app.domain

data class Curso(
    private val idCurso: String,
    private var nombre: String,
    private var codigo: String,
    private var porcentajeAsistenciaMinimo: Double = 75.0,
    private var notaMinimaAprobacion: Double = 4.0,
    private val evaluaciones: MutableList<String> = mutableListOf(),
    private val asistencias: MutableList<String> = mutableListOf(),
    private var estado: EstadoCurso = EstadoCurso.ACTIVO,
    private var idSemestre: String? = null,
    private var notaFinal: Double? = null,
    private var fechaArchivado: Long? = null,
    private var color: String = "#4F46E5"
) : Entidad(idCurso) {

    fun getNombre(): String = nombre
    fun getCodigo(): String = codigo
    fun getPorcentajeAsistenciaMinimo(): Double = porcentajeAsistenciaMinimo
    fun getNotaMinimaAprobacion(): Double = notaMinimaAprobacion
    fun getEvaluaciones(): List<String> = evaluaciones.toList()
    fun getAsistencias(): List<String> = asistencias.toList()
    fun getEstado(): EstadoCurso = estado
    fun getIdSemestre(): String? = idSemestre
    fun getNotaFinal(): Double? = notaFinal
    fun getFechaArchivado(): Long? = fechaArchivado
    fun getColor(): String = color

    fun setNombre(nuevoNombre: String) { if (nuevoNombre.isNotBlank()) nombre = nuevoNombre }
    fun setCodigo(nuevoCodigo: String) { if (nuevoCodigo.isNotBlank()) codigo = nuevoCodigo }
    fun setPorcentajeAsistenciaMinimo(porcentaje: Double) { if (porcentaje in 0.0..100.0) porcentajeAsistenciaMinimo = porcentaje }
    fun setIdSemestre(semestre: String?) { idSemestre = semestre }
    fun setColor(nuevoColor: String) { if (nuevoColor.isNotBlank()) color = nuevoColor }

    fun agregarEvaluacion(idEvaluacion: String) {
        if (!evaluaciones.contains(idEvaluacion)) evaluaciones.add(idEvaluacion)
    }

    // ESTA ES LA FUNCIÓN QUE PROBABLEMENTE TE FALTABA
    fun eliminarEvaluacion(idEvaluacion: String) {
        evaluaciones.remove(idEvaluacion)
    }

    fun agregarAsistencia(idAsistencia: String) {
        if (!asistencias.contains(idAsistencia)) asistencias.add(idAsistencia)
    }

    fun archivar(nuevoEstado: EstadoCurso, notaFinalCurso: Double? = null) {
        require(nuevoEstado != EstadoCurso.ACTIVO) { "No se puede archivar con estado ACTIVO" }
        estado = nuevoEstado
        notaFinal = notaFinalCurso
        fechaArchivado = System.currentTimeMillis()
    }

    fun reactivar(nuevoSemestre: String? = null) {
        estado = EstadoCurso.ACTIVO
        idSemestre = nuevoSemestre
        notaFinal = null
        fechaArchivado = null
    }

    fun estaActivo(): Boolean = estado == EstadoCurso.ACTIVO
    fun estaArchivado(): Boolean = estado != EstadoCurso.ACTIVO

    override fun validar(): Boolean = nombre.isNotBlank() && codigo.isNotBlank() && porcentajeAsistenciaMinimo in 0.0..100.0 && notaMinimaAprobacion in 1.0..7.0
    override fun obtenerDescripcion(): String = "Curso: $nombre ($codigo) - ${estado.descripcion}"
    override fun obtenerResumen(): String = "Curso: $nombre\nCódigo: $codigo"
}

enum class EstadoCurso(val descripcion: String) {
    ACTIVO("Cursando actualmente"), APROBADO("Curso aprobado"), REPROBADO("Curso reprobado"), RETIRADO("Retirado del curso")
}