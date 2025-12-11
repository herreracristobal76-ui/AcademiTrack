package com.academitrack.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    onAgregarConIA: () -> Unit,
    onVerCalendario: () -> Unit,
    onEditarCurso: () -> Unit,
    onEliminarCurso: () -> Unit
) {
    var mostrarMenuOpciones by remember { mutableStateOf(false) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

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
                },
                actions = {
                    IconButton(onClick = { mostrarMenuOpciones = true }) {
                        Icon(Icons.Default.MoreVert, "Opciones")
                    }
                    DropdownMenu(
                        expanded = mostrarMenuOpciones,
                        onDismissRequest = { mostrarMenuOpciones = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("✏️ Editar curso") },
                            onClick = {
                                mostrarMenuOpciones = false
                                onEditarCurso()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🗑️ Eliminar curso") },
                            onClick = {
                                mostrarMenuOpciones = false
                                mostrarDialogoEliminar = true
                            }
                        )
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
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = curso.getCodigo(),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Nota mín: ${curso.getNotaMinimaAprobacion()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                Text(
                                    text = "Asist. mín:",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "${curso.getPorcentajeAsistenciaMinimo()}%",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }

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
                                progress = (100 - proyeccion.porcentajeRestante).toFloat() / 100f ,
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
                    onClick = onVerCalendario,
                    colors = CardDefaults.cardColors(
                        containerColor = if (porcentajeAsistencia >= curso.getPorcentajeAsistenciaMinimo())
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
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
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Ver calendario",
                            modifier = Modifier.size(48.dp)
                        )
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
                        Text("📷 IA")
                    }
                    OutlinedButton(
                        onClick = onAgregarNota,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("✏️ Manual")
                    }
                }
            }

            item {
                Text(
                    text = "Evaluaciones (${evaluaciones.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (evaluaciones.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            Text("📝 No hay evaluaciones registradas")
                            Text(
                                "Agrega tu primera evaluación",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                items(evaluaciones.sortedByDescending { it.getFecha() }) { eval ->
                    EvaluacionCard(eval)
                }
            }
        }
    }

    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("⚠️ Eliminar Curso") },
            text = {
                Text("¿Estás seguro de que quieres eliminar este curso? Se perderán todas las evaluaciones y registros de asistencia asociados.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEliminarCurso()
                        mostrarDialogoEliminar = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEliminar = false }) {
                    Text("Cancelar")
                }
            }
        )
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