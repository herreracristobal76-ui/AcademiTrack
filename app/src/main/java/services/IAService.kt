package com.academitrack.app.services

import android.util.Base64
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

            val response = llamarClaudeAPI(imagenBase64, prompt)
            parsearRespuestaIA(response)

        } catch (e: Exception) {
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

    private fun llamarClaudeAPI(imagenBase64: String, prompt: String): String {
        val requestBody = JSONObject().apply {
            put("model", "claude-sonnet-4-20250514")
            put("max_tokens", 1024)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "image")
                            put("source", JSONObject().apply {
                                put("type", "base64")
                                put("media_type", "image/jpeg")
                                put("data", imagenBase64)
                            })
                        })
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", prompt)
                        })
                    })
                })
            })
        }.toString()

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("API Error: ${response.code} - ${response.message}")
        }

        return response.body?.string() ?: throw Exception("Respuesta vacía")
    }

    private fun parsearRespuestaIA(response: String): ResultadoIA {
        try {
            val jsonResponse = JSONObject(response)
            val contentArray = jsonResponse.getJSONArray("content")

            var textoRespuesta = ""
            for (i in 0 until contentArray.length()) {
                val item = contentArray.getJSONObject(i)
                if (item.getString("type") == "text") {
                    textoRespuesta = item.getString("text")
                    break
                }
            }

            val jsonLimpio = textoRespuesta
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val datos = JSONObject(jsonLimpio)

            return ResultadoIA(
                exito = true,
                nombreEvaluacion = datos.optString("nombre_evaluacion").takeIf { it.isNotEmpty() },
                nota = datos.optDouble("nota").takeIf { !it.isNaN() },
                porcentaje = datos.optDouble("porcentaje").takeIf { !it.isNaN() },
                fecha = datos.optString("fecha").takeIf { it.isNotEmpty() },
                observaciones = datos.optString("observaciones").takeIf { it.isNotEmpty() },
                confianza = datos.optDouble("confianza", 0.0),
                mensaje = "Datos extraídos correctamente"
            )

        } catch (e: Exception) {
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