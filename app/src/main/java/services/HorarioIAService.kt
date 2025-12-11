package com.academitrack.app.services

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.academitrack.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
 * VERSIÓN OPTIMIZADA - Maneja Rate Limits (429) automáticamente
 */
class HorarioIAService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)  // ⬆️ Aumentado
        .readTimeout(90, TimeUnit.SECONDS)     // ⬆️ Aumentado
        .writeTimeout(90, TimeUnit.SECONDS)    // ⬆️ Aumentado
        .build()

    companion object {
        private const val TAG = "HorarioIA"
        private const val MAX_IMAGE_SIZE = 800  // ⬇️ Reducido para enviar menos datos
        private const val JPEG_QUALITY = 70     // ⬇️ Reducido para comprimir más
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val MAX_REINTENTOS = 3     // 🔄 Máximo de reintentos
        private const val DELAY_BASE_MS = 5000L  // ⏱️ 5 segundos entre reintentos
    }

    suspend fun procesarImagenHorario(
        imagenBase64: String,
        cursosExistentes: List<Curso>,
        semestre: Semestre
    ): ResultadoHorarioConCursos = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Procesando horario para ${semestre.obtenerNombre()}")

            val imagenOptimizada = optimizarImagen(imagenBase64)
            Log.d(TAG, "📦 Imagen optimizada: ${imagenOptimizada.length / 1024}KB")

            val cursosInfo = if (cursosExistentes.isNotEmpty()) {
                "Cursos activos: ${cursosExistentes.filter { it.estaActivo() }.joinToString(", ") { it.getCodigo() }}"
            } else "Sin cursos previos"

            val prompt = """
                Analiza este horario universitario y extrae SOLO la información visible.
                
                Semestre: ${semestre.obtenerNombre()}
                $cursosInfo
                
                Responde en JSON (sin markdown):
                {
                    "cursos": [
                        {
                            "nombre": "Nombre Curso",
                            "codigo": "COD-123",
                            "clases": [
                                {
                                    "sala": "A-201",
                                    "profesor": "Apellido",
                                    "dia": 1,
                                    "horaInicio": "08:30",
                                    "horaFin": "10:00",
                                    "tipo": "CATEDRA"
                                }
                            ]
                        }
                    ]
                }
                
                • dia: 1=Lun, 2=Mar, 3=Mié, 4=Jue, 5=Vie, 6=Sáb, 7=Dom
                • tipo: CATEDRA, LABORATORIO, AYUDANTIA, TALLER
                • Si no ves el código, inventa uno como "CURSO-001"
            """.trimIndent()

            // 🎯 ESTRATEGIA: Probar modelos en orden de éxito conocido
            val modelos = listOf(
                "gemini-1.5-flash-8b",      // ⚡ MÁS RÁPIDO = Menos rate limit
                "gemini-1.5-flash",         // ✅ Más confiable
                "gemini-2.0-flash-exp",     // 🆕 Experimental pero potente
                "gemini-1.5-pro"            // 💪 Último recurso (más lento)
            )

            Log.d(TAG, "🎯 Estrategia: Probar ${modelos.size} modelos con reintentos inteligentes")

            for ((index, modelo) in modelos.withIndex()) {
                // ⏱️ Agregar delay entre modelos para evitar rate limit
                if (index > 0) {
                    val delayMs = 2000L // 2 segundos entre cambios de modelo
                    Log.d(TAG, "⏳ Esperando ${delayMs/1000}s antes de probar siguiente modelo...")
                    delay(delayMs)
                }

                try {
                    Log.d(TAG, "📡 [${index + 1}/${modelos.size}] Intentando: $modelo")

                    // 🔄 Intentar con reintentos automáticos
                    val resultado = intentarConReintentos(
                        modelo = modelo,
                        imagenBase64 = imagenOptimizada,
                        prompt = prompt,
                        cursosExistentes = cursosExistentes,
                        semestre = semestre
                    )

                    Log.d(TAG, "✅ ¡ÉXITO con $modelo!")
                    return@withContext resultado

                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ [$modelo] Error: ${e.message}")

                    when {
                        e.message?.contains("429") == true -> {
                            Log.w(TAG, "⏱️ Rate limit alcanzado. Probando siguiente modelo...")
                            continue // Probar siguiente modelo
                        }
                        e.message?.contains("403") == true -> {
                            Log.w(TAG, "🔒 Sin acceso a $modelo. Probando siguiente...")
                            continue
                        }
                        e.message?.contains("404") == true -> {
                            continue // Modelo no existe
                        }
                        else -> {
                            // Error grave, esperar y continuar
                            Log.e(TAG, "❌ Error grave: ${e.message}")
                            delay(3000) // Esperar 3 segundos
                            continue
                        }
                    }
                }
            }

            // Si llegamos aquí, ningún modelo funcionó
            throw Exception("""
                ⏱️ LÍMITE DE SOLICITUDES ALCANZADO
                
                Has superado el límite temporal de la API de Google.
                
                ✅ SOLUCIONES INMEDIATAS:
                
                1️⃣ ESPERA 1-2 MINUTOS
                   • Es un límite temporal
                   • Se resetea automáticamente
                
                2️⃣ USA EL MODO MANUAL
                   • Toca "Cancelar"
                   • Agrega las clases manualmente
                   • Es más rápido que esperar
                
                📊 LÍMITES DEL PLAN GRATUITO:
                   • 15 solicitudes por minuto
                   • 1,500 solicitudes por día
                
                💡 CONSEJO:
                   Si usas mucho la IA, considera:
                   • Esperar unos minutos entre análisis
                   • Procesar varios horarios de una vez
                   • Subir imágenes más pequeñas
            """.trimIndent())

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fatal", e)
            ResultadoHorarioConCursos(
                exito = false,
                cursosNuevos = emptyList(),
                clases = emptyList(),
                confianza = 0.0,
                mensaje = e.message ?: "Error desconocido"
            )
        }
    }

    /**
     * 🔄 Intenta llamar a la API con reintentos automáticos en caso de rate limit
     */
    private suspend fun intentarConReintentos(
        modelo: String,
        imagenBase64: String,
        prompt: String,
        cursosExistentes: List<Curso>,
        semestre: Semestre,
        intentoActual: Int = 1
    ): ResultadoHorarioConCursos {
        return try {
            llamarAPIYParsearConCursos(modelo, imagenBase64, prompt, cursosExistentes, semestre)
        } catch (e: Exception) {
            when {
                e.message?.contains("429") == true && intentoActual < MAX_REINTENTOS -> {
                    // Rate limit: esperar con backoff exponencial
                    val delayMs = DELAY_BASE_MS * intentoActual
                    Log.w(TAG, "⏱️ Rate limit. Reintento $intentoActual/$MAX_REINTENTOS en ${delayMs/1000}s...")
                    delay(delayMs)
                    intentarConReintentos(modelo, imagenBase64, prompt, cursosExistentes, semestre, intentoActual + 1)
                }
                e.message?.contains("500") == true || e.message?.contains("503") == true -> {
                    // Error del servidor: reintentar una vez
                    if (intentoActual == 1) {
                        Log.w(TAG, "🔄 Error del servidor. Reintentando en 3s...")
                        delay(3000)
                        intentarConReintentos(modelo, imagenBase64, prompt, cursosExistentes, semestre, 2)
                    } else {
                        throw e
                    }
                }
                else -> throw e
            }
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
                put("maxOutputTokens", 2048) // ⬇️ Reducido para respuestas más rápidas
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
                429 -> {
                    // Extraer tiempo de espera si está disponible
                    val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 60
                    "Rate limit alcanzado. Espera ${retryAfter}s"
                }
                403 -> "Sin permisos para modelo $modelo"
                404 -> "Modelo $modelo no disponible"
                500, 503 -> "Error temporal del servidor"
                else -> "Error HTTP ${response.code}"
            }
            throw Exception(errorMsg)
        }

        return parsearRespuestaConCursos(responseBody ?: "", cursosExistentes, semestre)
    }

    private fun parsearRespuestaConCursos(
        responseBody: String,
        cursosExistentes: List<Curso>,
        semestre: Semestre
    ): ResultadoHorarioConCursos {
        try {
            val jsonResponse = JSONObject(responseBody)

            if (jsonResponse.has("error")) {
                val error = jsonResponse.getJSONObject("error")
                throw Exception("API: ${error.optString("message", "Error desconocido")}")
            }

            val candidates = jsonResponse.getJSONArray("candidates")
            if (candidates.length() == 0) {
                throw Exception("Sin respuesta de la IA")
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

                    val cursoExistente = cursosExistentes.find {
                        it.estaActivo() && (
                                it.getCodigo().equals(codigoCurso, ignoreCase = true) ||
                                        it.getNombre().equals(nombreCurso, ignoreCase = true)
                                )
                    }

                    val idCurso = cursoExistente?.getId() ?: "curso_${System.currentTimeMillis()}_$i"

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
                    }

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
            Log.e(TAG, "Error parseando respuesta", e)
            throw Exception("Error interpretando respuesta: ${e.message}")
        }
    }

    private fun optimizarImagen(imagenBase64: String): String {
        return try {
            val imageBytes = Base64.decode(imagenBase64, Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            if (bitmap == null) return imagenBase64

            // 🎯 Optimización agresiva para reducir tamaño y velocidad
            val maxDimension = MAX_IMAGE_SIZE
            val needsResize = bitmap.width > maxDimension || bitmap.height > maxDimension

            val finalBitmap = if (needsResize) {
                val ratio = Math.min(
                    maxDimension.toFloat() / bitmap.width,
                    maxDimension.toFloat() / bitmap.height
                )
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt(),
                    (bitmap.height * ratio).toInt(),
                    true
                )
            } else {
                bitmap
            }

            val stream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            val optimizedBase64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

            Log.d(TAG, "📦 Optimización: ${imageBytes.size / 1024}KB → ${stream.size() / 1024}KB")

            optimizedBase64
        } catch (e: Exception) {
            Log.w(TAG, "Error optimizando imagen", e)
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