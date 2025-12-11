package com.academitrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.academitrack.app.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorarioSemanalScreen(
    gestorHorario: GestorHorario,
    onVolverClick: () -> Unit,
    onRegistrarHorario: () -> Unit,
    onEliminarClase: (ClaseHorario) -> Unit = {},
    onRegistrarAsistenciaDesdeHorario: (String, Long, EstadoAsistencia) -> Unit
) {
    val todasClases = gestorHorario.obtenerTodasClases()
    var mostrarDialogoEliminar by remember { mutableStateOf<ClaseHorario?>(null) }
    var claseSeleccionadaParaAsistencia by remember { mutableStateOf<ClaseHorario?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Mi Horario", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onVolverClick) { Icon(Icons.Default.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = onRegistrarHorario) {
                        Icon(Icons.Default.Add, "Agregar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (todasClases.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EventNote, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text("Sin horario configurado", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onRegistrarHorario, modifier = Modifier.padding(top = 16.dp)) { Text("Crear Horario") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                DiaSemana.values().forEach { dia ->
                    val clasesDia = gestorHorario.obtenerClasesPorDia(dia)
                    if (clasesDia.isNotEmpty()) {
                        item {
                            Text(
                                text = dia.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(clasesDia) { clase ->
                            ClaseItemModerno(
                                clase = clase,
                                onClick = { claseSeleccionadaParaAsistencia = clase },
                                onEliminar = { mostrarDialogoEliminar = clase }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    // Diálogo Eliminar
    mostrarDialogoEliminar?.let { clase ->
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = null },
            title = { Text("¿Eliminar clase?") },
            text = { Text("Se eliminará ${clase.nombreCurso} del horario.") },
            confirmButton = { Button(onClick = { onEliminarClase(clase); mostrarDialogoEliminar = null }, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { mostrarDialogoEliminar = null }) { Text("Cancelar") } }
        )
    }

    // Diálogo Asistencia
    claseSeleccionadaParaAsistencia?.let { clase ->
        AlertDialog(
            onDismissRequest = { claseSeleccionadaParaAsistencia = null },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Registrar Asistencia") },
            text = { Text("¿Asististe a la clase de ${clase.nombreCurso}?") },
            confirmButton = { Button(onClick = { onRegistrarAsistenciaDesdeHorario(clase.idCurso, System.currentTimeMillis(), EstadoAsistencia.PRESENTE); claseSeleccionadaParaAsistencia = null }) { Text("Sí, Asistí") } },
            dismissButton = { TextButton(onClick = { onRegistrarAsistenciaDesdeHorario(clase.idCurso, System.currentTimeMillis(), EstadoAsistencia.AUSENTE); claseSeleccionadaParaAsistencia = null }) { Text("No, Falté") } }
        )
    }
}

@Composable
fun ClaseItemModerno(clase: ClaseHorario, onClick: () -> Unit, onEliminar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Columna Hora
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(50.dp)
        ) {
            Text(clase.horaInicio, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.height(16.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Text(clase.horaFin, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.width(12.dp))

        // Bloque de Color y Contenido
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(android.graphics.Color.parseColor(clase.color)).copy(alpha = 0.15f))
                .padding(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(clase.nombreCurso, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Room, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(clase.sala, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onEliminar, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}