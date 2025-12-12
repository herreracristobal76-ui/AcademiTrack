package com.academitrack.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.academitrack.app.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarNotaScreen(
    evaluacion: Evaluacion,
    maxPorcentajeDisponible: Double,
    onVolverClick: () -> Unit,
    onGuardar: (Evaluacion) -> Unit
) {

    var nombre by remember { mutableStateOf(evaluacion.getNombre()) }

    var porcentaje by remember { mutableStateOf(evaluacion.getPorcentaje().toInt().toString()) }

    var nota by remember { mutableStateOf(evaluacion.notaObtenida?.let { String.format("%.1f", it) } ?: "") }


    val porcentajeOriginal = evaluacion.getPorcentaje()
    val maxReal = maxPorcentajeDisponible + porcentajeOriginal
    val maxRealInt = maxReal.toInt()


    val porcentajeIntVal = porcentaje.toIntOrNull()


    val esPorcentajeInvalido = porcentajeIntVal == null || porcentajeIntVal > maxRealInt || porcentajeIntVal <= 0

    val mensajeErrorPorcentaje = when {
        porcentajeIntVal == null -> "Ingresa un número entero"
        porcentajeIntVal > maxRealInt -> "Máximo permitido: $maxRealInt%"
        porcentajeIntVal <= 0 -> "El porcentaje debe ser mayor a 0%"
        else -> null
    }

    // Validación de nota: solo se valida el formato en el texto de soporte
    val esNotaInvalida = nota.toDoubleOrNull() == null || !nota.hasMaxOneDecimal()
    val notaPlaceholder = "Ej: 5.3 (Máx 1 decimal)"

    var showError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Nota") },
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
                text = "Evaluación de: ${evaluacion.obtenerTipoEvaluacion()}",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre de la evaluación") },
                modifier = Modifier.fillMaxWidth()
            )


            OutlinedTextField(
                value = porcentaje,
                onValueChange = {

                    if (it.all { char -> char.isDigit() } && it.length <= 3) {
                        porcentaje = it
                    }
                },
                label = { Text("Porcentaje (%)") },
                placeholder = { Text("Máx: $maxRealInt% (sin decimales)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = mensajeErrorPorcentaje != null,
                supportingText = {
                    if (mensajeErrorPorcentaje != null) {
                        Text(mensajeErrorPorcentaje, color = MaterialTheme.colorScheme.error)
                    } else if (porcentaje.isNotBlank()) {
                        Text("El máximo total para el curso es 100%.")
                    }
                }
            )

            OutlinedTextField(
                value = nota,
                onValueChange = {

                    val cleanInput = it.filter { char -> char.isDigit() || char == '.' }
                    if (cleanInput.count { it == '.' } <= 1 && cleanInput.hasMaxOneDecimal()) {
                        nota = cleanInput
                    }
                },
                label = { Text("Nota obtenida (1.0 - 7.0)") },
                placeholder = { Text(notaPlaceholder) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = showError && esNotaInvalida,
                supportingText = {
                    if (showError && esNotaInvalida) {
                        Text("La nota debe ser 1.0 a 7.0 con un decimal máximo (Ej: 5.3).", color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Máximo un decimal (Ej: 5.3)")
                    }
                }
            )

            if (showError) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "⚠️ Por favor verifica los datos ingresados (Nombre, Porcentaje o Nota)",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Button(
                onClick = {

                    val porc = porcentajeIntVal?.toDouble()
                    val notaVal = nota.toDoubleOrNull()


                    val esNotaFinalValida = notaVal != null && notaVal in 1.0..7.0 && nota.hasMaxOneDecimal()


                    if (nombre.isNotBlank() &&
                        porc != null && porc > 0 && mensajeErrorPorcentaje == null &&
                        esNotaFinalValida) {


                        evaluacion.setNombre(nombre)
                        evaluacion.setPorcentaje(porc)
                        evaluacion.setNotaObtenida(notaVal!!)


                        if (evaluacion is EvaluacionManual) {

                        }

                        onGuardar(evaluacion)
                        showError = false
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = mensajeErrorPorcentaje == null
            ) {
                Text("💾 Guardar Cambios")
            }
        }
    }
}