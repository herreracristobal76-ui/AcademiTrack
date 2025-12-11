package com.academitrack.app.services

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
import java.util.concurrent.TimeUnit

/**
 * Servicio para procesar imágenes de horarios con IA (Google Gemini)
 * VERSIÓN ACTUALIZADA - Solo usa gemini-1.5-flash
 */
class HorarioIAService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "HorarioIAService"
    }

    suspend fun procesarImagenHorario(
        imagenBase64: String,
        cursos: List<Curso>
    ): ResultadoHorarioIA = withContext(Dispatchers.IO) {
        try {
            val cursosInfo = cursos.joinToString("\n") {
                "- ${it.getNombre()} (${it.getCodigo()})"
            }

            // Primera fase: Validar formato
            val validacionPrompt = """
                Analiza esta imagen y determina si es un horario académico válido.
                
                Un horario válido DEBE tener:
                1. Una tabla con días de la semana (Lunes, Martes, Miércoles, Jueves, Viernes)
                2. Módulos numerados (1, 2, 3, etc.) con sus horarios (ej: 08:30-09:30)
                3. Celdas con información de clases que incluyan al menos el nombre del curso
                
                Responde SOLO con este JSON:
                {
                    "es_horario_valido": true o false,
                    "razon": "explicación de por qué es válido o no",
                    "tiene_modulos": true o false,
                    "tiene_dias": true o false,
                    "tiene_horarios": true o false
                }
            """.trimIndent()

            val validacionResponse = llamarGeminiAPI(imagenBase64, validacionPrompt)
            val esValido = validarFormatoHorario(validacionResponse)

            if (!esValido.esValido) {
                return@withContext ResultadoHorarioIA(
                    exito = false,
                    clases = emptyList(),
                    confianza = 0.0,
                    mensaje = """
                        ❌ Formato de horario inválido
                        
                        ${esValido.razon}
                        
                        📋 Formato requerido:
                        
                        Tu imagen debe ser un horario académico con:
                        
                        ✓ Columnas con días de la semana (Lunes a Viernes)
                        ✓ Filas con módulos numerados (1, 2, 3...)
                        ✓ Horarios de cada módulo (ej: 08:30-09:30)
                        ✓ Celdas con información de clases:
                          • Nombre del curso
                          • Sala
                          • Profesor
                        
                        Ejemplo de formato correcto:
                        | Módulo | Horario    | Lunes        | Martes       |
                        |   1    | 08:30-09:30| INF-215      | INF-215      |
                        |   2    | 09:35-10:35| Circuitos    | Circuitos    |
                        
                        Por favor, toma una foto clara de tu horario impreso o digital.
                    """.trimIndent()
                )
            }

            // Segunda fase: Extraer clases
            val extraccionPrompt = """
                Esta imagen contiene un horario académico semanal válido.
                
                Cursos del estudiante:
                $cursosInfo
                
                INSTRUCCIONES CRÍTICAS - SIGUE EXACTAMENTE:
                
                1. IDENTIFICACIÓN DE MÓDULOS:
                   - Busca la columna izquierda que dice "Módulo" o "Bloque"
                   - Cada fila tiene un número (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, etc.)
                   - Cada módulo tiene su HORARIO EXACTO (ej: 08:30-09:30, 09:35-10:35)
                
                2. LECTURA DE HORARIOS:
                   - LEE los horarios de la columna "Horario" o similar
                   - Formato típico: "HH:mm - HH:mm" o "HH:mm-HH:mm"
                   - Ejemplos: "08:30-09:30", "11:55-12:55", "14:10-15:10"
                   - USA ESTOS HORARIOS EXACTOS, NO inventes otros
                
                3. LECTURA DE CLASES:
                   - Para cada día (Lunes, Martes, Miércoles, Jueves, Viernes)
                   - Lee de ARRIBA hacia ABAJO siguiendo los módulos
                   - Si una celda contiene texto, extrae:
                     * Nombre del curso (puede incluir código como INF-215)
                     * Sala (ej: F-411, Lab DCI03, Sala 201)
                     * Profesor (si está visible)
                     * Tipo: LABORATORIO si dice "Lab" o "Laboratorio", sino CATEDRA
                
                4. FORMATO DE RESPUESTA:
                   Devuelve un JSON con este formato EXACTO:
                   {
                       "clases": [
                           {
                               "nombreCurso": "nombre completo incluyendo código si está",
                               "codigo": "código extraído (ej: INF-215) o vacío",
                               "sala": "sala exacta como aparece",
                               "profesor": "nombre del profesor o 'Por definir'",
                               "dia": número 1-5 (1=Lunes, 5=Viernes),
                               "modulo": número del módulo (1, 2, 3...),
                               "horaInicio": "HH:mm del inicio EXACTO del módulo",
                               "horaFin": "HH:mm del fin EXACTO del módulo",
                               "tipo": "CATEDRA" o "LABORATORIO"
                           }
                       ],
                       "confianza": número 0-100
                   }
                
                EJEMPLOS DE EXTRACCIÓN CORRECTA:
                
                Si el horario muestra:
                - Módulo 1: 08:30-09:30
                - Lunes módulo 1: "INF-215 Circuitos / F-411 / Prof. García"
                
                Debes crear:
                {
                    "nombreCurso": "INF-215 Circuitos",
                    "codigo": "INF-215",
                    "sala": "F-411",
                    "profesor": "Prof. García",
                    "dia": 1,
                    "modulo": 1,
                    "horaInicio": "08:30",
                    "horaFin": "09:30",
                    "tipo": "CATEDRA"
                }
                
                REGLAS IMPORTANTES:
                - Si un curso está en VARIOS DÍAS, créalo como CLASES SEPARADAS
                - Si no encuentras el profesor, usa "Por definir"
                - Si no encuentras la sala, usa "Por definir"
                - NUNCA inventes horarios, usa SOLO los del horario
                - Formato 24 horas SIEMPRE (14:30 NO 2:30 PM)
                - Si hay información de sección (S1, S2), inclúyela en nombreCurso
                
                Responde SOLO con el JSON, sin markdown ni explicaciones.
            """.trimIndent()

            val response = llamarGeminiAPI(imagenBase64, extraccionPrompt)
            parsearRespuestaHorario(response)

        } catch (e: Exception) {
            Log.e(TAG, "Error procesando imagen", e)
            e.printStackTrace()
            ResultadoHorarioIA(
                exito = false,
                clases = emptyList(),
                confianza = 0.0,
                mensaje = "Error al procesar la imagen: ${e.message}"
            )
        }
    }

    private fun validarFormatoHorario(response: String): ValidacionHorario {
        return try {
            val jsonResponse = JSONObject(response)

            if (jsonResponse.has("error")) {
                return ValidacionHorario(
                    esValido = false,
                    razon = "Error al validar: ${jsonResponse.getJSONObject("error").getString("message")}"
                )
            }

            val candidates = jsonResponse.getJSONArray("candidates")
            if (candidates.length() == 0) {
                return ValidacionHorario(false, "No se pudo analizar la imagen")
            }

            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val textoRespuesta = parts.getJSONObject(0).getString("text")

            val jsonLimpio = textoRespuesta
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val datos = JSONObject(jsonLimpio)

            val esValido = datos.optBoolean("es_horario_valido", false)
            val razon = datos.optString("razon", "No se pudo determinar")
            val tieneModulos = datos.optBoolean("tiene_modulos", false)
            val tieneDias = datos.optBoolean("tiene_dias", false)
            val tieneHorarios = datos.optBoolean("tiene_horarios", false)

            ValidacionHorario(
                esValido = esValido && tieneModulos && tieneDias && tieneHorarios,
                razon = if (esValido) razon else buildString {
                    append(razon)
                    if (!tieneModulos) append("\n• Faltan módulos numerados")
                    if (!tieneDias) append("\n• Faltan días de la semana")
                    if (!tieneHorarios) append("\n• Faltan horarios de los módulos")
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error validando formato", e)
            ValidacionHorario(
                esValido = false,
                razon = "No se pudo validar la imagen. Asegúrate de que sea un horario académico claro."
            )
        }
    }

    private fun llamarGeminiAPI(imagenBase64: String, prompt: String): String {
        // Usar solo gemini-1.5-flash que es el modelo que funciona
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
                put("temperature", 0.1)  // Más determinista
                put("topK", 10)          // Menos variabilidad
                put("topP", 0.5)         // Más preciso
                put("maxOutputTokens", 8192)  // Más espacio para respuestas largas
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

        if (!response.isSuccessful) {
            throw Exception("API Error: ${response.code} - ${response.message}\nBody: ${responseBody?.take(200)}")
        }

        return responseBody ?: throw Exception("Respuesta vacía")
    }

    private fun parsearRespuestaHorario(response: String): ResultadoHorarioIA {
        try {
            val jsonResponse = JSONObject(response)

            if (jsonResponse.has("error")) {
                val error = jsonResponse.getJSONObject("error")
                throw Exception("API Error: ${error.getString("message")}")
            }

            val candidates = jsonResponse.getJSONArray("candidates")
            if (candidates.length() == 0) {
                throw Exception("No se recibieron candidatos en la respuesta")
            }

            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val textoRespuesta = parts.getJSONObject(0).getString("text")

            Log.d(TAG, "Texto respuesta IA: $textoRespuesta")

            val jsonLimpio = textoRespuesta
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val datos = JSONObject(jsonLimpio)
            val clasesArray = datos.getJSONArray("clases")
            val confianza = datos.optDouble("confianza", 75.0)

            val clases = mutableListOf<ClaseHorario>()
            for (i in 0 until clasesArray.length()) {
                val claseObj = clasesArray.getJSONObject(i)

                try {
                    val nombreCurso = claseObj.getString("nombreCurso")
                    val modulo = claseObj.optInt("modulo", 0)

                    val clase = ClaseHorario(
                        id = "clase_${System.currentTimeMillis()}_$i",
                        idCurso = "",
                        nombreCurso = nombreCurso,
                        sala = claseObj.optString("sala", "Por definir"),
                        profesor = claseObj.optString("profesor", "Por definir"),
                        diaSemana = DiaSemana.fromNumero(claseObj.getInt("dia")),
                        horaInicio = claseObj.getString("horaInicio"),
                        horaFin = claseObj.getString("horaFin"),
                        tipoClase = parsearTipoClase(claseObj.optString("tipo", "CATEDRA")),
                        color = generarColorPorCurso(nombreCurso)
                    )
                    clases.add(clase)
                    Log.d(TAG, "Clase parseada: ${clase.nombreCurso} - Módulo $modulo (${clase.horaInicio}-${clase.horaFin})")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parseando clase $i", e)
                    e.printStackTrace()
                }
            }

            return ResultadoHorarioIA(
                exito = clases.isNotEmpty(),
                clases = clases,
                confianza = confianza,
                mensaje = if (clases.isNotEmpty())
                    "✅ Se encontraron ${clases.size} clases en el horario"
                else
                    "⚠️ No se encontraron clases en la imagen. Verifica que sea un horario válido."
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error parseando respuesta", e)
            e.printStackTrace()
            return ResultadoHorarioIA(
                exito = false,
                clases = emptyList(),
                confianza = 0.0,
                mensaje = "Error al extraer clases: ${e.message}"
            )
        }
    }

    private fun parsearTipoClase(tipo: String): TipoClase {
        return when {
            tipo.contains("LABORATORIO", ignoreCase = true) ||
                    tipo.contains("LAB", ignoreCase = true) -> TipoClase.LABORATORIO
            tipo.contains("AYUDANTIA", ignoreCase = true) -> TipoClase.AYUDANTIA
            tipo.contains("TALLER", ignoreCase = true) -> TipoClase.TALLER
            else -> TipoClase.CATEDRA
        }
    }

    private fun generarColorPorCurso(nombreCurso: String): String {
        // Genera un color consistente basado en el nombre del curso
        val hash = nombreCurso.hashCode()
        val colores = listOf(
            "#6200EE", "#03DAC6", "#FF6B6B", "#4ECDC4",
            "#45B7D1", "#FFA07A", "#98D8C8", "#F7DC6F",
            "#BB8FCE", "#85C1E2", "#F8B739", "#52B788"
        )
        return colores[Math.abs(hash) % colores.size]
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