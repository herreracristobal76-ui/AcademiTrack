@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.academitrack.app.ui

import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.academitrack.app.domain.Curso


@Composable
fun CursosScreen(
    cursos: List<Curso>,
    onCursoClick: (Curso) -> Unit,
    onAgregarCurso: () -> Unit,
    onAjustes: () -> Unit,
    onVerCalendario: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Cursos") },
                actions = {
                    IconButton(onClick = onAjustes) {
                        Icon(Icons.Default.Settings, "Ajustes")
                    }
                    IconButton(onClick = onVerCalendario) {
                        Icon(Icons.Filled.CalendarMonth, "Ver Calendario Mensual")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAgregarCurso) {
                Icon(Icons.Default.Add, "Agregar Curso")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (cursos.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📚 No tienes cursos",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Presiona el botón + para agregar tu primer curso",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cursos) { curso ->
                        CursoCard(
                            curso = curso,
                            onClick = { onCursoClick(curso) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CursoCard(
    curso: Curso,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = curso.getNombre(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = curso.getCodigo(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${curso.getEvaluaciones().size} evaluaciones",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Asist. mín: ${curso.getPorcentajeAsistenciaMinimo()}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}