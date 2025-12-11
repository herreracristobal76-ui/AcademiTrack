package com.academitrack.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.academitrack.app.domain.Curso

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarCursoScreen(
    curso: Curso,
    onVolverClick: () -> Unit,
    onGuardar: (Curso) -> Unit
) {
    var nombre by remember { mutableStateOf(curso.getNombre()) }
    var codigo by remember { mutableStateOf(curso.getCodigo()) }
    var asistenciaMinima by remember { mutableStateOf(curso.getPorcentajeAsistenciaMinimo().toString()) }
    var notaMinima by remember { mutableStateOf(curso.getNotaMinimaAprobacion().toString()) }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del curso") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = codigo,
                onValueChange = { codigo = it },
                label = { Text("Código del curso") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = asistenciaMinima,
                onValueChange = { asistenciaMinima = it },
                label = { Text("Porcentaje de asistencia mínimo (%)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notaMinima,
                onValueChange = { notaMinima = it },
                label = { Text("Nota mínima de aprobación (1.0 - 7.0)") },
                modifier = Modifier.fillMaxWidth()
            )

            if (showError) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "⚠️ Por favor completa todos los campos correctamente",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Button(
                onClick = {
                    val asist = asistenciaMinima.toDoubleOrNull()
                    val notaMin = notaMinima.toDoubleOrNull()

                    if (nombre.isNotBlank() &&
                        codigo.isNotBlank() &&
                        asist != null && asist in 0.0..100.0 &&
                        notaMin != null && notaMin in 1.0..7.0) {

                        val cursoEditado = Curso(
                            idCurso = curso.getId(),
                            nombre = nombre,
                            codigo = codigo,
                            porcentajeAsistenciaMinimo = asist,
                            notaMinimaAprobacion = notaMin,
                            evaluaciones = curso.getEvaluaciones().toMutableList(),
                            asistencias = curso.getAsistencias().toMutableList()
                        )

                        onGuardar(cursoEditado)
                        showError = false
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("💾 Guardar Cambios")
            }

            Spacer(modifier = Modifier.weight(1f))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "ℹ️ Información:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "• Los cambios no afectarán las evaluaciones y asistencias ya registradas",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}