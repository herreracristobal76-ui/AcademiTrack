package com.academitrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
        inicializarDatosEjemplo()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AcademiTrackApp(
                        gestorNotas = gestorNotas,
                        gestorAsistencia = gestorAsistencia
                    )
                }
            }
        }
    }

    private fun inicializarDatosEjemplo() {
        // Evaluación 1
        val eval1 = EvaluacionManual(
            id = "eval_1",
            nombreEval = "Certamen 1",
            porcentajeEval = 30.0,
            fechaEval = System.currentTimeMillis(),
            idCursoEval = "prog_avanzada"
        )
        eval1.setNotaObtenida(5.5)
        gestorNotas.agregarEvaluacion(eval1)

        // Evaluación 2
        val eval2 = EvaluacionManual(
            id = "eval_2",
            nombreEval = "Taller 1",
            porcentajeEval = 15.0,
            fechaEval = System.currentTimeMillis(),
            idCursoEval = "prog_avanzada"
        )
        eval2.setNotaObtenida(6.0)
        gestorNotas.agregarEvaluacion(eval2)

        // Evaluación pendiente
        val eval3 = EvaluacionManual(
            id = "eval_3",
            nombreEval = "Certamen 2",
            porcentajeEval = 30.0,
            fechaEval = System.currentTimeMillis() + 86400000L * 7,
            idCursoEval = "prog_avanzada"
        )
        gestorNotas.agregarEvaluacion(eval3)

        // Evaluación pendiente 2
        val eval4 = EvaluacionManual(
            id = "eval_4",
            nombreEval = "Proyecto Final",
            porcentajeEval = 25.0,
            fechaEval = System.currentTimeMillis() + 86400000L * 14,
            idCursoEval = "prog_avanzada"
        )
        gestorNotas.agregarEvaluacion(eval4)

        // Asistencias
        for (i in 1..10) {
            val asist = Asistencia(
                idAsistencia = "asist_$i",
                idCurso = "prog_avanzada",
                fecha = System.currentTimeMillis() - (86400000L * i),
                estado = if (i % 4 == 0) EstadoAsistencia.AUSENTE else EstadoAsistencia.PRESENTE
            )
            gestorAsistencia.registrarAsistencia(asist)
        }
    }
}

@Composable
fun AcademiTrackApp(
    gestorNotas: GestorNotas,
    gestorAsistencia: GestorAsistencia
) {
    var pantallaActual by remember { mutableStateOf("cursos") }
    var cursoSeleccionado by remember { mutableStateOf<Curso?>(null) }

    val apiKey = "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"

    val cursos = remember {
        mutableStateListOf(
            Curso(
                idCurso = "prog_avanzada",
                nombre = "Programación Avanzada",
                codigo = "INF-301",
                porcentajeAsistenciaMinimo = 75.0,
                notaMinimaAprobacion = 4.0
            ),
            Curso(
                idCurso = "base_datos",
                nombre = "Base de Datos",
                codigo = "INF-302",
                porcentajeAsistenciaMinimo = 80.0,
                notaMinimaAprobacion = 4.0
            )
        )
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
                    // TODO: Implementar diálogo agregar curso
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
                    onAgregarConIA = { pantallaActual = "camera" }
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
    }
}

