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
        private const val MAX_IMAGE_SIZE = 1200  // ⬆️ AUMENTADO: Mejor calidad
        private const val JPEG_QUALITY = 85      // ⬆️ AUMENTADO: Mejor reconocimiento
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val MAX_REINTENTOS = 3
        private const val DELAY_BASE_MS = 5000L
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
                "gemini-2.5-flash",      // ⚡ MÁS RÁPIDO Y PRECISO
                "gemini-flash-latest",   // 🔄 SIEMPRE ACTUALIZADO
                "gemini-2.0-flash-001",  // 💪 ESTABLE Y CONFIABLE
                "gemini-2.5-pro"         // 🎯 ÚLTIMA OPCIÓN
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
                ❌ TODOS LOS MODELOS FALLARON
                
                Ninguno de los ${modelos.size} modelos disponibles respondió.
                
                DIAGNÓSTICO:
                Verifica el último error arriba en los logs (Logcat).
                
                SOLUCIONES COMUNES:
                
                1️⃣ SI ES ERROR 429 (Límite):
                   • Espera 1-2 minutos
                   • O agrega más API Keys en MainActivity.kt
                
                2️⃣ SI ES ERROR 401/403 (Permisos):
                   • Crea nueva API Key en: https://aistudio.google.com/
                   • Cópiala en MainActivity.kt línea 34
                
                3️⃣ SI ES ERROR 400 (Imagen):
                   • Recorta la imagen
                   • Toma foto más clara
                   • Reduce el tamaño
                
                4️⃣ SI NO HAY INTERNET:
                   • Verifica WiFi/Datos
                   • Prueba en otra red
                
                💡 MIENTRAS TANTO:
                Usa el modo manual (botón Cancelar) para agregar
                las clases manualmente. Es más rápido.
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
                    val delayMs = DELAY_BASE_MS * intentoActual
                    Log.w(TAG, "⏱️ Rate limit. Reintento $intentoActual/$MAX_REINTENTOS en ${delayMs/1000}s...")
                    delay(delayMs)
                    intentarConReintentos(modelo, imagenBase64, prompt, cursosExistentes, semestre, intentoActual + 1)
                }
                e.message?.contains("500") == true || e.message?.contains("503") == true -> {
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
                put("temperature", 0.2)        // ⬆️ Un poco más flexible para tablas complejas
                put("topK", 40)                // ⬆️ Más opciones para análisis
                put("topP", 0.8)               // ⬆️ Mejor para estructuras
                put("maxOutputTokens", 4096)   // ⬆️ Más espacio para muchas clases
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
            Log.e(TAG, "❌ Error HTTP ${response.code}: $responseBody")

            val errorMsg = when (response.code) {
                400 -> """
                    ❌ IMAGEN INVÁLIDA (400)
                    
                    La imagen es demasiado grande o está corrupta.
                    
                    SOLUCIONES:
                    • Recorta la imagen para que sea más pequeña
                    • Toma una foto más clara con menos zoom
                    • Intenta con formato JPG en vez de PNG
                """.trimIndent()

                401 -> """
                    ❌ API KEY INVÁLIDA (401)
                    
                    Tu API Key no funciona.
                    
                    SOLUCIONES:
                    1. Ve a: https://aistudio.google.com/app/apikey
                    2. Crea una nueva API Key
                    3. Cópiala en MainActivity.kt línea 34:
                       "AIzaSyA..." // <- Reemplaza aquí
                """.trimIndent()

                403 -> """
                    ❌ SIN ACCESO (403)
                    
                    Tu API Key no tiene permisos para este modelo.
                    
                    POSIBLES CAUSAS:
                    • Cuenta sin billing habilitado
                    • Región bloqueada
                    • API Key restringida
                    
                    SOLUCIONES:
                    1. Verifica en: https://console.cloud.google.com/
                    2. Habilita "Generative Language API"
                    3. Crea una nueva API Key sin restricciones
                """.trimIndent()

                404 -> "❌ Modelo $modelo no existe (404)"

                429 -> {
                    val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 60
                    """
                    ⏱️ LÍMITE ALCANZADO (429)
                    
                    Has usado todas tus solicitudes disponibles.
                    
                    LÍMITES DEL PLAN GRATUITO:
                    • 15 solicitudes por minuto
                    • 1,500 solicitudes por día
                    
                    SOLUCIONES:
                    1. Espera $retryAfter segundos
                    2. Usa el modo manual (más rápido)
                    3. Crea más API Keys (hasta 5 gratis)
                    
                    💡 CONSEJO:
                    En MainActivity.kt puedes agregar más keys:
                    private val apiKeys = listOf(
                        "AIzaSy...", // Key 1
                        "AIzaSy...", // Key 2 <- Agrega aquí
                        "AIzaSy..."  // Key 3
                    )
                    """.trimIndent()
                }

                500, 503 -> """
                    ⚠️ ERROR DEL SERVIDOR (${response.code})
                    
                    Google Gemini está temporalmente caído.
                    
                    SOLUCIONES:
                    • Espera 2-3 minutos
                    • Verifica: https://status.cloud.google.com/
                    • Usa el modo manual mientras tanto
                """.trimIndent()

                else -> """
                    ❌ ERROR DESCONOCIDO (${response.code})
                    
                    Respuesta del servidor:
                    ${responseBody?.take(200) ?: "Sin detalles"}
                    
                    Intenta:
                    • Reiniciar la app
                    • Verificar tu conexión
                    • Crear una nueva API Key
                """.trimIndent()
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