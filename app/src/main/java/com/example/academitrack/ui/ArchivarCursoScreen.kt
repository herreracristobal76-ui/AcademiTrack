package com.academitrack.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.academitrack.app.domain.Curso
import com.academitrack.app.domain.EstadoCurso

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivarCursoScreen(
    curso: Curso,
    promedioActual: Double,
    onVolverClick: () -> Unit,
    onArchivar: (EstadoCurso, Double?) -> Unit
) {
    var estadoSeleccionado by remember { mutableStateOf<EstadoCurso?>(null) }
    var notaFinal by remember { mutableStateOf(promedioActual.toString()) }
    var mostrarError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archivar Curso") },
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = curso.getNombre(),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = curso.getCodigo(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Promedio actual: ${String.format("%.2f", promedioActual)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }


            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ℹ️ ¿Qué significa archivar?",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = """
                            Al archivar un curso:
                            • Se guardará su estado final
                            • No aparecerá en tu lista activa
                            • Podrás consultarlo en "Cursos Archivados"
                            • Conservarás todo el historial
                            • Podrás reactivarlo si lo necesitas
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }


            Text(
                text = "Selecciona el estado final del curso:",
                style = MaterialTheme.typography.titleMedium
            )


            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { estadoSeleccionado = EstadoCurso.APROBADO },
                colors = CardDefaults.cardColors(
                    containerColor = if (estadoSeleccionado == EstadoCurso.APROBADO)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "✅ Aprobado",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Aprobaste el curso exitosamente",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (estadoSeleccionado == EstadoCurso.APROBADO) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }


            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { estadoSeleccionado = EstadoCurso.REPROBADO },
                colors = CardDefaults.cardColors(
                    containerColor = if (estadoSeleccionado == EstadoCurso.REPROBADO)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "❌ Reprobado",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "No alcanzaste la nota mínima",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (estadoSeleccionado == EstadoCurso.REPROBADO) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }


            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { estadoSeleccionado = EstadoCurso.RETIRADO },
                colors = CardDefaults.cardColors(
                    containerColor = if (estadoSeleccionado == EstadoCurso.RETIRADO)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🚪 Retirado",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Te retiraste del curso",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (estadoSeleccionado == EstadoCurso.RETIRADO) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }


            if (estadoSeleccionado == EstadoCurso.APROBADO || estadoSeleccionado == EstadoCurso.REPROBADO) {
                OutlinedTextField(
                    value = notaFinal,
                    onValueChange = {
                        notaFinal = it
                        mostrarError = false
                    },
                    label = { Text("Nota final del curso (1.0 - 7.0)") },
                    placeholder = { Text(String.format("%.2f", promedioActual)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = mostrarError
                )
            }

            if (mostrarError) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "⚠️ Por favor ingresa una nota válida (1.0 - 7.0)",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))


            Button(
                onClick = {
                    if (estadoSeleccionado == null) {
                        mostrarError = true
                        return@Button
                    }

                    val nota = if (estadoSeleccionado == EstadoCurso.RETIRADO) {
                        null
                    } else {
                        val notaVal = notaFinal.toDoubleOrNull()
                        if (notaVal == null || notaVal !in 1.0..7.0) {
                            mostrarError = true
                            return@Button
                        }
                        notaVal
                    }

                    onArchivar(estadoSeleccionado!!, nota)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = estadoSeleccionado != null
            ) {
                Text("📦 Archivar Curso")
            }
        }
    }
}