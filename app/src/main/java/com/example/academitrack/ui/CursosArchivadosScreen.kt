package com.academitrack.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.academitrack.app.domain.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla para ver cursos archivados
 * UBICACIÓN: app/src/main/java/com/academitrack/app/ui/CursosArchivadosScreen.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CursosArchivadosScreen(
    cursos: List<Curso>,
    onVolverClick: () -> Unit,
    onCursoClick: (Curso) -> Unit
) {
    val cursosArchivados = remember(cursos) {
        cursos.filter { it.estaArchivado() }
            .sortedByDescending { it.getFechaArchivado() ?: 0 }
    }

    val aprobados = cursosArchivados.filter { it.getEstado() == EstadoCurso.APROBADO }
    val reprobados = cursosArchivados.filter { it.getEstado() == EstadoCurso.REPROBADO }
    val retirados = cursosArchivados.filter { it.getEstado() == EstadoCurso.RETIRADO }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cursos Archivados") },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (cursosArchivados.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No hay cursos archivados",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Los cursos aprobados o reprobados aparecerán aquí",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Estadísticas
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EstadisticaCard(
                            icono = Icons.Default.CheckCircle,
                            titulo = "Aprobados",
                            cantidad = aprobados.size,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        EstadisticaCard(
                            icono = Icons.Default.Cancel,
                            titulo = "Reprobados",
                            cantidad = reprobados.size,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        EstadisticaCard(
                            icono = Icons.Default.ExitToApp,
                            titulo = "Retirados",
                            cantidad = retirados.size,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Aprobados
                if (aprobados.isNotEmpty()) {
                    item {
                        Text(
                            text = "✅ Aprobados (${aprobados.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(aprobados) { curso ->
                        CursoArchivadoCard(curso, onClick = { onCursoClick(curso) })
                    }
                }

                // Reprobados
                if (reprobados.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "❌ Reprobados (${reprobados.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    items(reprobados) { curso ->
                        CursoArchivadoCard(curso, onClick = { onCursoClick(curso) })
                    }
                }

                // Retirados
                if (retirados.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🚪 Retirados (${retirados.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    items(retirados) { curso ->
                        CursoArchivadoCard(curso, onClick = { onCursoClick(curso) })
                    }
                }
            }
        }
    }
}

@Composable
fun EstadisticaCard(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    cantidad: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icono, null, modifier = Modifier.size(24.dp))
            Text(
                text = cantidad.toString(),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun CursoArchivadoCard(
    curso: Curso,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val colorFondo = when (curso.getEstado()) {
        EstadoCurso.APROBADO -> MaterialTheme.colorScheme.primaryContainer
        EstadoCurso.REPROBADO -> MaterialTheme.colorScheme.errorContainer
        EstadoCurso.RETIRADO -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = colorFondo)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = curso.getNombre(),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = curso.getCodigo(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = curso.getEstado().descripcion,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    curso.getNotaFinal()?.let { nota ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Nota: ${String.format("%.1f", nota)}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                curso.getFechaArchivado()?.let { fecha ->
                    Text(
                        text = "Archivado: ${dateFormat.format(Date(fecha))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ver detalles",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}