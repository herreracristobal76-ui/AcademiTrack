package com.academitrack.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.academitrack.app.domain.ClaseHorario
import com.academitrack.app.domain.Curso

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarCursoScreen(
    curso: Curso,
    horariosActuales: List<ClaseHorario>, // Recibimos los horarios actuales
    onVolverClick: () -> Unit,
    onGuardar: (Curso, List<ClaseHorario>) -> Unit
) {
    var nombre by remember { mutableStateOf(curso.getNombre()) }
    var codigo by remember { mutableStateOf(curso.getCodigo()) }
    var asistenciaMinima by remember { mutableStateOf(curso.getPorcentajeAsistenciaMinimo().toString()) }
    var notaMinima by remember { mutableStateOf(curso.getNotaMinimaAprobacion().toString()) }

    // Estado para los horarios (iniciamos con los actuales)
    val listaHorarios = remember { mutableStateListOf<ClaseHorario>().apply { addAll(horariosActuales) } }
    var mostrarDialogoHorario by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Curso") },
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
            item {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del curso") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it },
                    label = { Text("Código del curso") },
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
                    Text("Horarios", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { mostrarDialogoHorario = true }) {
                        Icon(Icons.Default.Add, null)
                        Text("Agregar")
                    }
                }
            }

            items(listaHorarios) { horario ->
                HorarioItem(horario) { listaHorarios.remove(horario) }
            }

            item {
                Spacer(Modifier.height(16.dp))
                if (showError) {
                    Text("⚠️ Verifica los datos", color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        val asist = asistenciaMinima.toDoubleOrNull()
                        val notaMin = notaMinima.toDoubleOrNull()

                        if (nombre.isNotBlank() && codigo.isNotBlank() && asist != null && notaMin != null) {

                            val cursoEditado = Curso(
                                idCurso = curso.getId(),
                                nombre = nombre,
                                codigo = codigo,
                                porcentajeAsistenciaMinimo = asist,
                                notaMinimaAprobacion = notaMin,
                                evaluaciones = curso.getEvaluaciones().toMutableList(),
                                asistencias = curso.getAsistencias().toMutableList()
                            )

                            // Actualizar info en horarios (por si cambió el nombre del curso)
                            val horariosFinales = listaHorarios.map {
                                it.copy(
                                    idCurso = curso.getId(),
                                    nombreCurso = nombre,
                                    // Mantener ID si ya existe, o crear uno nuevo si es agregado recién
                                    id = if(it.id.startsWith("clase_temp")) "clase_${System.currentTimeMillis()}_${Math.random()}" else it.id
                                )
                            }

                            onGuardar(cursoEditado, horariosFinales)
                            showError = false
                        } else {
                            showError = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar Cambios")
                }
            }
        }
    }

    if (mostrarDialogoHorario) {
        DialogAgregarHorario(
            onDismiss = { mostrarDialogoHorario = false },
            onConfirm = { nuevoHorario ->
                listaHorarios.add(nuevoHorario)
                mostrarDialogoHorario = false
            }
        )
    }
}