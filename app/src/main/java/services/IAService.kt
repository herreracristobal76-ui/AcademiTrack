package com.academitrack.app.services

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit


class IAService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "IAService"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
    }

    suspend fun procesarImagenNota(imagenBase64: String): ResultadoIA = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Iniciando procesamiento con Gemini 2.x/3.x")

            val prompt = """
                Eres un asistente que analiza imágenes de evaluaciones académicas chilenas.
                
                Analiza esta imagen y extrae:
                1. Nombre de la evaluación (ej: "Certamen 1", "Taller 2")
                2. Nota obtenida (escala chilena 1.0 a 7.0)
                3. Porcentaje que vale del curso (0-100)
                4. Fecha si está visible
                
                IMPORTANTE: Responde SOLO con JSON, sin texto adicional ni markdown:
                {
                    "nombre_evaluacion": "texto",
                    "nota": 5.5,
                    "porcentaje": 30,
                    "fecha": "dd/mm/yyyy",
                    "confianza": 85
                }
                
                Si no encuentras un dato, usa null.
            """.trimIndent()


            val modelos = listOf(
                "gemini-2.5-flash",           // ⭐ Mejor opción
                "gemini-flash-latest",        // ⭐ Siempre actualizado
                "gemini-2.0-flash-001",       // ✅ Alternativa confiable
                "gemini-2.5-pro"              // 💪 Último recurso
            )

            Log.d(TAG, "📋 Probando ${modelos.size} modelos compatibles...")

            for ((index, modelo) in modelos.withIndex()) {
                try {
                    Log.d(TAG, "📡 [${index + 1}/${modelos.size}] Intentando: $modelo")
                    val resultado = llamarGeminiAPI(modelo, imagenBase64, prompt)
                    Log.d(TAG, "✅ ¡ÉXITO! Funcionó con: $modelo")
                    return@withContext resultado
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ [$modelo] falló: ${e.message}")

                    // Si no es 404, es un error más grave
                    if (!e.message.orEmpty().contains("404")) {
                        // Si es 403 o 429, no tiene sentido seguir probando
                        if (e.message?.contains("403") == true ||
                            e.message?.contains("429") == true) {
                            throw e
                        }
                    }
                }
            }


            throw Exception("""
                ❌ No se pudo procesar la imagen
                
                Ninguno de los ${modelos.size} modelos disponibles respondió.
                
                POSIBLES CAUSAS:
                
                1️⃣ Cuota agotada:
                   • Ve a: https://aistudio.google.com/
                   • Verifica tus solicitudes disponibles
                   • El plan gratuito tiene límites
                
                2️⃣ Imagen muy grande:
                   • Intenta con una foto más pequeña
                   • O usa una imagen más clara
                
                3️⃣ Conexión:
                   • Verifica tu WiFi/datos
                   • Intenta en 1 minuto
                
                💡 TIP: Si sigues con problemas, intenta:
                   • Reiniciar la app
                   • Cambiar de red WiFi
                   • Esperar unos minutos
            """.trimIndent())

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fatal", e)
            ResultadoIA(
                exito = false,
                nombreEvaluacion = null,
                nota = null,
                porcentaje = null,
                confianza = 0.0,
                mensaje = e.message ?: "Error desconocido"
            )
        }
    }

    private fun llamarGeminiAPI(
        modelo: String,
        imagenBase64: String,
        prompt: String
    ): ResultadoIA {
        val url = "$BASE_URL/models/$modelo:generateContent?key=$apiKey"

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
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
                put("temperature", 0.2)
                put("topK", 20)
                put("topP", 0.8)
                put("maxOutputTokens", 2048)
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
        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            val errorMsg = when (response.code) {
                400 -> "❌ Imagen inválida (400)\n\nLa imagen podría ser muy grande o corrupta."
                401 -> "❌ API Key inválida (401)\n\nVerifica tu API Key en MainActivity.kt"
                403 -> "❌ Sin permisos (403)\n\nTu API Key no tiene acceso a este modelo."
                404 -> "❌ Modelo no existe (404)\n\nEste modelo fue eliminado o renombrado."
                429 -> "❌ Límite alcanzado (429)\n\nEspera 1 minuto e intenta de nuevo."
                500, 503 -> "❌ Error del servidor (${response.code})\n\nIntenta en unos minutos."
                else -> "❌ Error HTTP ${response.code}"
            }

            Log.e(TAG, "Error: $responseBody")
            throw Exception(errorMsg)
        }

        return parsearRespuesta(responseBody ?: "")
    }

    private fun parsearRespuesta(responseBody: String): ResultadoIA {
        try {
            val jsonResponse = JSONObject(responseBody)

            if (jsonResponse.has("error")) {
                val error = jsonResponse.getJSONObject("error")
                throw Exception("API Error: ${error.optString("message", "Error desconocido")}")
            }

            val candidates = jsonResponse.getJSONArray("candidates")
            if (candidates.length() == 0) {
                throw Exception("La API no generó respuesta.\n\nPosible bloqueo por filtros de seguridad.")
            }

            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val textoRespuesta = parts.getJSONObject(0).getString("text")

            Log.d(TAG, "📝 Respuesta IA (primeros 200 chars): ${textoRespuesta.take(200)}")

            // Limpiar markdown si existe
            val jsonLimpio = textoRespuesta
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val datos = JSONObject(jsonLimpio)

            return ResultadoIA(
                exito = true,
                nombreEvaluacion = datos.optString("nombre_evaluacion").takeIf {
                    it.isNotEmpty() && it != "null"
                },
                nota = datos.optDouble("nota").takeIf { !it.isNaN() },
                porcentaje = datos.optDouble("porcentaje").takeIf { !it.isNaN() },
                fecha = datos.optString("fecha").takeIf {
                    it.isNotEmpty() && it != "null"
                },
                confianza = datos.optDouble("confianza", 75.0),
                mensaje = "✅ Datos extraídos correctamente"
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error parseando respuesta", e)
            throw Exception("❌ Error al interpretar respuesta:\n\n${e.message}")
        }
    }

    fun convertirABase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

data class ResultadoIA(
    val exito: Boolean,
    val nombreEvaluacion: String?,
    val nota: Double?,
    val porcentaje: Double?,
    val fecha: String? = null,
    val observaciones: String? = null,
    val confianza: Double,
    val mensaje: String
)