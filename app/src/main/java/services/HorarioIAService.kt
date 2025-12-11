package com.academitrack.app.services

import android.util.Base64
import com.academitrack.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Servicio para procesar imágenes de horarios con IA
 *
 * UBICACIÓN: app/src/main/java/com/academitrack/app/services/HorarioIAService.kt
 */
class HorarioIAService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun procesarImagenHorario(
        imagenBase64: String,
        cursos: List<Curso>
    ): ResultadoHorarioIA = withContext(Dispatchers.IO) {
        try {
            val cursosInfo = cursos.joinToString("\n") {
                "- ${it.getNombre()} (${it.getCodigo()})"
            }

            val prompt = """
                Analiza esta imagen que contiene un horario académico semanal.
                
                Cursos del estudiante:
                $cursosInfo
                
                Extrae TODAS las clases y devuelve un JSON con este formato EXACTO:
                {
                    "clases": [
                        {
                            "nombreCurso": "nombre exacto del curso",
                            "codigo": "código del curso si está visible",
                            "sala": "sala o aula",
                            "profesor": "nombre del profesor",
                            "dia": número del 1 (Lunes) al 7 (Domingo),
                            "horaInicio": "HH:mm",
                            "horaFin": "HH:mm",
                            "tipo": "CATEDRA" o "LABORATORIO" o "AYUDANTIA" o "TALLER"
                        }
                    ],
                    "confianza": número del 0 al 100
                }
                
                IMPORTANTE:
                - Extrae TODAS las clases que veas en la imagen
                - Si un curso tiene múltiples bloques, inclúyelos todos
                - Usa el formato de 24 horas para las horas (ej: "14:30")
                - Si no puedes identificar algo, usa valores por defecto razonables
                - NO inventes información que no esté en la imagen
                - Responde SOLO con el JSON, sin texto adicional
            """.trimIndent()

            val response = llamarClaudeAPI(imagenBase64, prompt)
            parsearRespuestaHorario(response)

        } catch (e: Exception) {
            e.printStackTrace()
            ResultadoHorarioIA(
                exito = false,
                clases = emptyList(),
                confianza = 0.0,
                mensaje = "Error: ${e.message}"
            )
        }
    }

    private fun llamarClaudeAPI(imagenBase64: String, prompt: String): String {
        val requestBody = JSONObject().apply {
            put("model", "claude-sonnet-4-20250514")
            put("max_tokens", 4096)
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

    private fun parsearRespuestaHorario(response: String): ResultadoHorarioIA {
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
            val clasesArray = datos.getJSONArray("clases")
            val confianza = datos.optDouble("confianza", 0.0)

            val clases = mutableListOf<ClaseHorario>()
            for (i in 0 until clasesArray.length()) {
                val claseObj = clasesArray.getJSONObject(i)

                try {
                    val clase = ClaseHorario(
                        id = "clase_${System.currentTimeMillis()}_$i",
                        idCurso = "", // Se asignará después
                        nombreCurso = claseObj.getString("nombreCurso"),
                        sala = claseObj.optString("sala", "Por definir"),
                        profesor = claseObj.optString("profesor", "Por definir"),
                        diaSemana = DiaSemana.fromNumero(claseObj.getInt("dia")),
                        horaInicio = claseObj.getString("horaInicio"),
                        horaFin = claseObj.getString("horaFin"),
                        tipoClase = parsearTipoClase(claseObj.optString("tipo", "CATEDRA")),
                        color = generarColorAleatorio()
                    )
                    clases.add(clase)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Continuar con la siguiente clase si hay error
                }
            }

            return ResultadoHorarioIA(
                exito = clases.isNotEmpty(),
                clases = clases,
                confianza = confianza,
                mensaje = if (clases.isNotEmpty())
                    "Se encontraron ${clases.size} clases"
                else
                    "No se encontraron clases en la imagen"
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return ResultadoHorarioIA(
                exito = false,
                clases = emptyList(),
                confianza = 0.0,
                mensaje = "Error al parsear respuesta: ${e.message}"
            )
        }
    }

    private fun parsearTipoClase(tipo: String): TipoClase {
        return when (tipo.uppercase()) {
            "CATEDRA", "CLASE" -> TipoClase.CATEDRA
            "LABORATORIO", "LAB" -> TipoClase.LABORATORIO
            "AYUDANTIA" -> TipoClase.AYUDANTIA
            "TALLER" -> TipoClase.TALLER
            else -> TipoClase.CATEDRA
        }
    }

    private fun generarColorAleatorio(): String {
        val colores = listOf(
            "#6200EE", "#03DAC6", "#FF6B6B", "#4ECDC4",
            "#45B7D1", "#FFA07A", "#98D8C8", "#F7DC6F",
            "#BB8FCE", "#85C1E2", "#F8B739", "#52B788"
        )
        return colores.random()
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