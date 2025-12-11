package com.academitrack.app.domain

import java.util.*

/**
 * Representa una clase en el horario semanal
 *
 * UBICACIÓN: app/src/main/java/com/academitrack/app/domain/Horario.kt
 */
data class ClaseHorario(
    val id: String,
    val idCurso: String,
    val nombreCurso: String,
    val sala: String,
    val profesor: String,
    val diaSemana: DiaSemana,
    val horaInicio: String, // Formato "HH:mm"
    val horaFin: String,    // Formato "HH:mm"
    val tipoClase: TipoClase = TipoClase.CATEDRA,
    val color: String = "#6200EE"
) {
    fun obtenerHorarioCompleto(): String {
        return "$horaInicio - $horaFin"
    }

    fun estaDentroDeHorario(hora: Int, minuto: Int): Boolean {
        val horaActual = String.format("%02d:%02d", hora, minuto)
        return horaActual >= horaInicio && horaActual <= horaFin
    }

    fun minutosAntes(minutos: Int): Pair<Int, Int> {
        val partes = horaInicio.split(":")
        val hora = partes[0].toInt()
        val minuto = partes[1].toInt()

        val totalMinutos = hora * 60 + minuto - minutos
        return Pair(totalMinutos / 60, totalMinutos % 60)
    }
}

enum class DiaSemana(val numero: Int, val nombre: String, val nombreCorto: String) {
    LUNES(1, "Lunes", "Lun"),
    MARTES(2, "Martes", "Mar"),
    MIERCOLES(3, "Miércoles", "Mié"),
    JUEVES(4, "Jueves", "Jue"),
    VIERNES(5, "Viernes", "Vie"),
    SABADO(6, "Sábado", "Sáb"),
    DOMINGO(7, "Domingo", "Dom");

    companion object {
        fun fromCalendar(calendarDay: Int): DiaSemana {
            return when (calendarDay) {
                Calendar.MONDAY -> LUNES
                Calendar.TUESDAY -> MARTES
                Calendar.WEDNESDAY -> MIERCOLES
                Calendar.THURSDAY -> JUEVES
                Calendar.FRIDAY -> VIERNES
                Calendar.SATURDAY -> SABADO
                Calendar.SUNDAY -> DOMINGO
                else -> LUNES
            }
        }

        fun fromNumero(numero: Int): DiaSemana {
            return values().find { it.numero == numero } ?: LUNES
        }
    }
}

enum class TipoClase(val descripcion: String) {
    CATEDRA("Cátedra"),
    LABORATORIO("Laboratorio"),
    AYUDANTIA("Ayudantía"),
    TALLER("Taller")
}

/**
 * Gestor de horarios
 */
class GestorHorario {
    private val clases = mutableMapOf<String, ClaseHorario>()

    fun agregarClase(clase: ClaseHorario) {
        clases[clase.id] = clase
    }

    fun eliminarClase(idClase: String) {
        clases.remove(idClase)
    }

    fun obtenerClasesPorDia(dia: DiaSemana): List<ClaseHorario> {
        return clases.values.filter { it.diaSemana == dia }.sortedBy { it.horaInicio }
    }

    fun obtenerClasesPorCurso(idCurso: String): List<ClaseHorario> {
        return clases.values.filter { it.idCurso == idCurso }
    }

    fun obtenerTodasClases(): List<ClaseHorario> {
        return clases.values.toList()
    }

    fun obtenerClasesHoy(): List<ClaseHorario> {
        val hoy = DiaSemana.fromCalendar(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        return obtenerClasesPorDia(hoy)
    }

    fun obtenerProximaClase(): ClaseHorario? {
        val ahora = Calendar.getInstance()
        val horaActual = String.format("%02d:%02d", ahora.get(Calendar.HOUR_OF_DAY), ahora.get(Calendar.MINUTE))
        val diaActual = DiaSemana.fromCalendar(ahora.get(Calendar.DAY_OF_WEEK))

        // Buscar clases hoy que aún no han comenzado
        val clasesHoy = obtenerClasesPorDia(diaActual).filter { it.horaInicio > horaActual }
        if (clasesHoy.isNotEmpty()) {
            return clasesHoy.first()
        }

        // Buscar la primera clase del siguiente día
        val diasSiguientes = DiaSemana.values().filter { it.numero > diaActual.numero }
        for (dia in diasSiguientes) {
            val clasesDia = obtenerClasesPorDia(dia)
            if (clasesDia.isNotEmpty()) {
                return clasesDia.first()
            }
        }

        // Si no hay más clases esta semana, buscar el lunes
        return obtenerClasesPorDia(DiaSemana.LUNES).firstOrNull()
    }

    fun cargarClases(clasesGuardadas: List<ClaseHorario>) {
        clases.clear()
        clasesGuardadas.forEach { clases[it.id] = it }
    }
}