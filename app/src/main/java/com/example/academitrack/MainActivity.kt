package com.academitrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.academitrack.app.domain.*
import com.academitrack.app.services.*
import com.academitrack.app.ui.*
import com.academitrack.app.persistence.PersistenciaLocal

class MainActivity : ComponentActivity() {

    private val gestorNotas = GestorNotas()
    private val gestorAsistencia = GestorAsistencia()
    private lateinit var persistencia: PersistenciaLocal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        persistencia = PersistenciaLocal(this)

        setContent {
            var modoOscuro by remember {
                mutableStateOf(persistencia.cargarPreferenciaModoOscuro())
            }

            AcademiTrackTheme(darkTheme = modoOscuro) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AcademiTrackApp(
                        gestorNotas = gestorNotas,
                        gestorAsistencia = gestorAsistencia,
                        persistencia = persistencia,
                        modoOscuro = modoOscuro,
                        onCambiarModo = {
                            modoOscuro = it
                            persistencia.guardarPreferenciaModoOscuro(it)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AcademiTrackApp(
    gestorNotas: GestorNotas,
    gestorAsistencia: GestorAsistencia,
    persistencia: PersistenciaLocal,
    modoOscuro: Boolean,
    onCambiarModo: (Boolean) -> Unit
) {
    var pantallaActual by remember { mutableStateOf("cursos") }
    var cursoSeleccionado by remember { mutableStateOf<Curso?>(null) }

    val apiKey = "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"

    val cursos = remember {
        mutableStateListOf<Curso>().apply {
            addAll(persistencia.cargarCursos())
        }
    }

    // Guardar cursos cuando cambien
    LaunchedEffect(cursos.size) {
        if (cursos.isNotEmpty()) {
            persistencia.guardarCursos(cursos.toList())
        }
    }

    when (pantallaActual) {
        "cursos" -> {
            CursosScreen(
                cursos = cursos,
                onCursoClick = { curso ->
                    cursoSeleccionado = curso
                    pantallaActual = "detalle"
                },
                onAgregarCurso = {
                    pantallaActual = "agregar_curso"
                },
                onAjustes = {
                    pantallaActual = "ajustes"
                }
            )
        }

        "agregar_curso" -> {
            AgregarCursoScreen(
                onVolverClick = { pantallaActual = "cursos" },
                onGuardar = { curso ->
                    cursos.add(curso)
                    persistencia.guardarCursos(cursos.toList())
                    pantallaActual = "cursos"
                }
            )
        }

        "detalle" -> {
            cursoSeleccionado?.let { curso ->
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
                        cursos.remove(curso)
                        persistencia.guardarCursos(cursos.toList())
                        pantallaActual = "cursos"
                    }
                )
            }
        }

        "agregar_nota" -> {
            cursoSeleccionado?.let { curso ->
                AgregarNotaScreen(
                    curso = curso,
                    onVolverClick = { pantallaActual = "detalle" },
                    onGuardar = { eval ->
                        gestorNotas.agregarEvaluacion(eval)
                        pantallaActual = "detalle"
                    }
                )
            }
        }

        "camera" -> {
            cursoSeleccionado?.let { curso ->
                CameraScreen(
                    curso = curso,
                    apiKey = apiKey,
                    onVolverClick = { pantallaActual = "detalle" },
                    onGuardar = { eval ->
                        gestorNotas.agregarEvaluacion(eval)
                        pantallaActual = "detalle"
                    }
                )
            }
        }

        "calendario" -> {
            cursoSeleccionado?.let { curso ->
                CalendarioAsistenciaScreen(
                    curso = curso,
                    gestorAsistencia = gestorAsistencia,
                    onVolverClick = { pantallaActual = "detalle" },
                    onRegistrarAsistencia = { fecha, estado ->
                        val asist = Asistencia(
                            idAsistencia = "asist_${System.currentTimeMillis()}",
                            idCurso = curso.getId(),
                            fecha = fecha,
                            estado = estado
                        )
                        gestorAsistencia.registrarAsistencia(asist)
                    }
                )
            }
        }

        "editar_curso" -> {
            cursoSeleccionado?.let { curso ->
                EditarCursoScreen(
                    curso = curso,
                    onVolverClick = { pantallaActual = "detalle" },
                    onGuardar = { cursoEditado ->
                        val index = cursos.indexOfFirst { it.getId() == curso.getId() }
                        if (index != -1) {
                            cursos[index] = cursoEditado
                            cursoSeleccionado = cursoEditado
                            persistencia.guardarCursos(cursos.toList())
                        }
                        pantallaActual = "detalle"
                    }
                )
            }
        }

        "ajustes" -> {
            AjustesScreen(
                modoOscuro = modoOscuro,
                onCambiarModo = onCambiarModo,
                onVolverClick = { pantallaActual = "cursos" },
                onConfigNotificaciones = { pantallaActual = "notificaciones" }
            )
        }

        "notificaciones" -> {
            ConfigurarNotificacionesScreen(
                cursos = cursos,
                onVolverClick = { pantallaActual = "ajustes" },
                persistencia = persistencia
            )
        }
    }
}

@Composable
fun AcademiTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF6200EE),
            secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6),
            tertiary = androidx.compose.ui.graphics.Color(0xFF3700B3),
            background = androidx.compose.ui.graphics.Color(0xFF121212),
            surface = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
            error = androidx.compose.ui.graphics.Color(0xFFCF6679)
        )
    } else {
        lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF6200EE),
            secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6),
            tertiary = androidx.compose.ui.graphics.Color(0xFF3700B3),
            background = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
            surface = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
            error = androidx.compose.ui.graphics.Color(0xFFB00020)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}