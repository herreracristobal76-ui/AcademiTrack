package com.academitrack.app.domain

import java.util.*

/**
 * Representa un semestre académico
 * UBICACIÓN: app/src/main/java/com/academitrack/app/domain/Semestre.kt
 */
data class Semestre(
    val id: String,
    val tipo: TipoSemestre,
    val anio: Int,
    val fechaInicio: Long,
    val fechaFin: Long,
    val activo: Boolean = true
) {
    fun obtenerNombre(): String {
        return "${tipo.nombre} $anio"
    }

    fun estaDentroDelRango(fecha: Long): Boolean {
        return fecha >= fechaInicio && fecha <= fechaFin
    }

    companion object {
        fun crearSemestre1(anio: Int): Semestre {
            val fechaInicio = Calendar.getInstance().apply {
                set(anio, Calendar.MARCH, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val fechaFin = Calendar.getInstance().apply {
                set(anio, Calendar.JULY, 31, 23, 59, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            return Semestre(
                id = "sem1_$anio",
                tipo = TipoSemestre.SEMESTRE_1,
                anio = anio,
                fechaInicio = fechaInicio,
                fechaFin = fechaFin,
                activo = true
            )
        }

        fun crearSemestre2(anio: Int): Semestre {
            val fechaInicio = Calendar.getInstance().apply {
                set(anio, Calendar.AUGUST, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val fechaFin = Calendar.getInstance().apply {
                set(anio, Calendar.DECEMBER, 31, 23, 59, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            return Semestre(
                id = "sem2_$anio",
                tipo = TipoSemestre.SEMESTRE_2,
                anio = anio,
                fechaInicio = fechaInicio,
                fechaFin = fechaFin,
                activo = true
            )
        }

        fun obtenerSemestreActual(): Semestre {
            val ahora = Calendar.getInstance()
            val mes = ahora.get(Calendar.MONTH)
            val anio = ahora.get(Calendar.YEAR)

            return if (mes in Calendar.MARCH..Calendar.JULY) {
                crearSemestre1(anio)
            } else {
                crearSemestre2(anio)
            }
        }
    }
}

/**
 * Tipos de semestre
 */
enum class TipoSemestre(val nombre: String, val descripcion: String) {
    SEMESTRE_1("Semestre 1", "Marzo - Julio"),
    SEMESTRE_2("Semestre 2", "Agosto - Diciembre")
}