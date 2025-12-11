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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "IAService"
    }

    suspend fun procesarImagenNota(imagenBase64: String): ResultadoIA = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Analiza esta imagen que contiene información de una evaluación académica (nota, certamen, taller, examen).
                
                Extrae la siguiente información y devuélvela SOLO en formato JSON sin texto adicional:
                {
                    "nombre_evaluacion": "nombre de la evaluación",
                    "nota": número de 1.0 a 7.0,
                    "porcentaje": porcentaje del total del curso (0-100),
                    "fecha": "fecha en formato dd/mm/yyyy si está visible",
                    "observaciones": "cualquier información relevante",
                    "confianza": tu nivel de confianza en los datos extraídos (0-100)
                }
                
                Si no puedes identificar algún campo, usa null.
                IMPORTANTE: Responde SOLO con el JSON, sin markdown ni texto adicional.
            """.trimIndent()

            val response = llamarGeminiAPI(imagenBase64, prompt)
            parsearRespuestaIA(response)

        } catch (e: Exception) {
            Log.e(TAG, "Error procesando imagen", e)
            e.printStackTrace()
            ResultadoIA(
                exito = false,
                nombreEvaluacion = null,
                nota = null,
                porcentaje = null,
                confianza = 0.0,
                mensaje = "Error: ${e.message}"
            )
        }
    }

    private fun llamarGeminiAPI(imagenBase64: String, prompt: String): String {
        // Solo usar gemini-1.5-flash que es el modelo que funciona
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        Log.d(TAG, "Usando modelo: gemini-1.5-flash")
        return intentarLlamadaAPI(url, imagenBase64, prompt)
    }

    private fun intentarLlamadaAPI(url: String, imagenBase64: String, prompt: String): String {
        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        // Parte de texto
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        // Parte de imagen
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
                put("temperature", 0.4)
                put("topK", 32)
                put("topP", 1)
                put("maxOutputTokens", 2048)
            })
            put("safetySettings", JSONArray().apply {
                put(JSONObject().apply {
                    put("category", "HARM_CATEGORY_HARASSMENT")
                    put("threshold", "BLOCK_NONE")
                })
                put(JSONObject().apply {
                    put("category", "HARM_CATEGORY_HATE_SPEECH")
                    put("threshold", "BLOCK_NONE")
                })
                put(JSONObject().apply {
                    put("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT")
                    put("threshold", "BLOCK_NONE")
                })
                put(JSONObject().apply {
                    put("category", "HARM_CATEGORY_DANGEROUS_CONTENT")
                    put("threshold", "BLOCK_NONE")
                })
            })
        }.toString()

        Log.d(TAG, "Request body size: ${requestBody.length} characters")

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        Log.d(TAG, "Response code: ${response.code}")
        if (responseBody != null) {
            Log.d(TAG, "Response body: ${responseBody.take(500)}")
        }

        if (!response.isSuccessful) {
            throw Exception("API Error: ${response.code} - ${response.message}\nBody: $responseBody")
        }

        return responseBody ?: throw Exception("Respuesta vacía")
    }

    private fun parsearRespuestaIA(response: String): ResultadoIA {
        try {
            val jsonResponse = JSONObject(response)

            // Verificar si hay error en la respuesta
            if (jsonResponse.has("error")) {
                val error = jsonResponse.getJSONObject("error")
                throw Exception("API Error: ${error.getString("message")}")
            }

            // Gemini devuelve la respuesta en candidates[0].content.parts[0].text
            val candidates = jsonResponse.getJSONArray("candidates")
            if (candidates.length() == 0) {
                throw Exception("No se recibieron candidatos en la respuesta")
            }

            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val textoRespuesta = parts.getJSONObject(0).getString("text")

            Log.d(TAG, "Texto respuesta IA: $textoRespuesta")

            // Limpiar el texto de markdown si existe
            val jsonLimpio = textoRespuesta
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val datos = JSONObject(jsonLimpio)

            return ResultadoIA(
                exito = true,
                nombreEvaluacion = datos.optString("nombre_evaluacion").takeIf { it.isNotEmpty() && it != "null" },
                nota = datos.optDouble("nota").takeIf { !it.isNaN() },
                porcentaje = datos.optDouble("porcentaje").takeIf { !it.isNaN() },
                fecha = datos.optString("fecha").takeIf { it.isNotEmpty() && it != "null" },
                observaciones = datos.optString("observaciones").takeIf { it.isNotEmpty() && it != "null" },
                confianza = datos.optDouble("confianza", 80.0),
                mensaje = "Datos extraídos correctamente"
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error parseando respuesta", e)
            e.printStackTrace()
            return ResultadoIA(
                exito = false,
                nombreEvaluacion = null,
                nota = null,
                porcentaje = null,
                confianza = 0.0,
                mensaje = "Error al parsear respuesta: ${e.message}"
            )
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