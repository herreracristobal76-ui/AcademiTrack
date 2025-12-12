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
 * 🚀 VERSIÓN OPTIMIZADA - Usa modelos más rápidos y precisos
 */
class HorarioIAService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "HorarioIA"
        private const val MAX_IMAGE_SIZE = 1600
        private const val JPEG_QUALITY = 90
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val MAX_REINTENTOS = 3
        private const val DELAY_BASE_MS = 5000L
    }

    suspend fun procesarImagenHorario(
        imagenBase64: String,
        mimeType: String, // <-- ACEPTAR EL MIME TYPE
        cursosExistentes: List<Curso>,
        semestre: Semestre
    ): ResultadoHorarioConCursos = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Procesando horario para ${semestre.obtenerNombre()} (MIME: $mimeType)")

            // Solo optimizar la imagen si es una imagen JPEG o PNG estándar.
            val imagenParaEnvio = if (mimeType.startsWith("image") && (mimeType.contains("jpeg") || mimeType.contains("png"))) {
                optimizarImagen(imagenBase64)
            } else {
                // PDF o cualquier otro tipo de archivo se envía sin optimizar el Base64 original
                imagenBase64
            }

            Log.d(TAG, "📦 Datos a enviar: ${imagenParaEnvio.length / 1024}KB")

            val cursosInfo = if (cursosExistentes.isNotEmpty()) {
                "Cursos activos: ${cursosExistentes.filter { it.estaActivo() }.joinToString(", ") { it.getCodigo() }}"
            } else "Sin cursos previos"

            val prompt = """
                Analiza CUIDADOSAMENTE este horario universitario. Es una tabla con módulos (filas) y días (columnas).
                
                INSTRUCCIONES CRÍTICAS:
                1. Lee CADA celda que tenga texto (ignora celdas vacías)
                2. Identifica el CÓDIGO del curso (ej: INF-215, INF-213, MFG-114)
                3. Extrae el nombre completo del curso
                4. Identifica la SALA (ej: Sala I100, Sala F-307, Laboratorio DCI03)
                5. Extrae el nombre del PROFESOR
                6. Determina el DÍA de la semana según la columna
                7. Calcula las HORAS según el módulo (mira la columna izquierda)
                8. Si dice "Laboratorio" o "Lab" es tipo LABORATORIO, sino CATEDRA
                
                Semestre: ${semestre.obtenerNombre()}
                $cursosInfo
                
                FORMATO DE RESPUESTA (SOLO JSON, sin ```):
                {
                    "cursos": [
                        {
                            "nombre": "Circuitos digitales L2",
                            "codigo": "INF-215",
                            "clases": [
                                {
                                    "sala": "Laboratorio DCI03",
                                    "profesor": "Pablo Vilches",
                                    "dia": 1,
                                    "horaInicio": "08:30",
                                    "horaFin": "10:35",
                                    "tipo": "LABORATORIO"
                                }
                            ]
                        }
                    ]
                }
                
                REGLAS:
                • dia: 1=Lunes, 2=Martes, 3=Miércoles, 4=Jueves, 5=Viernes
                • tipo: LABORATORIO si dice "Lab/Laboratorio", sino CATEDRA
                • Si no ves el profesor, usa "Por asignar"
                • Si el código no es claro, extráelo del texto (ej: "INF-215" de "INF - 215 Circuitos...")
                • IMPORTANTE: Detecta TODAS las clases de TODOS los días
            """.trimIndent()

            // 🎯 MODELOS OPTIMIZADOS Y CORREGIDOS
            val modelos = listOf(
                "gemini-2.5-flash",
                "gemini-flash-latest",
                "gemini-2.0-flash-001",
                "gemini-2.5-pro"
            )

            Log.d(TAG, "🎯 Estrategia: Probar ${modelos.size} modelos optimizados")

            for ((index, modelo) in modelos.withIndex()) {
                if (index > 0) {
                    val delayMs = 2000L
                    Log.d(TAG, "⏳ Esperando ${delayMs/1000}s antes de probar siguiente modelo...")
                    delay(delayMs)
                }

                try {
                    Log.d(TAG, "📡 [${index + 1}/${modelos.size}] Intentando: $modelo")

                    val resultado = intentarConReintentos(
                        modelo = modelo,
                        imagenBase64 = imagenParaEnvio,
                        mimeType = mimeType,
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
                            continue
                        }
                        e.message?.contains("403") == true -> {
                            Log.w(TAG, "🔒 Sin acceso a $modelo. Probando siguiente...")
                            continue
                        }
                        e.message?.contains("404") == true -> {
                            Log.w(TAG, "❌ Modelo $modelo no existe. Probando siguiente...")
                            continue
                        }
                        else -> {
                            Log.e(TAG, "❌ Error grave: ${e.message}")
                            delay(3000)
                            continue
                        }
                    }
                }
            }

            throw Exception("""
                ⏱️ LÍMITE DE SOLICITUDES ALCANZADO
                
                Has superado el límite temporal de la API de Google o la imagen es demasiado compleja.
                
                ✅ SOLUCIONES INMEDIATAS:
                
                1️⃣ ESPERA 1-2 MINUTOS
                   • Es un límite temporal
                   • Se resetea automáticamente
                
                2️⃣ MEJORA LA CALIDAD
                   • Si es PDF: Asegúrate de que el PDF tenga texto seleccionable o intenta convertir solo la página del horario a imagen.
                   • Si es Foto: Asegúrate de que la tabla sea legible, con buen contraste y luz.
                
                3️⃣ USA EL MODO MANUAL
                   • Toca "Cancelar"
                   • Agrega las clases manualmente
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

    private suspend fun intentarConReintentos(
        modelo: String,
        imagenBase64: String,
        mimeType: String, // <-- ACEPTAR MIME TYPE
        prompt: String,
        cursosExistentes: List<Curso>,
        semestre: Semestre,
        intentoActual: Int = 1
    ): ResultadoHorarioConCursos {
        return try {
            llamarAPIYParsearConCursos(modelo, imagenBase64, prompt, mimeType, cursosExistentes, semestre)
        } catch (e: Exception) {
            when {
                e.message?.contains("429") == true && intentoActual < MAX_REINTENTOS -> {
                    val delayMs = DELAY_BASE_MS * intentoActual
                    Log.w(TAG, "⏱️ Rate limit. Reintento $intentoActual/$MAX_REINTENTOS en ${delayMs/1000}s...")
                    delay(delayMs)
                    intentarConReintentos(modelo, imagenBase64, mimeType, prompt, cursosExistentes, semestre, intentoActual + 1)
                }
                e.message?.contains("500") == true || e.message?.contains("503") == true -> {
                    if (intentoActual == 1) {
                        Log.w(TAG, "🔄 Error del servidor. Reintentando en 3s...")
                        delay(3000)
                        intentarConReintentos(modelo, imagenBase64, mimeType, prompt, cursosExistentes, semestre, 2)
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
        mimeType: String, // <-- ACEPTAR MIME TYPE
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
                                put("mime_type", mimeType) // <-- USAR MIME TYPE DINÁMICO
                                put("data", imagenBase64)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("topK", 40)
                put("topP", 0.8)
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
        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            val errorMsg = when (response.code) {
                429 -> {
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
                throw Exception("Sin respuesta de la IA. Posiblemente el contenido no fue detectado o fue bloqueado por filtros de seguridad.")
            }

            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val textoRespuesta = parts.getJSONObject(0).getString("text")

            val jsonLimpio = textoRespuesta
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val datos: JSONObject
            try {
                datos = JSONObject(jsonLimpio)
            } catch (e: Exception) {
                // El modelo no devolvió un JSON válido.
                throw Exception("El modelo de IA devolvió un texto no válido. Asegúrate de que la imagen es clara y el texto es nítido. Texto devuelto (inicio): ${jsonLimpio.take(100)}...")
            }

            // Usar optJSONArray para manejar con gracia si la clave 'cursos' no existe.
            val cursosArray = datos.optJSONArray("cursos")

            if (cursosArray == null || cursosArray.length() == 0) {
                // Si el modelo devolvió un JSON vacío o sin la estructura esperada, forzamos un error claro.
                throw Exception("El modelo de IA no pudo extraer la tabla. Asegúrate de que el horario sea legible y esté en formato de tabla (días/horas).")
            }

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
                    // Informar al usuario que una parte falló, pero continuar si es posible.
                    if (cursosArray.length() == 1) {
                        throw Exception("Error: Falla crítica al procesar los datos de la clase. Verifica si el JSON de respuesta es inválido o si faltan campos obligatorios (horaInicio, horaFin, dia).")
                    }
                }
            }

            if (todasLasClases.isEmpty()) {
                throw Exception("El modelo de IA no pudo extraer ninguna clase válida. Asegúrate de que el documento es un horario legible y que no contiene solo texto genérico.")
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
            // Captura cualquier error de análisis o excepción lanzada por los checks de robustez.
            Log.e(TAG, "Error fatal en parseo", e)
            throw e
        }
    }

    private fun optimizarImagen(imagenBase64: String): String {
        return try {
            val imageBytes = Base64.decode(imagenBase64, Base64.DEFAULT)
            // Se usa android.graphics.BitmapFactory.decodeByteArray para decodificar la imagen
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            if (bitmap == null) return imagenBase64

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
            // Si la optimización falla (ej. imagen corrupta), devolvemos el Base64 original
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