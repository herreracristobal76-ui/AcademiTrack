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
import com.academitrack.app.domain.Semestre

class MainActivity : ComponentActivity() {

    private val gestorNotas = GestorNotas()
    private val gestorAsistencia = GestorAsistencia()
    private val gestorHorario = GestorHorario()
    private lateinit var persistencia: PersistenciaLocal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        persistencia = PersistenciaLocal(this)

        // Cargar datos guardados
        val evaluacionesGuardadas = persistencia.cargarEvaluaciones()
        evaluacionesGuardadas.forEach { (_, eval) ->
            gestorNotas.agregarEvaluacion(eval)
        }

        val asistenciasGuardadas = persistencia.cargarAsistencias()
        gestorAsistencia.cargarAsistencias(asistenciasGuardadas)

        val horariosGuardados = persistencia.cargarHorarios()
        gestorHorario.cargarClases(horariosGuardados)

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
                        gestorHorario = gestorHorario,
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
    onCambiarModo: (Boolean) -> Unit
) {
    var pantallaActual by remember { mutableStateOf("cursos") }
    var cursoSeleccionado by remember { mutableStateOf<Curso?>(null) }
    var semestreSeleccionado by remember { mutableStateOf<Semestre?>(null) }

    val apiKey = "AIzaSyDtpM1m_CHzXefhZ9zYcv3qWe1nnkt7rvo"

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
                cursos = cursos.filter { it.estaActivo() },
                onCursoClick = { curso ->
                    cursoSeleccionado = curso
                    pantallaActual = "detalle"
                },
                onAgregarCurso = {
                    pantallaActual = "agregar_curso"
                },
                onAjustes = {
                    pantallaActual = "ajustes"
                },
                onVerCalendario = {
                    pantallaActual = "calendario_mensual"
                },
                onVerArchivados = {
                    pantallaActual = "cursos_archivados"
                }
            )
        }

        "cursos_archivados" -> {
            CursosArchivadosScreen(
                cursos = cursos.toList(),
                onVolverClick = { pantallaActual = "cursos" },
                onCursoClick = { curso ->
                    cursoSeleccionado = curso
                    pantallaActual = "detalle_archivado"
                }
            )
        }

        "detalle_archivado" -> {
            cursoSeleccionado?.let { curso ->
                DetalleCursoArchivadoScreen(
                    curso = curso,
                    gestorNotas = gestorNotas,
                    gestorAsistencia = gestorAsistencia,
                    onVolverClick = { pantallaActual = "cursos_archivados" },
                    onReactivar = { nuevoSemestre ->
                        curso.reactivar(nuevoSemestre)
                        val index = cursos.indexOfFirst { it.getId() == curso.getId() }
                        if (index != -1) {
                            cursos[index] = curso
                            persistencia.guardarCursos(cursos.toList())
                        }
                        pantallaActual = "cursos"
                    }
                )
            }
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
                    },
                    onArchivarCurso = { pantallaActual = "archivar_curso" }
                )
            }
        }

        "archivar_curso" -> {
            cursoSeleccionado?.let { curso ->
                val promedio = gestorNotas.calcularPromedioActual(curso.getId())
                ArchivarCursoScreen(
                    curso = curso,
                    promedioActual = promedio,
                    onVolverClick = { pantallaActual = "detalle" },
                    onArchivar = { estado, notaFinal ->
                        curso.archivar(estado, notaFinal)
                        val index = cursos.indexOfFirst { it.getId() == curso.getId() }
                        if (index != -1) {
                            cursos[index] = curso
                            persistencia.guardarCursos(cursos.toList())
                        }
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
                    onGuardar = { eval: Evaluacion ->
                        gestorNotas.agregarEvaluacion(eval)
                        persistencia.guardarEvaluaciones(mapOf(eval.getId() to eval))
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
                    onGuardar = { eval: Evaluacion ->
                        gestorNotas.agregarEvaluacion(eval)
                        persistencia.guardarEvaluaciones(mapOf(eval.getId() to eval))
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
                    onRegistrarAsistencia = { fecha: Long, estado: EstadoAsistencia ->
                        val asist = Asistencia(
                            idAsistencia = "asist_${System.currentTimeMillis()}",
                            idCurso = curso.getId(),
                            fecha = fecha,
                            estado = estado
                        )
                        gestorAsistencia.registrarAsistencia(asist)
                        persistencia.guardarAsistencias(gestorAsistencia.obtenerTodasAsistencias())
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
                cursos = cursos.filter { it.estaActivo() },
                onVolverClick = { pantallaActual = "ajustes" },
                persistencia = persistencia
            )
        }

        "calendario_mensual" -> {
            CalendarioMensualScreen(
                gestorHorario = gestorHorario,
                gestorAsistencia = gestorAsistencia,
                cursos = cursos.filter { it.estaActivo() },
                onVolverClick = { pantallaActual = "cursos" },
                onRegistrarHorario = { pantallaActual = "seleccionar_semestre" },
                onVerHorarioSemanal = { pantallaActual = "horario_semanal" }
            )
        }

        "horario_semanal" -> {
            HorarioSemanalScreen(
                gestorHorario = gestorHorario,
                onVolverClick = { pantallaActual = "calendario_mensual" },
                onRegistrarHorario = { pantallaActual = "seleccionar_semestre" }
            )
        }

        "seleccionar_semestre" -> {
            SeleccionarSemestreScreen(
                onVolverClick = { pantallaActual = "horario_semanal" },
                onSemestreSeleccionado = { semestre ->
                    semestreSeleccionado = semestre
                    pantallaActual = "registrar_horario"
                }
            )
        }

        "registrar_horario" -> {
            semestreSeleccionado?.let { semestre ->
                RegistrarHorarioScreen(
                    apiKey = apiKey,
                    cursos = cursos.filter { it.estaActivo() },
                    semestre = semestre,
                    onVolverClick = { pantallaActual = "seleccionar_semestre" },
                    onGuardarHorario = { resultado ->
                        // Agregar cursos nuevos
                        resultado.cursosNuevos.forEach { nuevoCurso ->
                            cursos.add(nuevoCurso)
                        }

                        // Agregar clases al horario
                        resultado.clases.forEach { gestorHorario.agregarClase(it) }

                        // Guardar todo
                        persistencia.guardarCursos(cursos.toList())
                        persistencia.guardarHorarios(gestorHorario.obtenerTodasClases())

                        pantallaActual = "horario_semanal"
                    }
                )
            }
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