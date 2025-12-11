package com.academitrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.academitrack.app.domain.*

/**
 * Pantalla de Horario Semanal
 *
 * UBICACIÓN: app/src/main/java/com/academitrack/app/ui/HorarioSemanalScreen.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorarioSemanalScreen(
    gestorHorario: GestorHorario,
    onVolverClick: () -> Unit,
    onRegistrarHorario: () -> Unit
) {
    val todasClases = gestorHorario.obtenerTodasClases()
    val proximaClase = gestorHorario.obtenerProximaClase()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Horario Semanal") },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onRegistrarHorario) {
                        Icon(Icons.Default.Add, "Registrar Horario")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (todasClases.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No hay horarios registrados",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Registra tu horario para ver tus clases aquí",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onRegistrarHorario) {
                    Icon(Icons.Default.PhotoCamera, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Registrar con Foto")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Próxima clase
                proximaClase?.let { clase ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "📍 Próxima Clase",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = clase.nombreCurso,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "${clase.diaSemana.nombre} • ${clase.obtenerHorarioCompleto()}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${clase.sala} • ${clase.profesor}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Horario por día
                item {
                    Text(
                        text = "Clases de la Semana",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                DiaSemana.values().forEach { dia ->
                    val clasesDia = gestorHorario.obtenerClasesPorDia(dia)
                    if (clasesDia.isNotEmpty()) {
                        item {
                            Text(
                                text = dia.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        items(clasesDia) { clase ->
                            ClaseHorarioCard(clase)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClaseHorarioCard(clase: ClaseHorario) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(clase.color))
                .copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Barra de color
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .background(
                        Color(android.graphics.Color.parseColor(clase.color)),
                        shape = MaterialTheme.shapes.small
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = clase.nombreCurso,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = clase.tipoClase.descripcion,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = clase.obtenerHorarioCompleto(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Room,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = clase.sala,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = clase.profesor,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}