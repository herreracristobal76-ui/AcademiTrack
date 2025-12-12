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

// SOLUCIÓN: La función String.hasMaxOneDecimal() fue movida a AgregarNotaScreen.kt
// para evitar duplicidad, ya que ambas están en el mismo paquete.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarNotaScreen(
    evaluacion: Evaluacion,
    maxPorcentajeDisponible: Double, // Límite real permitido (100 - %otras evaluaciones)
    onVolverClick: () -> Unit,
    onGuardar: (Evaluacion) -> Unit
) {
    // Inicializar estados con los valores de la evaluación
    var nombre by remember { mutableStateOf(evaluacion.getNombre()) }
    // Inicializar porcentaje como String sin decimales
    var porcentaje by remember { mutableStateOf(evaluacion.getPorcentaje().toInt().toString()) }
    // Inicializar nota con un decimal (o vacía)
    var nota by remember { mutableStateOf(evaluacion.notaObtenida?.let { String.format("%.1f", it) } ?: "") }

    // El porcentaje original de esta evaluación, que debe sumarse al máximo disponible
    val porcentajeOriginal = evaluacion.getPorcentaje()
    val maxReal = maxPorcentajeDisponible + porcentajeOriginal
    val maxRealInt = maxReal.toInt()

    // Validaciones en tiempo real
    val porcentajeIntVal = porcentaje.toIntOrNull()

    // Es inválido si no es entero, si es mayor que el total disponible o si es <= 0
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

            // Campo Porcentaje con Validación
            OutlinedTextField(
                value = porcentaje,
                onValueChange = {
                    // Solo permitir números naturales (dígitos)
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
                    // Permite números, punto y valida un máximo de un decimal.
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
                    // Convertir el porcentaje de Int a Double
                    val porc = porcentajeIntVal?.toDouble()
                    val notaVal = nota.toDoubleOrNull()

                    // Validación de formato de nota (rango y decimales)
                    val esNotaFinalValida = notaVal != null && notaVal in 1.0..7.0 && nota.hasMaxOneDecimal()

                    // Validación Final antes de guardar
                    if (nombre.isNotBlank() &&
                        porc != null && porc > 0 && mensajeErrorPorcentaje == null &&
                        esNotaFinalValida) {

                        // Actualizar los campos mutables de la evaluación
                        evaluacion.setNombre(nombre)
                        evaluacion.setPorcentaje(porc)
                        evaluacion.setNotaObtenida(notaVal!!)

                        // Si es Evaluación Manual, actualizamos sus campos específicos si es necesario
                        if (evaluacion is EvaluacionManual) {
                            // No hay cambios necesarios aquí.
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