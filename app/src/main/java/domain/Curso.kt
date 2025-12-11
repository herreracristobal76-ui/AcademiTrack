package com.academitrack.app.domain

/**
 * Clase Curso con sistema de estados y semestres
 * UBICACIÓN: app/src/main/java/com/academitrack/app/domain/Curso.kt
 */
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
    private var fechaArchivado: Long? = null
) : Entidad(idCurso) {

    // Getters
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

    // Setters
    fun setNombre(nuevoNombre: String) {
        if (nuevoNombre.isNotBlank()) nombre = nuevoNombre
    }

    fun setCodigo(nuevoCodigo: String) {
        if (nuevoCodigo.isNotBlank()) codigo = nuevoCodigo
    }

    fun setPorcentajeAsistenciaMinimo(porcentaje: Double) {
        if (porcentaje in 0.0..100.0) porcentajeAsistenciaMinimo = porcentaje
    }

    fun setIdSemestre(semestre: String?) {
        idSemestre = semestre
    }

    // Gestión de evaluaciones y asistencias
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

    // Métodos para archivar/reactivar
    fun archivar(nuevoEstado: EstadoCurso, notaFinalCurso: Double? = null) {
        require(nuevoEstado != EstadoCurso.ACTIVO) {
            "No se puede archivar con estado ACTIVO"
        }
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

    // Verificación de estado
    fun estaActivo(): Boolean = estado == EstadoCurso.ACTIVO
    fun estaArchivado(): Boolean = estado != EstadoCurso.ACTIVO

    // Validación
    override fun validar(): Boolean {
        return nombre.isNotBlank() &&
                codigo.isNotBlank() &&
                porcentajeAsistenciaMinimo in 0.0..100.0 &&
                notaMinimaAprobacion in 1.0..7.0
    }

    override fun obtenerDescripcion(): String {
        return "Curso: $nombre ($codigo) - ${estado.descripcion}"
    }

    override fun obtenerResumen(): String {
        return buildString {
            appendLine("Curso: $nombre")
            appendLine("Código: $codigo")
            appendLine("Estado: ${estado.descripcion}")
            appendLine("Evaluaciones: ${evaluaciones.size}")
            appendLine("Asistencia mínima: $porcentajeAsistenciaMinimo%")
            if (notaFinal != null) {
                appendLine("Nota final: ${String.format("%.1f", notaFinal)}")
            }
        }.trimIndent()
    }
}

/**
 * Estados posibles de un curso
 */
enum class EstadoCurso(val descripcion: String) {
    ACTIVO("Cursando actualmente"),
    APROBADO("Curso aprobado"),
    REPROBADO("Curso reprobado"),
    RETIRADO("Retirado del curso")
}