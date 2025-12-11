package com.academitrack.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.academitrack.app.domain.*
import com.academitrack.app.services.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleCursoScreen(
    curso: Curso,
    gestorNotas: GestorNotas,
    gestorAsistencia: GestorAsistencia,
    onVolverClick: () -> Unit,
    onAgregarNota: () -> Unit,
    onAgregarConIA: () -> Unit = {}
){
    val promedio = gestorNotas.calcularPromedioActual(curso.getId())
    val porcentajeAsistencia = gestorAsistencia.calcularPorcentajeAsistencia(curso.getId())
    val evaluaciones = gestorNotas.obtenerEvaluacionesPorCurso(curso.getId())
    val stats = gestorAsistencia.obtenerEstadisticas(curso.getId())

    val proyeccion = gestorNotas.calcularNotaNecesaria(
        idCurso = curso.getId(),
        notaObjetivo = curso.getNotaMinimaAprobacion(),
        curso = curso
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(curso.getNombre()) },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (promedio >= curso.getNotaMinimaAprobacion())
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Promedio Actual",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = String.format("%.2f", promedio),
                            style = MaterialTheme.typography.displayLarge,
                            color = if (promedio >= curso.getNotaMinimaAprobacion())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                        if (promedio > 0) {
                            Text(
                                text = if (promedio >= curso.getNotaMinimaAprobacion())
                                    "✅ Vas aprobando"
                                else
                                    "⚠️ Bajo el mínimo (${curso.getNotaMinimaAprobacion()})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🎯 Proyección",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = proyeccion.mensaje,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (proyeccion.porcentajeRestante > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = (100 - proyeccion.porcentajeRestante).toFloat() / 100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Completado: ${String.format("%.0f", 100 - proyeccion.porcentajeRestante)}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (porcentajeAsistencia >= curso.getPorcentajeAsistenciaMinimo())
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📅 Asistencia",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${String.format("%.1f", porcentajeAsistencia)}%",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            text = "${stats.clasesAsistidas} de ${stats.totalClases} clases",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (stats.faltas > 0) {
                            Text(
                                text = "🔴 ${stats.faltas} faltas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAgregarConIA,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📷 Con IA")
                    }
                    OutlinedButton(
                        onClick = onAgregarNota,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("✍️ Manual")
                    }
                }
            }

            item {
                Text(
                    text = "Evaluaciones",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (evaluaciones.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("No hay evaluaciones registradas")
                        }
                    }
                }
            } else {
                items(evaluaciones) { eval ->
                    EvaluacionCard(eval)
                }
            }
        }
    }
}

@Composable
fun EvaluacionCard(evaluacion: Evaluacion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (evaluacion.estado) {
                EstadoEvaluacion.REALIZADA -> MaterialTheme.colorScheme.primaryContainer
                EstadoEvaluacion.PENDIENTE -> MaterialTheme.colorScheme.surfaceVariant
                EstadoEvaluacion.CANCELADA -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = evaluacion.getNombre(),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${evaluacion.getPorcentaje()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = evaluacion.obtenerTipoEvaluacion(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (evaluacion.notaObtenida != null)
                        "Nota: ${String.format("%.1f", evaluacion.notaObtenida)}"
                    else
                        "⏳ Pendiente",
                    style = MaterialTheme.typography.bodyLarge
                )
                if (evaluacion.notaObtenida != null) {
                    Text(
                        text = "Ponderado: ${String.format("%.2f", evaluacion.calcularNotaPonderada())}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}