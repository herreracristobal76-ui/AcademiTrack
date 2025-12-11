@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.academitrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.academitrack.app.domain.*
import com.academitrack.app.services.*

@Composable
fun DetalleCursoScreen(
    curso: Curso,
    gestorNotas: GestorNotas,
    gestorAsistencia: GestorAsistencia,
    onVolverClick: () -> Unit,
    onAgregarNota: () -> Unit,
    onAgregarConIA: () -> Unit,
    onVerCalendario: () -> Unit,
    onEditarCurso: () -> Unit,
    onEliminarCurso: () -> Unit,
    onArchivarCurso: () -> Unit,
    onEliminarEvaluacion: (Evaluacion) -> Unit,
    onColorChanged: (String) -> Unit // NUEVO CALLBACK
) {
    var mostrarMenu by remember { mutableStateOf(false) }
    var mostrarDialogoEliminarCurso by remember { mutableStateOf(false) }
    var mostrarDialogoColor by remember { mutableStateOf(false) }

    var evaluacionAEliminar by remember { mutableStateOf<Evaluacion?>(null) }

    val promedio = gestorNotas.calcularPromedioActual(curso.getId())
    val puntosAcumulados = gestorNotas.calcularPuntosAcumulados(curso.getId())
    val porcentajeEvaluado = gestorNotas.calcularPorcentajeEvaluado(curso.getId())
    val porcentajeTotalPlanificado = gestorNotas.calcularPorcentajeTotal(curso.getId())

    val porcentajeAsistencia = gestorAsistencia.calcularPorcentajeAsistencia(curso.getId())
    val evaluaciones = gestorNotas.obtenerEvaluacionesPorCurso(curso.getId())
    val proyeccion = gestorNotas.calcularNotaNecesaria(curso.getId(), curso.getNotaMinimaAprobacion(), curso)

    // Usar el color del curso
    val colorCurso = try { Color(android.graphics.Color.parseColor(curso.getColor())) } catch(e: Exception) { MaterialTheme.colorScheme.primary }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(curso.getNombre(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(curso.getCodigo(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = { mostrarMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Opciones")
                    }
                    DropdownMenu(
                        expanded = mostrarMenu,
                        onDismissRequest = { mostrarMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("🎨 Cambiar Color") },
                            onClick = { mostrarMenu = false; mostrarDialogoColor = true },
                            leadingIcon = { Icon(Icons.Outlined.Palette, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            onClick = { mostrarMenu = false; onEditarCurso() },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Archivar") },
                            onClick = { mostrarMenu = false; onArchivarCurso() },
                            leadingIcon = { Icon(Icons.Outlined.Archive, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar Curso", color = MaterialTheme.colorScheme.error) },
                            onClick = { mostrarMenu = false; mostrarDialogoEliminarCurso = true },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {

            if (porcentajeTotalPlanificado > 100.0) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(16.dp))
                            Text("Suma total: ${porcentajeTotalPlanificado.toInt()}%. El límite es 100%.", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // Resumen
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        title = "Promedio Actual",
                        value = String.format("%.1f", promedio),
                        subtext = "${String.format("%.1f", puntosAcumulados)} pts (${porcentajeEvaluado.toInt()}%)",
                        color = if(promedio >= 4.0) colorCurso else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Asistencia",
                        value = "${String.format("%.0f", porcentajeAsistencia)}%",
                        subtext = "Meta: ${curso.getPorcentajeAsistenciaMinimo().toInt()}%",
                        color = if(porcentajeAsistencia >= curso.getPorcentajeAsistenciaMinimo()) colorCurso else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        onClick = onVerCalendario
                    )
                }
            }

            // Proyección
            if (proyeccion.porcentajeRestante > 0) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Flag, null, tint = colorCurso, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Meta de Aprobación", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(proyeccion.mensaje, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))
                            val progress = try { (100 - proyeccion.porcentajeRestante).toFloat() / 100f } catch (e: Exception) { 0f }
                            LinearProgressIndicator(
                                progress = progress.coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                color = colorCurso
                            )
                        }
                    }
                }
            }

            // Botones
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onAgregarConIA,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorCurso)
                    ) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Escanear")
                    }
                    OutlinedButton(
                        onClick = onAgregarNota,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Manual")
                    }
                }
            }

            item {
                Text("Historial de Notas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }

            if (evaluaciones.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No hay notas registradas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(evaluaciones.sortedByDescending { it.getFecha() }) { eval ->
                    EvaluacionItem(
                        eval = eval,
                        onEliminar = { evaluacionAEliminar = eval },
                        colorCurso = colorCurso
                    )
                }
            }
        }
    }

    if (mostrarDialogoEliminarCurso) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminarCurso = false },
            title = { Text("Eliminar Curso") },
            text = { Text("¿Seguro que deseas eliminar este curso y todos sus datos?") },
            confirmButton = { Button(onClick = { onEliminarCurso(); mostrarDialogoEliminarCurso = false }, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { mostrarDialogoEliminarCurso = false }) { Text("Cancelar") } }
        )
    }

    evaluacionAEliminar?.let { eval ->
        AlertDialog(
            onDismissRequest = { evaluacionAEliminar = null },
            title = { Text("Borrar Nota") },
            text = { Text("¿Eliminar ${eval.getNombre()} (${eval.getPorcentaje()}%)?") },
            confirmButton = {
                Button(
                    onClick = {
                        onEliminarEvaluacion(eval)
                        evaluacionAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
                ) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { evaluacionAEliminar = null }) { Text("Cancelar") } }
        )
    }

    // Diálogo Selector de Color
    if (mostrarDialogoColor) {
        ColorPickerDialog(
            onDismiss = { mostrarDialogoColor = false },
            onColorSelected = { colorHex ->
                onColorChanged(colorHex)
                mostrarDialogoColor = false
            }
        )
    }
}

@Composable
fun ColorPickerDialog(onDismiss: () -> Unit, onColorSelected: (String) -> Unit) {
    val colores = listOf(
        "#4F46E5", // Indigo (Default)
        "#EF4444", // Rojo
        "#F59E0B", // Ámbar
        "#10B981", // Verde
        "#3B82F6", // Azul
        "#8B5CF6", // Violeta
        "#EC4899", // Rosa
        "#14B8A6", // Teal
        "#6366F1", // Indigo Claro
        "#F97316"  // Naranja
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Elige un color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(colores) { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colorHex)))
                                .clickable { onColorSelected(colorHex) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatCard(title: String, value: String, subtext: String, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Card(
        onClick = { onClick?.invoke() }, enabled = onClick != null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp), modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                Text(subtext, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.9f))
            }
        }
    }
}

@Composable
fun EvaluacionItem(
    eval: Evaluacion,
    onEliminar: () -> Unit,
    colorCurso: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(eval.getNombre(), fontWeight = FontWeight.SemiBold)
            Text("Vale ${eval.getPorcentaje()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (eval.notaObtenida != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (eval.notaObtenida!! >= 4.0) colorCurso.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        String.format("%.1f", eval.notaObtenida),
                        fontWeight = FontWeight.Bold,
                        color = if (eval.notaObtenida!! >= 4.0) colorCurso else MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Text("Pendiente", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.width(12.dp))

            IconButton(
                onClick = onEliminar,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Borrar nota",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}