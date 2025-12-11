package com.academitrack.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.academitrack.app.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarNotaScreen(
    curso: Curso,
    onVolverClick: () -> Unit,
    onGuardar: (EvaluacionManual) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var porcentaje by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agregar Nota Manual") },
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
            Text(
                text = "Curso: ${curso.getNombre()}",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre de la evaluación") },
                placeholder = { Text("Ej: Certamen 1") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = porcentaje,
                onValueChange = { porcentaje = it },
                label = { Text("Porcentaje (%)") },
                placeholder = { Text("Ej: 30") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = nota,
                onValueChange = { nota = it },
                label = { Text("Nota obtenida (1.0 - 7.0)") },
                placeholder = { Text("Ej: 5.5") },
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
                    val porc = porcentaje.toDoubleOrNull()
                    val notaVal = nota.toDoubleOrNull()

                    if (nombre.isNotBlank() &&
                        porc != null && porc in 0.0..100.0 &&
                        notaVal != null && notaVal in 1.0..7.0) {

                        val eval = EvaluacionManual(
                            id = "eval_${System.currentTimeMillis()}",
                            nombreEval = nombre,
                            porcentajeEval = porc,
                            fechaEval = System.currentTimeMillis(),
                            idCursoEval = curso.getId()
                        )
                        eval.setNotaObtenida(notaVal)

                        onGuardar(eval)
                        showError = false
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("💾 Guardar Nota")
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
                        text = "💡 Tip:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "También puedes usar la función de IA para procesar fotos automáticamente",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}