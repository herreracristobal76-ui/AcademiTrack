package com.academitrack.app.services

import android.graphics.Bitmap
import android.graphics.Matrix
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
 * Servicio optimizado para procesar imágenes de horarios con IA
 */
class HorarioIAService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "HorarioIAService"
        private const val MAX_IMAGE_SIZE = 800 // Tamaño máximo de la imagen
        private const val JPEG_QUALITY = 70 // Calidad JPEG (0-100)
    }

    /**
     * Procesa una imagen de horario y extrae las clases
     */
    suspend fun procesarImagenHorario(
        imagenBase64: String,
        cursos: List<Curso>
    ): ResultadoHorarioIA = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando procesamiento de horario")
            Log.d(TAG, "Tamaño imagen original: ${imagenBase64.length} chars")

            // Optimizar imagen antes de enviar
            val imagenOptimizada = optimizarImagen(imagenBase64)
            Log.d(TAG, "Tamaño imagen optimizada: ${imagenOptimizada.length} chars")

            val cursosInfo = if (cursos.isNotEmpty()) {
                cursos.joinToString("\n") { "- ${it.getNombre()} (${it.getCodigo()})" }
            } else {
                "No hay cursos registrados previamente"
            }

            // Prompt simplificado y más directo
            val prompt = """
                Analiza este horario académico y extrae TODAS las clases.
                
                FORMATO ESPERADO DEL HORARIO:
                - Columnas: Días de la semana (Lunes a Viernes)
                - Filas: Módulos con horarios (ej: 1: 08:30-09:30)
                - Celdas: Información de clases
                
                INSTRUCCIONES:
                1. Por cada celda con texto, extrae:
                   - Nombre del curso
                   - Sala (si está)
                   - Profesor (si está)
                   - Día (1=Lun, 2=Mar, 3=Mié, 4=Jue, 5=Vie)
                   - Horario EXACTO del módulo
                
                2. Si un curso aparece en varios días, créalo como clases SEPARADAS
                
                3. IMPORTANTE: USA LOS HORARIOS EXACTOS DEL HORARIO, NO inventes
                
                Cursos conocidos:
                $cursosInfo
                
                Responde SOLO con JSON (sin markdown):
                {
                    "clases": [
                        {
                            "nombreCurso": "nombre",
                            "sala": "sala o 'Por definir'",
                            "profesor": "profesor o 'Por definir'",
                            "dia": 1-5,
                            "horaInicio": "HH:mm",
                            "horaFin": "HH:mm",
                            "tipo": "CATEDRA o LABORATORIO"
                        }
                    ]
                }
            """.trimIndent()

            val response = llamarGeminiAPI(imagenOptimizada, prompt)
            parsearRespuestaHorario(response)

        } catch (e: Exception) {
            Log.e(TAG, "Error procesando imagen", e)
            ResultadoHorarioIA(
                exito = false,
                clases = emptyList(),
                confianza = 0.0,
                mensaje = """
                    ❌ Error al procesar la imagen
                    
                    ${e.message}
                    
                    Posibles causas:
                    • Problema de conexión a internet
                    • API Key inválida o sin cuota
                    • Imagen muy grande o poco clara
                    • Formato de horario no reconocible
                    
                    Sugerencias:
                    ✓ Verifica tu conexión a internet
                    ✓ Asegúrate de que la API Key sea válida
                    ✓ Toma una foto más clara del horario
                    ✓ Verifica que sea un horario académico estándar
                """.trimIndent()
            )
        }
    }

    /**
     * Optimiza la imagen reduciéndola y comprimiéndola
     */
    private fun optimizarImagen(imagenBase64: String): String {
        return try {
            // Decodificar imagen
            val imageBytes = Base64.decode(imagenBase64, Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            // Calcular nuevo tamaño manteniendo proporción
            val ratio = Math.min(
                MAX_IMAGE_SIZE.toFloat() / bitmap.width,
                MAX_IMAGE_SIZE.toFloat() / bitmap.height
            )

            val newWidth = (bitmap.width * ratio).toInt()
            val newHeight = (bitmap.height * ratio).toInt()

            // Redimensionar si es necesario
            val resizedBitmap = if (ratio < 1.0f) {
                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else {
                bitmap
            }

            // Comprimir a JPEG
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            val compressedBytes = outputStream.toByteArray()

            // Convertir a Base64
            Base64.encodeToString(compressedBytes, Base64.NO_WRAP)

        } catch (e: Exception) {
            Log.w(TAG, "No se pudo optimizar imagen, usando original", e)
            imagenBase64
        }
    }

    /**
     * Llama a la API de Gemini probando múltiples modelos
     */
    private fun llamarGeminiAPI(imagenBase64: String, prompt: String): String {
        val modelos = listOf(
            "gemini-1.5-flash-latest",
            "gemini-1.5-flash",
            "gemini-pro-vision"
        )

        var ultimoError = ""

        for (modelo in modelos) {
            try {
                Log.d(TAG, "Probando modelo: $modelo")
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent?key=$apiKey"

                return llamarAPIConModelo(url, imagenBase64, prompt)
            } catch (e: Exception) {
                ultimoError = e.message ?: "Error desconocido"
                Log.w(TAG, "Falló $modelo: $ultimoError")

                // Si no es 404, lanzar error inmediatamente
                if (!ultimoError.contains("404")) {
                    throw e
                }
                // Si es 404, continuar con el siguiente modelo
            }
        }

        throw Exception("⚠️ Ningún modelo funcionó.\n\n" +
                "Último error: $ultimoError\n\n" +
                "Posibles causas:\n" +
                "• API Key inválida o expirada\n" +
                "• Sin cuota disponible\n" +
                "• Región no soportada\n\n" +
                "Solución:\n" +
                "1. Ve a https://aistudio.google.com/app/apikey\n" +
                "2. Crea una nueva API Key\n" +
                "3. Copia y pega en MainActivity.kt")
    }

    private fun llamarAPIConModelo(url: String, imagenBase64: String, prompt: String): String {
        Log.d(TAG, "Llamando a: $url")

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
        }.toString()

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        Log.d(TAG, "Enviando request (${requestBody.length} bytes)")

        val response = client.newCall(request).execute()

        Log.d(TAG, "Response code: ${response.code}")

        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            Log.e(TAG, "API Error: $errorBody")

            val mensajeError = when (response.code) {
                400 -> "Solicitud inválida. Verifica el formato de la imagen."
                401 -> "API Key inválida. Ve a https://aistudio.google.com/app/apikey"
                403 -> "Acceso denegado. Verifica los permisos de tu API Key."
                404 -> "Modelo no encontrado. Probando otro modelo..."
                429 -> "Límite de solicitudes alcanzado. Espera 1 minuto."
                500, 503 -> "Error en el servidor de Google. Intenta más tarde."
                else -> "Error ${response.code}: ${response.message}"
            }

            throw Exception(mensajeError)
        }

        return response.body?.string() ?: throw Exception("Respuesta vacía del servidor")
    }

    /**
     * Parsea la respuesta de la API
     */
    private fun parsearRespuestaHorario(response: String): ResultadoHorarioIA {
        try {
            val jsonResponse = JSONObject(response)

            if (jsonResponse.has("error")) {
                val error = jsonResponse.getJSONObject("error")
                throw Exception(error.getString("message"))
            }

            val candidates = jsonResponse.getJSONArray("candidates")
            if (candidates.length() == 0) {
                throw Exception("No se generó respuesta")
            }

            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val textoRespuesta = parts.getJSONObject(0).getString("text")

            Log.d(TAG, "Respuesta IA: ${textoRespuesta.take(200)}...")

            // Limpiar markdown
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
                        tipoClase = parsearTipo(obj.optString("tipo", "CATEDRA")),
                        color = generarColor(obj.getString("nombreCurso"))
                    )
                    clases.add(clase)
                    Log.d(TAG, "✓ Clase: ${clase.nombreCurso} ${clase.diaSemana.nombreCorto} ${clase.horaInicio}")
                } catch (e: Exception) {
                    Log.w(TAG, "Error parseando clase $i", e)
                }
            }

            return ResultadoHorarioIA(
                exito = clases.isNotEmpty(),
                clases = clases,
                confianza = if (clases.size >= 10) 85.0 else 70.0,
                mensaje = when {
                    clases.isEmpty() -> "No se encontraron clases. Verifica que sea un horario válido."
                    clases.size < 5 -> "Se encontraron ${clases.size} clases. Puede que falten algunas."
                    else -> "✓ ${clases.size} clases detectadas correctamente"
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error parseando respuesta", e)
            return ResultadoHorarioIA(
                exito = false,
                clases = emptyList(),
                confianza = 0.0,
                mensaje = "Error al interpretar la respuesta: ${e.message}"
            )
        }
    }

    private fun parsearTipo(tipo: String): TipoClase {
        return when {
            tipo.contains("LAB", ignoreCase = true) -> TipoClase.LABORATORIO
            tipo.contains("AYUD", ignoreCase = true) -> TipoClase.AYUDANTIA
            tipo.contains("TALL", ignoreCase = true) -> TipoClase.TALLER
            else -> TipoClase.CATEDRA
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

data class ValidacionHorario(
    val esValido: Boolean,
    val razon: String
)