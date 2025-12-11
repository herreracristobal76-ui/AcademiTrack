package com.academitrack.app.services

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.academitrack.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * VERSIÓN ACTUALIZADA - Con creación automática de cursos
 */
class HorarioIAService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "HorarioIA"
        private const val MAX_IMAGE_SIZE = 1024
        private const val JPEG_QUALITY = 75
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
    }

    suspend fun procesarImagenHorario(
        imagenBase64: String,
        cursosExistentes: List<Curso>,
        semestre: Semestre
    ): ResultadoHorarioConCursos = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Procesando horario para ${semestre.obtenerNombre()}")

            val imagenOptimizada = optimizarImagen(imagenBase64)

            val cursosInfo = if (cursosExistentes.isNotEmpty()) {
                "Cursos existentes activos:\n" + cursosExistentes
                    .filter { it.estaActivo() }
                    .joinToString("\n") { "• ${it.getNombre()} (${it.getCodigo()})" }
            } else "No hay cursos registrados aún"

            val prompt = """
                Analiza esta imagen de un horario académico universitario.
                
                CONTEXTO:
                Semestre: ${semestre.obtenerNombre()}
                Período: ${semestre.tipo.descripcion}
                
                $cursosInfo
                
                ESTRUCTURA:
                • Columnas: Días de la semana
                • Filas: Módulos con horarios
                • Celdas: Nombre curso, sala, profesor
                
                INSTRUCCIONES:
                1. Identifica TODOS los cursos únicos del horario
                2. Para cada curso, extrae su código si está visible
                3. Genera una lista de cursos y sus clases
                
                FORMATO DE RESPUESTA (JSON sin markdown):
                {
                    "cursos": [
                        {
                            "nombre": "Programación Orientada a Objetos",
                            "codigo": "INF-2241",
                            "clases": [
                                {
                                    "sala": "A-201",
                                    "profesor": "Juan Pérez",
                                    "dia": 1,
                                    "horaInicio": "08:30",
                                    "horaFin": "10:00",
                                    "tipo": "CATEDRA"
                                }
                            ]
                        }
                    ]
                }
                
                IMPORTANTE:
                • Agrupa todas las clases por curso
                • Si no ves el código, genera uno (ej: "CURSO-001")
                • dia: 1=Lun, 2=Mar, 3=Mié, 4=Jue, 5=Vie
                • tipo: CATEDRA, LABORATORIO, AYUDANTIA, TALLER
            """.trimIndent()

            val modelos = listOf(
                "gemini-2.5-flash",
                "gemini-flash-latest",
                "gemini-2.0-flash"
            )

            for ((index, modelo) in modelos.withIndex()) {
                try {
                    Log.d(TAG, "📡 [${index + 1}/${modelos.size}] $modelo")
                    return@withContext llamarAPIYParsearConCursos(modelo, imagenOptimizada, prompt, cursosExistentes, semestre)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Falló: ${e.message}")
                    if (e.message?.contains("403") == true || e.message?.contains("429") == true) {
                        throw e
                    }
                }
            }

            throw Exception("No se pudo procesar el horario")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error", e)
            ResultadoHorarioConCursos(
                exito = false,
                cursosNuevos = emptyList(),
                clases = emptyList(),
                confianza = 0.0,
                mensaje = e.message ?: "Error desconocido"
            )
        }
    }

    private fun llamarAPIYParsearConCursos(
        modelo: String,
        imagenBase64: String,
        prompt: String,
        cursosExistentes: List<Curso>,
        semestre: Semestre
    ): ResultadoHorarioConCursos {
        val url = "$BASE_URL/models/$modelo:generateContent?key=$apiKey"

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", imagenBase64)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("topK", 10)
                put("topP", 0.5)
                put("maxOutputTokens", 4096)
            })
            put("safetySettings", JSONArray().apply {
                listOf(
                    "HARM_CATEGORY_HARASSMENT",
                    "HARM_CATEGORY_HATE_SPEECH",
                    "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                    "HARM_CATEGORY_DANGEROUS_CONTENT"
                ).forEach { category ->
                    put(JSONObject().apply {
                        put("category", category)
                        put("threshold", "BLOCK_NONE")
                    })
                }
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Error ${response.code}")
        }

        val responseBody = response.body?.string() ?: throw Exception("Respuesta vacía")
        return parsearRespuestaConCursos(responseBody, cursosExistentes, semestre)
    }

    private fun parsearRespuestaConCursos(
        responseBody: String,
        cursosExistentes: List<Curso>,
        semestre: Semestre
    ): ResultadoHorarioConCursos {
        try {
            val jsonResponse = JSONObject(responseBody)

            if (jsonResponse.has("error")) {
                throw Exception(jsonResponse.getJSONObject("error").getString("message"))
            }

            val candidates = jsonResponse.getJSONArray("candidates")
            if (candidates.length() == 0) {
                throw Exception("Sin respuesta generada")
            }

            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val textoRespuesta = parts.getJSONObject(0).getString("text")

            val jsonLimpio = textoRespuesta
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val datos = JSONObject(jsonLimpio)
            val cursosArray = datos.getJSONArray("cursos")

            val cursosNuevos = mutableListOf<Curso>()
            val todasLasClases = mutableListOf<ClaseHorario>()

            for (i in 0 until cursosArray.length()) {
                try {
                    val objCurso = cursosArray.getJSONObject(i)
                    val nombreCurso = objCurso.getString("nombre")
                    val codigoCurso = objCurso.optString("codigo", "CURSO-${System.currentTimeMillis() / 1000}")

                    // Verificar si el curso ya existe
                    val cursoExistente = cursosExistentes.find {
                        it.estaActivo() && (
                                it.getCodigo().equals(codigoCurso, ignoreCase = true) ||
                                        it.getNombre().equals(nombreCurso, ignoreCase = true)
                                )
                    }

                    val idCurso = cursoExistente?.getId() ?: "curso_${System.currentTimeMillis()}_$i"

                    // Si no existe, crear nuevo curso
                    if (cursoExistente == null) {
                        val nuevoCurso = Curso(
                            idCurso = idCurso,
                            nombre = nombreCurso,
                            codigo = codigoCurso,
                            porcentajeAsistenciaMinimo = 75.0,
                            notaMinimaAprobacion = 4.0,
                            idSemestre = semestre.id
                        )
                        cursosNuevos.add(nuevoCurso)
                        Log.d(TAG, "✨ Curso nuevo: $nombreCurso ($codigoCurso)")
                    } else {
                        Log.d(TAG, "♻️ Curso existente: $nombreCurso")
                    }

                    // Procesar clases del curso
                    val clasesArray = objCurso.getJSONArray("clases")
                    for (j in 0 until clasesArray.length()) {
                        val objClase = clasesArray.getJSONObject(j)

                        val clase = ClaseHorario(
                            id = "clase_${System.currentTimeMillis()}_${i}_$j",
                            idCurso = idCurso,
                            nombreCurso = nombreCurso,
                            sala = objClase.optString("sala", "Por definir"),
                            profesor = objClase.optString("profesor", "Por definir"),
                            diaSemana = DiaSemana.fromNumero(objClase.getInt("dia")),
                            horaInicio = objClase.getString("horaInicio"),
                            horaFin = objClase.getString("horaFin"),
                            tipoClase = when (objClase.optString("tipo", "CATEDRA").uppercase()) {
                                "LABORATORIO" -> TipoClase.LABORATORIO
                                "AYUDANTIA" -> TipoClase.AYUDANTIA
                                "TALLER" -> TipoClase.TALLER
                                else -> TipoClase.CATEDRA
                            },
                            color = generarColor(nombreCurso)
                        )

                        todasLasClases.add(clase)
                    }

                } catch (e: Exception) {
                    Log.w(TAG, "Error procesando curso $i", e)
                }
            }

            return ResultadoHorarioConCursos(
                exito = todasLasClases.isNotEmpty(),
                cursosNuevos = cursosNuevos,
                clases = todasLasClases,
                confianza = when {
                    todasLasClases.size >= 15 -> 90.0
                    todasLasClases.size >= 10 -> 85.0
                    todasLasClases.size >= 5 -> 75.0
                    else -> 60.0
                },
                mensaje = when {
                    cursosNuevos.isEmpty() -> "✅ ${todasLasClases.size} clases detectadas"
                    else -> "✅ ${cursosNuevos.size} cursos nuevos, ${todasLasClases.size} clases"
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error parseando", e)
            throw Exception("Error: ${e.message}")
        }
    }

    private fun optimizarImagen(imagenBase64: String): String {
        return try {
            val imageBytes = Base64.decode(imagenBase64, Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            if (bitmap == null) return imagenBase64 // Si no es imagen (ej. PDF corrupto detectado como img), devolver original

            val maxDimension = 1024
            if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                val ratio = Math.min(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
                val resized = Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
                val stream = ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
                return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            }
            imagenBase64
        } catch (e: Exception) {
            imagenBase64
        }
    }

    private fun generarColor(nombre: String): String {
        val colores = listOf(
            "#6200EE", "#03DAC6", "#FF6B6B", "#4ECDC4",
            "#45B7D1", "#FFA07A", "#98D8C8", "#F7DC6F",
            "#BB8FCE", "#85C1E2", "#F8B739", "#52B788"
        )
        return colores[Math.abs(nombre.hashCode()) % colores.size]
    }

    fun convertirABase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

data class ResultadoHorarioConCursos(
    val exito: Boolean,
    val cursosNuevos: List<Curso>,
    val clases: List<ClaseHorario>,
    val confianza: Double,
    val mensaje: String
)