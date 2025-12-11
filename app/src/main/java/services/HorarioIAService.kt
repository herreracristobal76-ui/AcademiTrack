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
 * VERSIÓN FINAL - Compatible con Gemini 2.0, 2.5 y 3.0
 * Optimizado para procesar horarios académicos
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
        cursos: List<Curso>
    ): ResultadoHorarioIA = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Procesando horario con Gemini 2.x/3.x")

            val imagenOptimizada = optimizarImagen(imagenBase64)
            Log.d(TAG, "📦 Imagen optimizada: ${imagenOptimizada.length} chars")

            val cursosInfo = if (cursos.isNotEmpty()) {
                "Cursos conocidos:\n" + cursos.joinToString("\n") {
                    "• ${it.getNombre()} (${it.getCodigo()})"
                }
            } else ""

            val prompt = """
                Analiza esta imagen de un horario académico universitario.
                
                ESTRUCTURA TÍPICA:
                • Columnas: Lunes, Martes, Miércoles, Jueves, Viernes
                • Filas: Módulos con horarios (ej: "1: 08:30-09:45")
                • Celdas: Nombre curso, sala, profesor
                
                EXTRAE TODAS LAS CLASES visibles.
                
                $cursosInfo
                
                Para cada clase:
                • nombreCurso: nombre completo
                • sala: número (o "Por definir")
                • profesor: nombre (o "Por definir")
                • dia: 1=Lun, 2=Mar, 3=Mié, 4=Jue, 5=Vie
                • horaInicio: "HH:mm" (ej: "08:30")
                • horaFin: "HH:mm"
                • tipo: "CATEDRA", "LABORATORIO", "AYUDANTIA" o "TALLER"
                
                IMPORTANTE:
                • Un curso en varios días = varias clases separadas
                • Usa horarios EXACTOS de la imagen
                
                Responde SOLO con JSON (sin markdown):
                {
                    "clases": [
                        {
                            "nombreCurso": "Programación",
                            "sala": "A-201",
                            "profesor": "Juan Pérez",
                            "dia": 1,
                            "horaInicio": "08:30",
                            "horaFin": "10:00",
                            "tipo": "CATEDRA"
                        }
                    ]
                }
            """.trimIndent()

            // Modelos compatibles con tu cuenta
            val modelos = listOf(
                "gemini-2.5-flash",
                "gemini-flash-latest",
                "gemini-2.0-flash",
                "gemini-2.5-pro",
                "gemini-pro-latest"
            )

            Log.d(TAG, "📋 Probando ${modelos.size} modelos...")

            for ((index, modelo) in modelos.withIndex()) {
                try {
                    Log.d(TAG, "📡 [${index + 1}/${modelos.size}] $modelo")
                    return@withContext llamarAPIYParsear(modelo, imagenOptimizada, prompt)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Falló: ${e.message}")

                    if (e.message?.contains("403") == true ||
                        e.message?.contains("429") == true) {
                        throw e
                    }
                }
            }

            throw Exception("""
                ❌ No se pudo procesar el horario
                
                VERIFICA:
                
                1️⃣ LA IMAGEN:
                   • ¿Es un horario académico?
                   • ¿Está completo y legible?
                   • ¿Tiene buena iluminación?
                
                2️⃣ TU CUOTA:
                   • Ve a: https://aistudio.google.com/
                   • Verifica solicitudes disponibles
                
                3️⃣ CONEXIÓN:
                   • Verifica tu internet
                   • Intenta en 1 minuto
            """.trimIndent())

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error", e)
            ResultadoHorarioIA(
                exito = false,
                clases = emptyList(),
                confianza = 0.0,
                mensaje = e.message ?: "Error desconocido"
            )
        }
    }

    private fun optimizarImagen(imagenBase64: String): String {
        return try {
            val imageBytes = Base64.decode(imagenBase64, Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(
                imageBytes, 0, imageBytes.size
            )

            val ratio = Math.min(
                MAX_IMAGE_SIZE.toFloat() / bitmap.width,
                MAX_IMAGE_SIZE.toFloat() / bitmap.height
            )

            val resized = if (ratio < 1.0f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt(),
                    (bitmap.height * ratio).toInt(),
                    true
                )
            } else bitmap

            val outputStream = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)

            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo optimizar imagen")
            imagenBase64
        }
    }

    private fun llamarAPIYParsear(
        modelo: String,
        imagenBase64: String,
        prompt: String
    ): ResultadoHorarioIA {
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
            val errorMsg = when (response.code) {
                400 -> "Imagen inválida"
                401 -> "API Key inválida"
                403 -> "Sin permisos"
                404 -> "Modelo no existe"
                429 -> "Límite alcanzado - Espera 1 min"
                else -> "Error ${response.code}"
            }
            throw Exception(errorMsg)
        }

        val responseBody = response.body?.string() ?: throw Exception("Respuesta vacía")
        return parsearRespuestaHorario(responseBody)
    }

    private fun parsearRespuestaHorario(responseBody: String): ResultadoHorarioIA {
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

            Log.d(TAG, "📝 Respuesta: ${textoRespuesta.take(200)}")

            val jsonLimpio = textoRespuesta
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val datos = JSONObject(jsonLimpio)
            val clasesArray = datos.getJSONArray("clases")

            val clases = mutableListOf<ClaseHorario>()

            for (i in 0 until clasesArray.length()) {
                try {
                    val obj = clasesArray.getJSONObject(i)

                    val clase = ClaseHorario(
                        id = "clase_${System.currentTimeMillis()}_$i",
                        idCurso = "",
                        nombreCurso = obj.getString("nombreCurso"),
                        sala = obj.optString("sala", "Por definir"),
                        profesor = obj.optString("profesor", "Por definir"),
                        diaSemana = DiaSemana.fromNumero(obj.getInt("dia")),
                        horaInicio = obj.getString("horaInicio"),
                        horaFin = obj.getString("horaFin"),
                        tipoClase = when (obj.optString("tipo", "CATEDRA").uppercase()) {
                            "LABORATORIO" -> TipoClase.LABORATORIO
                            "AYUDANTIA" -> TipoClase.AYUDANTIA
                            "TALLER" -> TipoClase.TALLER
                            else -> TipoClase.CATEDRA
                        },
                        color = generarColor(obj.getString("nombreCurso"))
                    )

                    clases.add(clase)
                    Log.d(TAG, "✓ ${clase.nombreCurso} ${clase.diaSemana.nombreCorto}")

                } catch (e: Exception) {
                    Log.w(TAG, "Error clase $i", e)
                }
            }

            return ResultadoHorarioIA(
                exito = clases.isNotEmpty(),
                clases = clases,
                confianza = when {
                    clases.size >= 15 -> 90.0
                    clases.size >= 10 -> 85.0
                    clases.size >= 5 -> 75.0
                    else -> 60.0
                },
                mensaje = when {
                    clases.isEmpty() -> "❌ No se detectaron clases"
                    clases.size < 5 -> "⚠️ Solo ${clases.size} clases"
                    else -> "✅ ${clases.size} clases detectadas"
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error parseando", e)
            throw Exception("Error: ${e.message}")
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

data class ResultadoHorarioIA(
    val exito: Boolean,
    val clases: List<ClaseHorario>,
    val confianza: Double,
    val mensaje: String
)