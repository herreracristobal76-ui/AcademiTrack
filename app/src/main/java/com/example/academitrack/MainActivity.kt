package com.academitrack.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.academitrack.app.domain.*
import com.academitrack.app.services.*
import com.academitrack.app.ui.*
import com.academitrack.app.persistence.PersistenciaLocal

class MainActivity : ComponentActivity() {

    private val gestorNotas = GestorNotas()
    private val gestorAsistencia = GestorAsistencia()
    private val gestorHorario = GestorHorario()
    private lateinit var persistencia: PersistenciaLocal

    // 🔑 MULTI-API-KEY: Agrega todas las API Keys que quieras
    // Crea más en: https://aistudio.google.com/app/apikey
    private val apiKeys = listOf( // Tu API Key actual
        "AIzaSyA9eOAua2Fh5GuEoMD2G618dJNuKrEz-24"                  // API Key 4
    )
    init {
        if (apiKeys.size == 1) {
            android.util.Log.w("MainActivity", "⚠️ ADVERTENCIA: Solo tienes 1 API Key. Crea más para evitar límites.")
        } else {
            android.util.Log.i("MainActivity", "✅ Usando ${apiKeys.size} API Keys (${apiKeys.size * 15} req/min)")
        }
    }
    // 🔄 Rotación automática de API Keys
    private var currentKeyIndex = 0

    private fun getNextApiKey(): String {
        val key = apiKeys[currentKeyIndex]
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size
        return key
    }

    // Solo necesitas cambiar el setContent en MainActivity.kt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        persistencia = PersistenciaLocal(this)

        // Cargar datos
        val evaluacionesGuardadas = persistencia.cargarEvaluaciones()
        evaluacionesGuardadas.forEach { (_, eval) -> gestorNotas.agregarEvaluacion(eval) }
        gestorAsistencia.cargarAsistencias(persistencia.cargarAsistencias())
        gestorHorario.cargarClases(persistencia.cargarHorarios())

        setContent {
            var modoOscuro by remember { mutableStateOf(persistencia.cargarPreferenciaModoOscuro()) }
            var mostrarSplash by remember { mutableStateOf(true) }

            AcademiTrackTheme(darkTheme = modoOscuro) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // ⚡ OPTIMIZADO: Crossfade más rápido (400ms)
                    Crossfade(targetState = mostrarSplash, animationSpec = tween(400), label = "Splash") { isSplash ->
                        if (isSplash) SplashScreen { mostrarSplash = false }
                        else AcademiTrackApp(
                            gestorNotas,
                            gestorAsistencia,
                            gestorHorario,
                            persistencia,
                            modoOscuro,
                            onCambiarModo = { modoOscuro = it; persistencia.guardarPreferenciaModoOscuro(it) },
                            onGetApiKey = { getNextApiKey() }
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        persistencia.guardarAsistencias(gestorAsistencia.obtenerTodasAsistencias())
        persistencia.guardarHorarios(gestorHorario.obtenerTodasClases())
    }
}

@Composable
fun AcademiTrackApp(
    gestorNotas: GestorNotas,
    gestorAsistencia: GestorAsistencia,
    gestorHorario: GestorHorario,
    persistencia: PersistenciaLocal,
    modoOscuro: Boolean,
    onCambiarModo: (Boolean) -> Unit,
    onGetApiKey: () -> String  // 🔑 NUEVO: Función para obtener API Key
) {
    var pantallaActual by remember { mutableStateOf("cursos") }
    var cursoSeleccionado by remember { mutableStateOf<Curso?>(null) }
    var semestreSeleccionado by remember { mutableStateOf<Semestre?>(null) }
    val context = LocalContext.current

    val cursos = remember { mutableStateListOf<Curso>().apply { addAll(persistencia.cargarCursos()) } }
    val triggerUpdate = remember { mutableStateOf(0L) }

    LaunchedEffect(cursos.size) { if (cursos.isNotEmpty()) persistencia.guardarCursos(cursos.toList()) }

    val pantallasPrincipales = listOf("cursos", "calendario_mensual", "estadisticas")

    Scaffold(
        bottomBar = {
            if (pantallaActual in pantallasPrincipales) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        icon = { Icon(if(pantallaActual=="cursos") Icons.Filled.School else Icons.Outlined.School, null) },
                        label = { Text("Ramos") },
                        selected = pantallaActual == "cursos",
                        onClick = { pantallaActual = "cursos" }
                    )
                    NavigationBarItem(
                        icon = { Icon(if(pantallaActual=="calendario_mensual") Icons.Filled.DateRange else Icons.Outlined.DateRange, null) },
                        label = { Text("Calendario") },
                        selected = pantallaActual == "calendario_mensual",
                        onClick = { pantallaActual = "calendario_mensual" }
                    )
                    NavigationBarItem(
                        icon = { Icon(if(pantallaActual=="estadisticas") Icons.Filled.BarChart else Icons.Outlined.BarChart, null) },
                        label = { Text("Gráficos") },
                        selected = pantallaActual == "estadisticas",
                        onClick = { pantallaActual = "estadisticas" }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (pantallaActual) {
                "cursos" -> CursosScreen(
                    cursos = cursos.filter { it.estaActivo() },
                    onCursoClick = { curso -> cursoSeleccionado = curso; pantallaActual = "detalle" },
                    onAgregarCurso = { pantallaActual = "agregar_curso" },
                    onAjustes = { pantallaActual = "ajustes" },
                    onVerCalendario = { pantallaActual = "calendario_mensual" },
                    onVerArchivados = { pantallaActual = "cursos_archivados" },
                    onEliminarCurso = { curso ->
                        gestorHorario.obtenerClasesPorCurso(curso.getId()).forEach { gestorHorario.eliminarClase(it.id) }
                        cursos.remove(curso)
                        persistencia.guardarCursos(cursos.toList())
                        persistencia.guardarHorarios(gestorHorario.obtenerTodasClases())
                        Toast.makeText(context, "Curso eliminado", Toast.LENGTH_SHORT).show()
                    }
                )

                "calendario_mensual" -> CalendarioMensualScreen(
                    gestorHorario = gestorHorario,
                    gestorAsistencia = gestorAsistencia,
                    cursos = cursos.filter { it.estaActivo() },
                    onVolverClick = { pantallaActual = "cursos" },
                    onRegistrarHorario = { pantallaActual = "seleccionar_semestre" },
                    onLimpiarHorario = {
                        gestorHorario.obtenerTodasClases().forEach { gestorHorario.eliminarClase(it.id) }
                        persistencia.guardarHorarios(emptyList())
                        Toast.makeText(context, "Horario limpiado", Toast.LENGTH_SHORT).show()
                    },
                    onRegistrarAsistencia = { id, f, e ->
                        gestorAsistencia.registrarOActualizar(id, f, e)
                        persistencia.guardarAsistencias(gestorAsistencia.obtenerTodasAsistencias())
                        Toast.makeText(context, "Estado actualizado", Toast.LENGTH_SHORT).show()
                    },
                    onEliminarClase = { clase ->
                        gestorHorario.eliminarClase(clase.id)
                        persistencia.guardarHorarios(gestorHorario.obtenerTodasClases())
                        Toast.makeText(context, "Clase eliminada", Toast.LENGTH_SHORT).show()
                    },
                    esPantallaPrincipal = true
                )

                "estadisticas" -> EstadisticasScreen(cursos, gestorNotas, gestorAsistencia)

                "cursos_archivados" -> CursosArchivadosScreen(
                    cursos = cursos.toList(),
                    onVolverClick = { pantallaActual = "cursos" },
                    onCursoClick = { c -> cursoSeleccionado = c; pantallaActual = "detalle_archivado" }
                )

                "detalle_archivado" -> cursoSeleccionado?.let { curso ->
                    DetalleCursoArchivadoScreen(curso, gestorNotas, gestorAsistencia, { pantallaActual = "cursos_archivados" }) {
                        curso.reactivar(it)
                        val idx = cursos.indexOfFirst { c -> c.getId() == curso.getId() }
                        if (idx != -1) cursos[idx] = curso
                        persistencia.guardarCursos(cursos.toList())
                        pantallaActual = "cursos"
                    }
                }

                "agregar_curso" -> AgregarCursoScreen({ pantallaActual = "cursos" }) { c, h ->
                    cursos.add(c)
                    h.forEach { gestorHorario.agregarClase(it) }
                    persistencia.guardarCursos(cursos.toList())
                    persistencia.guardarHorarios(gestorHorario.obtenerTodasClases())
                    pantallaActual = "cursos"
                }

                "detalle" -> cursoSeleccionado?.let { curso ->
                    key(triggerUpdate.value) {
                        DetalleCursoScreen(
                            curso = curso,
                            gestorNotas = gestorNotas,
                            gestorAsistencia = gestorAsistencia,
                            onVolverClick = { pantallaActual = "cursos" },
                            onAgregarNota = { pantallaActual = "agregar_nota" },
                            onAgregarConIA = { pantallaActual = "camera" },
                            onVerCalendario = { pantallaActual = "calendario" },
                            onEditarCurso = { pantallaActual = "editar_curso" },
                            onEliminarCurso = {
                                gestorHorario.obtenerClasesPorCurso(curso.getId()).forEach { gestorHorario.eliminarClase(it.id) }
                                cursos.remove(curso)
                                persistencia.guardarCursos(cursos.toList())
                                persistencia.guardarHorarios(gestorHorario.obtenerTodasClases())
                                pantallaActual = "cursos"
                            },
                            onArchivarCurso = { pantallaActual = "archivar_curso" },
                            onEliminarEvaluacion = { eval ->
                                gestorNotas.eliminarEvaluacion(eval.getId())
                                curso.eliminarEvaluacion(eval.getId())
                                persistencia.guardarEvaluaciones(gestorNotas.obtenerTodasEvaluaciones())
                                persistencia.guardarCursos(cursos.toList())
                                triggerUpdate.value = System.currentTimeMillis()
                                Toast.makeText(context, "Nota eliminada", Toast.LENGTH_SHORT).show()
                            },
                            onColorChanged = { nuevoColor ->
                                curso.setColor(nuevoColor)
                                val idx = cursos.indexOfFirst { it.getId() == curso.getId() }
                                if (idx != -1) cursos[idx] = curso
                                persistencia.guardarCursos(cursos.toList())
                                triggerUpdate.value = System.currentTimeMillis()
                            }
                        )
                    }
                }

                "agregar_nota" -> cursoSeleccionado?.let { curso ->
                    val disp = 100.0 - gestorNotas.calcularPorcentajeTotal(curso.getId())
                    AgregarNotaScreen(curso, disp, { pantallaActual = "detalle" }) { eval ->
                        gestorNotas.agregarEvaluacion(eval)
                        curso.agregarEvaluacion(eval.getId())
                        persistencia.guardarEvaluaciones(mapOf(eval.getId() to eval))
                        persistencia.guardarEvaluaciones(gestorNotas.obtenerTodasEvaluaciones())
                        persistencia.guardarCursos(cursos.toList())
                        pantallaActual = "detalle"
                    }
                }

                // 🔑 Usar API Key dinámica
                "camera" -> cursoSeleccionado?.let { curso ->
                    CameraScreen(curso, onGetApiKey(), { pantallaActual = "detalle" }) { eval ->
                        gestorNotas.agregarEvaluacion(eval)
                        curso.agregarEvaluacion(eval.getId())
                        persistencia.guardarEvaluaciones(gestorNotas.obtenerTodasEvaluaciones())
                        persistencia.guardarCursos(cursos.toList())
                        pantallaActual = "detalle"
                    }
                }

                "archivar_curso" -> cursoSeleccionado?.let { curso ->
                    val prom = gestorNotas.calcularPromedioActual(curso.getId())
                    ArchivarCursoScreen(curso, prom, { pantallaActual = "detalle" }) { est, n ->
                        curso.archivar(est, n)
                        val idx = cursos.indexOfFirst { it.getId() == curso.getId() }
                        if (idx != -1) cursos[idx] = curso
                        persistencia.guardarCursos(cursos.toList())
                        pantallaActual = "cursos"
                    }
                }

                "calendario" -> cursoSeleccionado?.let { curso ->
                    CalendarioAsistenciaScreen(curso, gestorAsistencia, { pantallaActual = "detalle" }) { f, e ->
                        gestorAsistencia.registrarOActualizar(curso.getId(), f, e)
                        persistencia.guardarAsistencias(gestorAsistencia.obtenerTodasAsistencias())
                    }
                }

                "editar_curso" -> cursoSeleccionado?.let { curso ->
                    val horarios = gestorHorario.obtenerClasesPorCurso(curso.getId())
                    EditarCursoScreen(curso, horarios, { pantallaActual = "detalle" }) { edit, nuevosHorarios ->
                        val idx = cursos.indexOfFirst { it.getId() == curso.getId() }
                        if (idx != -1) {
                            cursos[idx] = edit
                            cursoSeleccionado = edit
                            horarios.forEach { gestorHorario.eliminarClase(it.id) }
                            nuevosHorarios.forEach { gestorHorario.agregarClase(it) }
                            persistencia.guardarCursos(cursos.toList())
                            persistencia.guardarHorarios(gestorHorario.obtenerTodasClases())
                        }
                        pantallaActual = "detalle"
                    }
                }

                "ajustes" -> AjustesScreen(modoOscuro, onCambiarModo, { pantallaActual = "cursos" }, { pantallaActual = "notificaciones" })
                "notificaciones" -> ConfigurarNotificacionesScreen(cursos.filter { it.estaActivo() }, { pantallaActual = "ajustes" }, persistencia)
                "seleccionar_semestre" -> SeleccionarSemestreScreen({ pantallaActual = "calendario_mensual" }) { s -> semestreSeleccionado = s; pantallaActual = "registrar_horario" }

                // 🔑 Usar API Key dinámica
                "registrar_horario" -> semestreSeleccionado?.let { sem ->
                    RegistrarHorarioScreen(onGetApiKey(), cursos.filter { it.estaActivo() }, sem, { pantallaActual = "seleccionar_semestre" }) { res ->
                        res.cursosNuevos.forEach { cursos.add(it) }
                        res.clases.forEach { gestorHorario.agregarClase(it) }
                        persistencia.guardarCursos(cursos.toList())
                        persistencia.guardarHorarios(gestorHorario.obtenerTodasClases())
                        pantallaActual = "calendario_mensual"
                    }
                }
            }
        }
    }
}

@Composable
fun AcademiTrackTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val lightColors = lightColorScheme(
        primary = Color(0xFF4F46E5), onPrimary = Color.White,
        primaryContainer = Color(0xFFE0E7FF), onPrimaryContainer = Color(0xFF312E81),
        secondary = Color(0xFF10B981), onSecondary = Color.White,
        secondaryContainer = Color(0xFFD1FAE5), onSecondaryContainer = Color(0xFF065F46),
        tertiary = Color(0xFFF59E0B), background = Color(0xFFF8FAFC),
        surface = Color.White, onSurface = Color(0xFF1E293B),
        surfaceVariant = Color(0xFFF1F5F9), onSurfaceVariant = Color(0xFF64748B),
        error = Color(0xFFEF4444), errorContainer = Color(0xFFFEE2E2)
    )
    val darkColors = darkColorScheme(
        primary = Color(0xFF818CF8), onPrimary = Color(0xFF312E81),
        primaryContainer = Color(0xFF3730A3), onPrimaryContainer = Color(0xFFE0E7FF),
        secondary = Color(0xFF34D399), onSecondary = Color(0xFF064E3B),
        tertiary = Color(0xFFFBBF24), background = Color(0xFF0F172A),
        surface = Color(0xFF1E293B), onSurface = Color(0xFFF1F5F9),
        surfaceVariant = Color(0xFF334155), onSurfaceVariant = Color(0xFF94A3B8),
        error = Color(0xFFF87171)
    )
    MaterialTheme(colorScheme = if (darkTheme) darkColors else lightColors, typography = Typography(), content = content)
}