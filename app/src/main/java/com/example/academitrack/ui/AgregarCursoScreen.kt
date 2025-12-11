package com.academitrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.academitrack.app.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarCursoScreen(
    onVolverClick: () -> Unit,
    onGuardar: (Curso, List<ClaseHorario>) -> Unit // Modificado para devolver horarios
) {
    var nombre by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var asistenciaMinima by remember { mutableStateOf("75") }
    var notaMinima by remember { mutableStateOf("4.0") }

    // Lista temporal de horarios que vamos agregando
    val horariosAgregados = remember { mutableStateListOf<ClaseHorario>() }
    var mostrarDialogoHorario by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Curso") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección Datos Básicos
            item {
                Text("Datos del Curso", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }

            item {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    placeholder = { Text("Ej: Cálculo I") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it },
                    label = { Text("Código") },
                    placeholder = { Text("Ej: MAT-101") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = asistenciaMinima,
                        onValueChange = { asistenciaMinima = it },
                        label = { Text("Asist. Mín (%)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = notaMinima,
                        onValueChange = { notaMinima = it },
                        label = { Text("Nota Mín") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            // Sección Horarios
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Horarios de Clase", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { mostrarDialogoHorario = true }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Agregar")
                    }
                }
            }

            if (horariosAgregados.isEmpty()) {
                item {
                    Text(
                        "No hay horarios definidos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            } else {
                items(horariosAgregados) { horario ->
                    HorarioItem(horario) { horariosAgregados.remove(horario) }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                if (showError) {
                    Text(
                        "⚠️ Completa todos los campos obligatorios",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        val asist = asistenciaMinima.toDoubleOrNull()
                        val notaMin = notaMinima.toDoubleOrNull()

                        if (nombre.isNotBlank() && codigo.isNotBlank() &&
                            asist != null && notaMin != null) {

                            val nuevoCurso = Curso(
                                idCurso = "curso_${System.currentTimeMillis()}",
                                nombre = nombre,
                                codigo = codigo,
                                porcentajeAsistenciaMinimo = asist,
                                notaMinimaAprobacion = notaMin
                            )

                            // Asignar el ID del curso a los horarios creados
                            val horariosFinales = horariosAgregados.map {
                                it.copy(
                                    idCurso = nuevoCurso.getId(),
                                    nombreCurso = nuevoCurso.getNombre(),
                                    color = generarColor(nuevoCurso.getNombre()) // Color consistente
                                )
                            }

                            onGuardar(nuevoCurso, horariosFinales)
                            showError = false
                        } else {
                            showError = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Guardar Curso")
                }
            }
        }
    }

    if (mostrarDialogoHorario) {
        DialogAgregarHorario(
            onDismiss = { mostrarDialogoHorario = false },
            onConfirm = { nuevoHorario ->
                horariosAgregados.add(nuevoHorario)
                mostrarDialogoHorario = false
            }
        )
    }
}

@Composable
fun HorarioItem(horario: ClaseHorario, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "${horario.diaSemana.nombre} ${horario.horaInicio} - ${horario.horaFin}",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${horario.sala} • ${horario.tipoClase.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogAgregarHorario(onDismiss: () -> Unit, onConfirm: (ClaseHorario) -> Unit) {
    var diaSeleccionado by remember { mutableStateOf(DiaSemana.LUNES) }
    var horaInicio by remember { mutableStateOf("") }
    var horaFin by remember { mutableStateOf("") }
    var sala by remember { mutableStateOf("") }
    var expandedDia by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Agregar Horario", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                // Selector de Día
                ExposedDropdownMenuBox(expanded = expandedDia, onExpandedChange = { expandedDia = !expandedDia }) {
                    OutlinedTextField(
                        value = diaSeleccionado.nombre,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Día") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDia) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedDia, onDismissRequest = { expandedDia = false }) {
                        DiaSemana.values().forEach { dia ->
                            DropdownMenuItem(
                                text = { Text(dia.nombre) },
                                onClick = { diaSeleccionado = dia; expandedDia = false }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = horaInicio,
                        onValueChange = { horaInicio = it },
                        label = { Text("Inicio (HH:MM)") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("08:30") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = horaFin,
                        onValueChange = { horaFin = it },
                        label = { Text("Fin (HH:MM)") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("10:00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = sala,
                    onValueChange = { sala = it },
                    label = { Text("Sala (Opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (horaInicio.isNotBlank() && horaFin.isNotBlank()) {
                                onConfirm(
                                    ClaseHorario(
                                        id = "clase_temp_${System.currentTimeMillis()}",
                                        idCurso = "", // Se asigna al guardar el curso
                                        nombreCurso = "",
                                        sala = sala.ifBlank { "Sin sala" },
                                        profesor = "",
                                        diaSemana = diaSeleccionado,
                                        horaInicio = horaInicio,
                                        horaFin = horaFin,
                                        tipoClase = TipoClase.CATEDRA,
                                        color = "#FFFFFF"
                                    )
                                )
                            }
                        }
                    ) { Text("Agregar") }
                }
            }
        }
    }
}

// Función auxiliar para color (copia simple de la que tienes en el servicio)
fun generarColor(nombre: String): String {
    val colores = listOf("#6200EE", "#03DAC6", "#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A", "#98D8C8")
    return colores[Math.abs(nombre.hashCode()) % colores.size]
}