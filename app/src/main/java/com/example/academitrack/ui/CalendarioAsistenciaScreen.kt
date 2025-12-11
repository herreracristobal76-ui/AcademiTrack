package com.academitrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.academitrack.app.domain.*
import com.academitrack.app.services.GestorAsistencia
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioAsistenciaScreen(
    curso: Curso,
    gestorAsistencia: GestorAsistencia,
    onVolverClick: () -> Unit,
    onRegistrarAsistencia: (Long, EstadoAsistencia) -> Unit
) {
    var mostrarDialogoRegistro by remember { mutableStateOf(false) }
    var fechaSeleccionada by remember { mutableStateOf(System.currentTimeMillis()) }

    val asistencias = gestorAsistencia.obtenerAsistenciasPorCurso(curso.getId())
    val stats = gestorAsistencia.obtenerEstadisticas(curso.getId())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario de Asistencia") },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                fechaSeleccionada = System.currentTimeMillis()
                mostrarDialogoRegistro = true
            }) {
                Text("➕", style = MaterialTheme.typography.titleLarge)
            }
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
                        containerColor = if (stats.porcentajeAsistencia >= curso.getPorcentajeAsistenciaMinimo())
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📊 Resumen de Asistencia",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${String.format("%.1f", stats.porcentajeAsistencia)}%",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text("${stats.clasesAsistidas} de ${stats.totalClases} clases")
                        if (stats.faltas > 0) {
                            Text(
                                text = "🔴 ${stats.faltas} faltas",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LeyendaItem("✅", "Presente", Color(0xFF4CAF50))
                        LeyendaItem("❌", "Ausente", Color(0xFFF44336))
                        LeyendaItem("📝", "Justificado", Color(0xFF2196F3))
                        LeyendaItem("⚠️", "Cancelada", Color(0xFFFFC107))
                    }
                }
            }

            item {
                Text(
                    text = "Historial (${asistencias.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (asistencias.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📅 No hay registros de asistencia")
                            Text(
                                "Presiona + para registrar",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                items(asistencias) { asistencia ->
                    AsistenciaCard(asistencia)
                }
            }
        }
    }

    if (mostrarDialogoRegistro) {
        DialogoRegistrarAsistencia(
            fecha = fechaSeleccionada,
            onDismiss = { mostrarDialogoRegistro = false },
            onConfirm = { estado ->
                onRegistrarAsistencia(fechaSeleccionada, estado)
                mostrarDialogoRegistro = false
            }
        )
    }
}

@Composable
fun LeyendaItem(emoji: String, texto: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
        Text(texto, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun AsistenciaCard(asistencia: Asistencia) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val fecha = dateFormat.format(Date(asistencia.getFecha()))

    val (emoji, color) = when (asistencia.getEstado()) {
        EstadoAsistencia.PRESENTE -> "✅" to Color(0xFF4CAF50)
        EstadoAsistencia.AUSENTE -> "❌" to Color(0xFFF44336)
        EstadoAsistencia.JUSTIFICADO -> "📝" to Color(0xFF2196F3)
        EstadoAsistencia.CLASE_CANCELADA -> "⚠️" to Color(0xFFFFC107)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Column {
                    Text(
                        text = asistencia.getEstado().descripcion,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = fecha,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun DialogoRegistrarAsistencia(
    fecha: Long,
    onDismiss: () -> Unit,
    onConfirm: (EstadoAsistencia) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val fechaStr = dateFormat.format(Date(fecha))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Asistencia") },
        text = {
            Column {
                Text("Fecha: $fechaStr")
                Spacer(modifier = Modifier.height(16.dp))
                Text("Selecciona el estado:")
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onConfirm(EstadoAsistencia.PRESENTE) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✅ Presente")
                }
                Button(
                    onClick = { onConfirm(EstadoAsistencia.AUSENTE) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("❌ Ausente")
                }
                OutlinedButton(
                    onClick = { onConfirm(EstadoAsistencia.JUSTIFICADO) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📝 Justificado")
                }
                OutlinedButton(
                    onClick = { onConfirm(EstadoAsistencia.CLASE_CANCELADA) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⚠️ Clase Cancelada")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}