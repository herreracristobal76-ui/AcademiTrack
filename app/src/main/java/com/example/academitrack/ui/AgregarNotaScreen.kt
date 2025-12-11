package com.academitrack.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.academitrack.app.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarNotaScreen(
    curso: Curso,
    maxPorcentajeDisponible: Double, // Límite para validar
    onVolverClick: () -> Unit,
    onGuardar: (EvaluacionManual) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var porcentaje by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }

    // Validaciones en tiempo real
    val porcentajeVal = porcentaje.toDoubleOrNull() ?: 0.0
    // Error si es mayor al disponible o menor que 0
    val esPorcentajeInvalido = porcentajeVal > maxPorcentajeDisponible || porcentajeVal < 0

    val mensajeErrorPorcentaje = if (porcentajeVal > maxPorcentajeDisponible) {
        "Máximo permitido: ${maxPorcentajeDisponible.toInt()}%"
    } else null

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

            // Campo Porcentaje con Validación
            OutlinedTextField(
                value = porcentaje,
                onValueChange = {
                    // Solo permitir números y punto
                    if (it.all { char -> char.isDigit() || char == '.' }) {
                        porcentaje = it
                    }
                },
                label = { Text("Porcentaje (%)") },
                placeholder = { Text("Disponible: ${maxPorcentajeDisponible.toInt()}%") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = esPorcentajeInvalido,
                supportingText = {
                    if (esPorcentajeInvalido && mensajeErrorPorcentaje != null) {
                        Text(mensajeErrorPorcentaje, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Restante disponible: ${maxPorcentajeDisponible.toInt()}%")
                    }
                }
            )

            OutlinedTextField(
                value = nota,
                onValueChange = {
                    if (it.all { char -> char.isDigit() || char == '.' }) {
                        nota = it
                    }
                },
                label = { Text("Nota obtenida (1.0 - 7.0)") },
                placeholder = { Text("Ej: 5.5") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            if (showError) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "⚠️ Por favor verifica los datos ingresados",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Button(
                onClick = {
                    val porc = porcentaje.toDoubleOrNull()
                    val notaVal = nota.toDoubleOrNull()

                    // Validación Final antes de guardar
                    if (nombre.isNotBlank() &&
                        porc != null && porc > 0 && !esPorcentajeInvalido &&
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
                modifier = Modifier.fillMaxWidth(),
                enabled = !esPorcentajeInvalido // Bloquear botón si hay error
            ) {
                Text("💾 Guardar Nota")
            }
        }
    }
}