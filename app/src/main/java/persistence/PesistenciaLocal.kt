package com.academitrack.app.persistence

import android.content.Context
import android.content.SharedPreferences
import com.academitrack.app.domain.*
import com.academitrack.app.ui.NotificacionConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class PersistenciaLocal(private val context: Context) {

    private val fileName = "academitrack_data.json"
    private val prefs: SharedPreferences =
        context.getSharedPreferences("academitrack_prefs", Context.MODE_PRIVATE)

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
                    put("evaluaciones", JSONArray(curso.getEvaluaciones()))
                    put("asistencias", JSONArray(curso.getAsistencias()))
                    put("estado", curso.getEstado().name)
                    put("idSemestre", curso.getIdSemestre() ?: "")
                    put("notaFinal", curso.getNotaFinal() ?: 0.0)
                    put("fechaArchivado", curso.getFechaArchivado() ?: 0L)
                    put("color", curso.getColor())
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

                val evaluaciones = mutableListOf<String>()
                val evalArray = obj.optJSONArray("evaluaciones")
                if (evalArray != null) {
                    for (j in 0 until evalArray.length()) {
                        evaluaciones.add(evalArray.getString(j))
                    }
                }

                val asistencias = mutableListOf<String>()
                val asistArray = obj.optJSONArray("asistencias")
                if (asistArray != null) {
                    for (j in 0 until asistArray.length()) {
                        asistencias.add(asistArray.getString(j))
                    }
                }

                val estado = try {
                    EstadoCurso.valueOf(obj.optString("estado", "ACTIVO"))
                } catch (e: Exception) {
                    EstadoCurso.ACTIVO
                }

                val idSemestre = obj.optString("idSemestre", "").takeIf { it.isNotEmpty() }
                val notaFinal = obj.optDouble("notaFinal").takeIf { it != 0.0 }
                val fechaArchivado = obj.optLong("fechaArchivado").takeIf { it != 0L }
                val color = obj.optString("color", "#4F46E5")

                val curso = Curso(
                    idCurso = obj.getString("id"),
                    nombre = obj.getString("nombre"),
                    codigo = obj.getString("codigo"),
                    porcentajeAsistenciaMinimo = obj.getDouble("asistenciaMinima"),
                    notaMinimaAprobacion = obj.getDouble("notaMinima"),
                    evaluaciones = evaluaciones,
                    asistencias = asistencias,
                    estado = estado,
                    idSemestre = idSemestre,
                    notaFinal = notaFinal,
                    fechaArchivado = fechaArchivado,
                    color = color
                )
                cursos.add(curso)
            }

            cursos
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }


    fun guardarHorarios(clases: List<ClaseHorario>): Boolean {
        return try {
            val jsonArray = JSONArray()
            clases.forEach { clase ->
                val jsonObj = JSONObject().apply {
                    put("id", clase.id)
                    put("idCurso", clase.idCurso)
                    put("nombreCurso", clase.nombreCurso)
                    put("sala", clase.sala)
                    put("profesor", clase.profesor)
                    put("diaSemana", clase.diaSemana.numero)
                    put("horaInicio", clase.horaInicio)
                    put("horaFin", clase.horaFin)
                    put("tipoClase", clase.tipoClase.name)
                    put("color", clase.color)
                }
                jsonArray.put(jsonObj)
            }
            val file = File(context.filesDir, "horarios_$fileName")
            file.writeText(jsonArray.toString())
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    fun cargarHorarios(): List<ClaseHorario> {
        return try {
            val file = File(context.filesDir, "horarios_$fileName")
            if (!file.exists()) return emptyList()
            val jsonArray = JSONArray(file.readText())
            val clases = mutableListOf<ClaseHorario>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val clase = ClaseHorario(
                    id = obj.getString("id"),
                    idCurso = obj.getString("idCurso"),
                    nombreCurso = obj.getString("nombreCurso"),
                    sala = obj.getString("sala"),
                    profesor = obj.getString("profesor"),
                    diaSemana = DiaSemana.fromNumero(obj.getInt("diaSemana")),
                    horaInicio = obj.getString("horaInicio"),
                    horaFin = obj.getString("horaFin"),
                    tipoClase = TipoClase.valueOf(obj.getString("tipoClase")),
                    color = obj.getString("color")
                )
                clases.add(clase)
            }
            clases
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    fun guardarEvaluaciones(evaluaciones: Map<String, Evaluacion>): Boolean {
        return try {
            val jsonArray = JSONArray()
            evaluaciones.values.forEach { eval ->
                val jsonObj = JSONObject().apply {
                    put("id", eval.getId())
                    put("nombre", eval.getNombre())
                    put("porcentaje", eval.getPorcentaje())
                    put("fecha", eval.getFecha())
                    put("idCurso", eval.getIdCurso())
                    put("notaObtenida", eval.notaObtenida)
                    put("estado", eval.estado.name)
                    when (eval) {
                        is EvaluacionManual -> {
                            put("tipo", "manual")
                            put("notaMaxima", eval.getNotaMaxima())
                            put("descripcion", eval.getDescripcion())
                        }
                        is EvaluacionFotografica -> {
                            put("tipo", "fotografica")
                            put("rutaImagen", eval.getRutaImagen())
                            put("confianzaIA", eval.getConfianzaIA())
                        }
                    }
                }
                jsonArray.put(jsonObj)
            }
            val file = File(context.filesDir, "evaluaciones_$fileName")
            file.writeText(jsonArray.toString())
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    fun cargarEvaluaciones(): Map<String, Evaluacion> {
        return try {
            val file = File(context.filesDir, "evaluaciones_$fileName")
            if (!file.exists()) return emptyMap()
            val jsonArray = JSONArray(file.readText())
            val evaluaciones = mutableMapOf<String, Evaluacion>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val tipo = obj.getString("tipo")
                val eval: Evaluacion = when (tipo) {
                    "manual" -> EvaluacionManual(
                        id = obj.getString("id"),
                        nombreEval = obj.getString("nombre"),
                        porcentajeEval = obj.getDouble("porcentaje"),
                        fechaEval = obj.getLong("fecha"),
                        idCursoEval = obj.getString("idCurso"),
                        notaMaxima = obj.optDouble("notaMaxima", 7.0),
                        descripcion = obj.optString("descripcion", "")
                    )
                    "fotografica" -> EvaluacionFotografica(
                        id = obj.getString("id"),
                        nombreEval = obj.getString("nombre"),
                        porcentajeEval = obj.getDouble("porcentaje"),
                        fechaEval = obj.getLong("fecha"),
                        idCursoEval = obj.getString("idCurso"),
                        rutaImagen = obj.optString("rutaImagen", ""),
                        confianzaIA = obj.optDouble("confianzaIA", 0.0)
                    )
                    else -> continue
                }
                if (!obj.isNull("notaObtenida")) {
                    eval.setNotaObtenida(obj.getDouble("notaObtenida"))
                }
                eval.estado = EstadoEvaluacion.valueOf(obj.getString("estado"))
                evaluaciones[eval.getId()] = eval
            }
            evaluaciones
        } catch (e: Exception) { e.printStackTrace(); emptyMap() }
    }

    fun guardarAsistencias(asistencias: Map<String, Asistencia>): Boolean {
        return try {
            val jsonArray = JSONArray()
            asistencias.values.forEach { asist ->
                val jsonObj = JSONObject().apply {
                    put("id", asist.getId())
                    put("idCurso", asist.getIdCurso())
                    put("fecha", asist.getFecha())
                    put("estado", asist.getEstado().name)
                    put("observacion", asist.getObservacion())
                }
                jsonArray.put(jsonObj)
            }
            val file = File(context.filesDir, "asistencias_$fileName")
            file.writeText(jsonArray.toString())
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    fun cargarAsistencias(): Map<String, Asistencia> {
        return try {
            val file = File(context.filesDir, "asistencias_$fileName")
            if (!file.exists()) return emptyMap()
            val jsonArray = JSONArray(file.readText())
            val asistencias = mutableMapOf<String, Asistencia>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val asist = Asistencia(
                    idAsistencia = obj.getString("id"),
                    idCurso = obj.getString("idCurso"),
                    fecha = obj.getLong("fecha"),
                    estado = EstadoAsistencia.valueOf(obj.getString("estado")),
                    observacion = obj.optString("observacion", "")
                )
                asistencias[asist.getId()] = asist
            }
            asistencias
        } catch (e: Exception) { e.printStackTrace(); emptyMap() }
    }

    fun guardarConfigNotificacion(cursoId: String, config: NotificacionConfig) {
        prefs.edit().apply {
            putBoolean("notif_activo_$cursoId", config.activo)
            putInt("notif_hora_$cursoId", config.hora)
            putInt("notif_minuto_$cursoId", config.minuto)
            apply()
        }
    }

    fun cargarConfigNotificacion(cursoId: String): NotificacionConfig {
        return NotificacionConfig(
            activo = prefs.getBoolean("notif_activo_$cursoId", false),
            hora = prefs.getInt("notif_hora_$cursoId", 20),
            minuto = prefs.getInt("notif_minuto_$cursoId", 0)
        )
    }

    fun limpiarDatos() {
        try {
            File(context.filesDir, "cursos_$fileName").delete()
            File(context.filesDir, "evaluaciones_$fileName").delete()
            File(context.filesDir, "asistencias_$fileName").delete()
            File(context.filesDir, "horarios_$fileName").delete()
            prefs.edit().clear().apply()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun exportarDatosGlobales(): String {
        return try {
            val root = JSONObject()
            fun leerArchivo(prefijo: String): JSONArray {
                val file = File(context.filesDir, "${prefijo}_$fileName")
                return if (file.exists()) JSONArray(file.readText()) else JSONArray()
            }
            root.put("version", 1)
            root.put("timestamp", System.currentTimeMillis())
            root.put("cursos", leerArchivo("cursos"))
            root.put("horarios", leerArchivo("horarios"))
            root.put("evaluaciones", leerArchivo("evaluaciones"))
            root.put("asistencias", leerArchivo("asistencias"))
            root.toString()
        } catch (e: Exception) { e.printStackTrace(); "" }
    }

    fun importarDatosGlobales(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            fun guardarArchivo(prefijo: String, key: String) {
                val data = root.optJSONArray(key) ?: JSONArray()
                val file = File(context.filesDir, "${prefijo}_$fileName")
                file.writeText(data.toString())
            }
            guardarArchivo("cursos", "cursos")
            guardarArchivo("horarios", "horarios")
            guardarArchivo("evaluaciones", "evaluaciones")
            guardarArchivo("asistencias", "asistencias")
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    fun guardarPreferenciaModoOscuro(modoOscuro: Boolean) {
        prefs.edit().putBoolean("modo_oscuro", modoOscuro).apply()
    }

    fun cargarPreferenciaModoOscuro(): Boolean {
        return prefs.getBoolean("modo_oscuro", false)
    }
}