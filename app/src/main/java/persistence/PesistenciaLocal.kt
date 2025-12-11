package com.academitrack.app.persistence

import android.content.Context
import com.academitrack.app.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class PersistenciaLocal(private val context: Context) {

    private val fileName = "academitrack_data.json"

    fun guardarCursos(cursos: List<Curso>): Boolean {
        return try {
            val jsonArray = JSONArray()
            cursos.forEach { curso ->
                val jsonObj = JSONObject().apply {
                    put("id", curso.getId())
                    put("nombre", curso.getNombre())
                    put("codigo", curso.getCodigo())
                    put("asistenciaMinima", curso.getPorcentajeAsistenciaMinimo())
                    put("notaMinima", curso.getNotaMinimaAprobacion())
                }
                jsonArray.put(jsonObj)
            }

            val file = File(context.filesDir, "cursos_$fileName")
            file.writeText(jsonArray.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun cargarCursos(): List<Curso> {
        return try {
            val file = File(context.filesDir, "cursos_$fileName")
            if (!file.exists()) return emptyList()

            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            val cursos = mutableListOf<Curso>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val curso = Curso(
                    idCurso = obj.getString("id"),
                    nombre = obj.getString("nombre"),
                    codigo = obj.getString("codigo"),
                    porcentajeAsistenciaMinimo = obj.getDouble("asistenciaMinima"),
                    notaMinimaAprobacion = obj.getDouble("notaMinima")
                )
                cursos.add(curso)
            }

            cursos
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun exportarReporteCSV(cursoId: String, evaluaciones: List<Evaluacion>): String {
        val csv = StringBuilder()
        csv.appendLine("Nombre,Porcentaje,Nota,Tipo")

        evaluaciones.filter { it.getIdCurso() == cursoId }.forEach { eval ->
            csv.appendLine(
                "${eval.getNombre()},${eval.getPorcentaje()}," +
                        "${eval.notaObtenida ?: "Pendiente"},${eval.obtenerTipoEvaluacion()}"
            )
        }

        return csv.toString()
    }
}