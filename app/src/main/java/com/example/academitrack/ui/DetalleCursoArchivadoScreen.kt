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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleCursoArchivadoScreen(
    curso: Curso,
    gestorNotas: GestorNotas,
    gestorAsistencia: GestorAsistencia,
    onVolverClick: () -> Unit,
    onReactivar: (String?) -> Unit
) {
    var mostrarDialogoReactivar by remember { mutableStateOf(false) }

    val promedio = gestorNotas.calcularPromedioActual(curso.getId())
    val porcentajeAsistencia = gestorAsistencia.calcularPorcentajeAsistencia(curso.getId())
    val evaluaciones = gestorNotas.obtenerEvaluacionesPorCurso(curso.getId())
    val stats = gestorAsistencia.obtenerEstadisticas(curso.getId())

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

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
            // Estado del curso
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (curso.getEstado()) {
                            EstadoCurso.APROBADO -> MaterialTheme.colorScheme.primaryContainer
                            EstadoCurso.REPROBADO -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = when (curso.getEstado()) {
                                        EstadoCurso.APROBADO -> "✅ Curso Aprobado"
                                        EstadoCurso.REPROBADO -> "❌ Curso Reprobado"
                                        EstadoCurso.RETIRADO -> "🚪 Curso Retirado"
                                        else -> "Curso"
                                    },
                                    style = MaterialTheme.typography.titleLarge
                                )
                                curso.getNotaFinal()?.let { nota ->
                                    Text(
                                        text = "Nota final: ${String.format("%.1f", nota)}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                curso.getFechaArchivado()?.let { fecha ->
                                    Text(
                                        text = "Archivado: ${dateFormat.format(Date(fecha))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = when (curso.getEstado()) {
                                    EstadoCurso.APROBADO -> Icons.Default.CheckCircle
                                    EstadoCurso.REPROBADO -> Icons.Default.Cancel
                                    else -> Icons.Default.Archive
                                },
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }

            // Información del curso
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📋 Información",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Código: ${curso.getCodigo()}")
                        Text("Asistencia mínima: ${curso.getPorcentajeAsistenciaMinimo()}%")
                        Text("Nota mínima: ${curso.getNotaMinimaAprobacion()}")
                    }
                }
            }

            // Estadísticas finales
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (promedio >= curso.getNotaMinimaAprobacion())
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Promedio",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = String.format("%.2f", promedio),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (porcentajeAsistencia >= curso.getPorcentajeAsistenciaMinimo())
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Asistencia",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "${String.format("%.1f", porcentajeAsistencia)}%",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }

            // Evaluaciones
            item {
                Text(
                    text = "Evaluaciones (${evaluaciones.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(evaluaciones.sortedByDescending { it.getFecha() }) { eval ->
                EvaluacionCard(eval)
            }

            // Botón reactivar
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { mostrarDialogoReactivar = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reactivar Curso")
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ℹ️ Reactivar:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Si reactivas este curso, volverá a aparecer en tus cursos activos y podrás seguir registrando evaluaciones y asistencia.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    if (mostrarDialogoReactivar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoReactivar = false },
            title = { Text("Reactivar Curso") },
            text = {
                Text(
                    """
                    ¿Quieres reactivar este curso?
                    
                    El curso volverá a aparecer en tus cursos activos y podrás seguir registrando evaluaciones y asistencia.
                    
                    Los datos históricos se conservarán.
                    """.trimIndent()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReactivar(null)
                        mostrarDialogoReactivar = false
                    }
                ) {
                    Text("Reactivar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoReactivar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun EvaluacionCard(x0: Evaluacion) {
    TODO("Not yet implemented")
}