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
fun AgregarCursoScreen(
    onVolverClick: () -> Unit,
    onGuardar: (Curso) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var asistenciaMinima by remember { mutableStateOf("75") }
    var notaMinima by remember { mutableStateOf("4.0") }
    var showError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agregar Curso") },
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
                placeholder = { Text("Ej: Programación Avanzada") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = codigo,
                onValueChange = { codigo = it },
                label = { Text("Código del curso") },
                placeholder = { Text("Ej: INF-301") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = asistenciaMinima,
                onValueChange = { asistenciaMinima = it },
                label = { Text("Porcentaje de asistencia mínimo (%)") },
                placeholder = { Text("75") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notaMinima,
                onValueChange = { notaMinima = it },
                label = { Text("Nota mínima de aprobación (1.0 - 7.0)") },
                placeholder = { Text("4.0") },
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

                        val curso = Curso(
                            idCurso = "curso_${System.currentTimeMillis()}",
                            nombre = nombre,
                            codigo = codigo,
                            porcentajeAsistenciaMinimo = asist,
                            notaMinimaAprobacion = notaMin
                        )

                        onGuardar(curso)
                        showError = false
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("💾 Guardar Curso")
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
                        text = "💡 Información:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "• La escala de notas es chilena (1.0 - 7.0)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Puedes editar todos estos datos después",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}