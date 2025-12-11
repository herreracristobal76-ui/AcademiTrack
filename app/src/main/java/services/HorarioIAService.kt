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
 * Servicio de IA para procesar Horarios (Imágenes y PDFs).
 * ACTUALIZADO: Usa modelo 'gemini-1.5-flash' para evitar errores de permisos.
 */
class HorarioIAService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "HorarioIA"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        // Calidad de compresión para imágenes
        private const val JPEG_QUALITY = 80
    }

    /**
     * Procesa un archivo (Imagen o PDF) para extraer el horario.
     */
    suspend fun procesarArchivoHorario(
        base64Data: String,
        mimeType: String,
        cursosExistentes: List<Curso>,
        semestre: Semestre
    ): ResultadoHorarioConCursos = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Procesando horario ($mimeType)...")

            // Optimizar solo si es imagen, los PDF se envían directo
            val (datosFinales, mimeFinal) = if (mimeType.startsWith("image")) {
                val optimizada = optimizarImagen(base64Data)
                Pair(optimizada, "image/jpeg")
            } else {
                Pair(base64Data, mimeType)
            }

            val cursosInfo = if (cursosExistentes.isNotEmpty()) {
                "Cursos existentes:\n" + cursosExistentes
                    .filter { it.estaActivo() }
                    .joinToString("\n") { "• ${it.getNombre()} (${it.getCodigo()})" }
            } else "No hay cursos registrados"

            val prompt = """
                Analiza este documento (horario académico).
                
                CONTEXTO:
                Semestre: ${semestre.obtenerNombre()}
                $cursosInfo
                
                TU TAREA:
                Identifica cursos, salas, profesores y horarios.
                
                FORMATO JSON REQUERIDO (Sin markdown):
                {
                    "cursos": [
                        {
                            "nombre": "Cálculo I",
                            "codigo": "MAT-101",
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
                
                NOTA: dia 1=Lunes, 2=Martes... 6=Sábado.
            """.trimIndent()

            // MODELO ESTABLE
            val modelo = "gemini-1.5-flash"

            return@withContext llamarAPIYParsearConCursos(modelo, datosFinales, mimeFinal, prompt, cursosExistentes, semestre)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error general", e)
            ResultadoHorarioConCursos(
                exito = false,
                cursosNuevos = emptyList(),
                clases = emptyList(),
                confianza = 0.0,
                mensaje = "Error: ${e.message}"
            )
        }
    }

    private fun llamarAPIYParsearConCursos(
        modelo: String,
        base64Data: String,
        mimeType: String,
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
                                put("mime_type", mimeType)
                                put("data", base64Data)
                            })
                        })
                    })
                })
            })
            // Configuración para respuestas más creativas/precisas
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("maxOutputTokens", 4096)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorMsg = when (response.code) {
                403 -> "Error 403: API Key sin permisos."
                404 -> "Error 404: Modelo no encontrado."
                else -> "Error del servidor (${response.code})"
            }
            throw Exception(errorMsg)
        }

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

            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                throw Exception("Sin respuesta de la IA.")
            }

            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textoRespuesta = parts?.getJSONObject(0)?.optString("text") ?: ""

            val jsonLimpio = textoRespuesta
                .replace("```json", "")
                .replace("```", "")
                .trim()

            if (jsonLimpio.isEmpty()) throw Exception("Respuesta vacía.")

            val datos = JSONObject(jsonLimpio)
            val cursosArray = datos.optJSONArray("cursos") ?: JSONArray()

            val cursosNuevos = mutableListOf<Curso>()
            val todasLasClases = mutableListOf<ClaseHorario>()

            for (i in 0 until cursosArray.length()) {
                val objCurso = cursosArray.getJSONObject(i)
                val nombreCurso = objCurso.optString("nombre", "Curso $i")
                val codigoCurso = objCurso.optString("codigo", "C-$i")

                // Buscar curso existente
                val cursoExistente = cursosExistentes.find {
                    it.estaActivo() && (
                            it.getCodigo().equals(codigoCurso, true) ||
                                    it.getNombre().equals(nombreCurso, true)
                            )
                }

                val idCurso = cursoExistente?.getId() ?: "curso_${System.currentTimeMillis()}_$i"

                if (cursoExistente == null) {
                    val nuevoCurso = Curso(
                        idCurso = idCurso,
                        nombre = nombreCurso,
                        codigo = codigoCurso,
                        idSemestre = semestre.id
                    )
                    cursosNuevos.add(nuevoCurso)
                }

                // Clases
                val clasesArray = objCurso.optJSONArray("clases") ?: JSONArray()
                for (j in 0 until clasesArray.length()) {
                    val objClase = clasesArray.getJSONObject(j)
                    val clase = ClaseHorario(
                        id = "clase_${System.currentTimeMillis()}_${i}_$j",
                        idCurso = idCurso,
                        nombreCurso = nombreCurso,
                        sala = objClase.optString("sala", "S/A"),
                        profesor = objClase.optString("profesor", "S/P"),
                        diaSemana = DiaSemana.fromNumero(objClase.optInt("dia", 1)),
                        horaInicio = objClase.optString("horaInicio", "08:00"),
                        horaFin = objClase.optString("horaFin", "09:30"),
                        tipoClase = TipoClase.valueOf(objClase.optString("tipo", "CATEDRA").uppercase()),
                        color = generarColor(nombreCurso)
                    )
                    todasLasClases.add(clase)
                }
            }

            return ResultadoHorarioConCursos(
                exito = true,
                cursosNuevos = cursosNuevos,
                clases = todasLasClases,
                confianza = 90.0,
                mensaje = "Se detectaron ${todasLasClases.size} clases."
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error parseando", e)
            throw Exception("Error de formato: ${e.message}")
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
        val colores = listOf("#6200EE", "#03DAC6", "#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A")
        return colores[Math.abs(nombre.hashCode()) % colores.size]
    }

    fun convertirABase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
}

// CLASE DE DATOS NECESARIA
data class ResultadoHorarioConCursos(
    val exito: Boolean,
    val cursosNuevos: List<Curso>,
    val clases: List<ClaseHorario>,
    val confianza: Double,
    val mensaje: String
)