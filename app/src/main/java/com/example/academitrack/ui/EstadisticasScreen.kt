package com.academitrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.academitrack.app.domain.Curso
import com.academitrack.app.services.GestorAsistencia
import com.academitrack.app.services.GestorNotas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasScreen(
    cursos: List<Curso>,
    gestorNotas: GestorNotas,
    gestorAsistencia: GestorAsistencia
) {
    // Cálculos Globales
    val cursosActivos = cursos.filter { it.estaActivo() }

    val promedioGlobal = if (cursosActivos.isNotEmpty()) {
        cursosActivos.map { gestorNotas.calcularPromedioActual(it.getId()) }
            .filter { it > 0 }
            .average()
    } else 0.0

    val asistenciaGlobal = if (cursosActivos.isNotEmpty()) {
        cursosActivos.map { gestorAsistencia.calcularPorcentajeAsistencia(it.getId()) }
            .average()
    } else 0.0

    // Contadores
    val cursosAprobando = cursosActivos.count { gestorNotas.calcularPromedioActual(it.getId()) >= 4.0 }
    val cursosReprobando = cursosActivos.count {
        val p = gestorNotas.calcularPromedioActual(it.getId())
        p > 0 && p < 4.0
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Mi Rendimiento", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. Tarjetas Resumen Global
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Tarjeta Promedio Global
                    StatCardLarge(
                        title = "PGA Actual",
                        value = if (promedioGlobal.isNaN()) "0.0" else String.format("%.1f", promedioGlobal),
                        icon = Icons.Outlined.School,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    // Tarjeta Asistencia Global
                    StatCardLarge(
                        title = "Asistencia",
                        value = "${String.format("%.0f", asistenciaGlobal)}%",
                        icon = Icons.Outlined.AccessTime,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Estado de Cursos (Gráfico de Barras simulado)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Resumen de Notas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))

                        if (cursosActivos.isEmpty()) {
                            Text("No hay datos suficientes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            cursosActivos.forEach { curso ->
                                val nota = gestorNotas.calcularPromedioActual(curso.getId())
                                val colorBarra = try { Color(android.graphics.Color.parseColor(curso.getColor())) } catch (e: Exception) { MaterialTheme.colorScheme.primary }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        curso.getCodigo(),
                                        modifier = Modifier.width(60.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Barra de progreso
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth((nota / 7.0).toFloat().coerceIn(0f, 1f)) // Escala 1.0 - 7.0
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colorBarra)
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        String.format("%.1f", nota),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if(nota >= 4.0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Resumen Rápido
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ResumenItem(cursosAprobando.toString(), "Aprobando", Color(0xFF10B981))
                    ResumenItem(cursosReprobando.toString(), "En riesgo", Color(0xFFEF4444))
                    ResumenItem(cursosActivos.size.toString(), "Total", MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun StatCardLarge(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ResumenItem(numero: String, etiqueta: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(numero, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}