@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.academitrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.academitrack.app.domain.Curso

@Composable
fun CursosScreen(
    cursos: List<Curso>,
    onCursoClick: (Curso) -> Unit,
    onAgregarCurso: () -> Unit,
    onAjustes: () -> Unit,
    onVerArchivados: () -> Unit,
    onEliminarCurso: (Curso) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Hola, Estudiante 👋",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = onVerArchivados) {
                        Icon(Icons.Outlined.Archive, "Archivados", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    // ELIMINADO: Botón Calendario (ahora está en el BottomBar)
                    IconButton(onClick = onAjustes) {
                        Icon(Icons.Outlined.Settings, "Ajustes", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAgregarCurso,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Nuevo Curso", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "Tus Cursos Activos",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            if (cursos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.School,
                            null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Sin cursos aún",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Agrega uno para comenzar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp) // Espacio extra para el BottomBar
                ) {
                    items(cursos) { curso ->
                        CursoCardMinimal(
                            curso = curso,
                            onClick = { onCursoClick(curso) },
                            onEliminar = { onEliminarCurso(curso) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CursoCardMinimal(
    curso: Curso,
    onClick: () -> Unit,
    onEliminar: () -> Unit
) {
    var mostrarDialogo by remember { mutableStateOf(false) }

    val colorBase = try {
        Color(android.graphics.Color.parseColor(curso.getColor()))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val colorFondoPastel = lerp(Color.White, colorBase, 0.15f)
    val colorContenidoNeutro = Color(0xFF1E293B)

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = colorFondoPastel,
            contentColor = colorContenidoNeutro
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = curso.getNombre().take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorBase
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = curso.getNombre(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = curso.getCodigo().uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = colorContenidoNeutro.copy(alpha = 0.7f)
                        )
                    }
                }

                IconButton(
                    onClick = { mostrarDialogo = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Eliminar",
                        tint = colorContenidoNeutro.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider(color = colorContenidoNeutro.copy(alpha = 0.1f))
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoPill(
                    icon = Icons.Outlined.Assignment,
                    text = "${curso.getEvaluaciones().size} Notas",
                    tint = colorContenidoNeutro
                )
                InfoPill(
                    icon = Icons.Outlined.Person,
                    text = "${curso.getPorcentajeAsistenciaMinimo().toInt()}% Mín.",
                    tint = colorContenidoNeutro
                )
            }
        }
    }

    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("¿Eliminar curso?") },
            text = { Text("Se perderán todas las notas y asistencias.") },
            confirmButton = {
                Button(
                    onClick = { onEliminar(); mostrarDialogo = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun InfoPill(
    icon: ImageVector,
    text: String,
    tint: Color
) {
    Surface(
        color = Color.White.copy(alpha = 0.7f),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = tint
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = tint
            )
        }
    }
}