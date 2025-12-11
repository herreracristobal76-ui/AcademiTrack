package com.academitrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.academitrack.app.domain.*
import com.academitrack.app.services.GestorAsistencia
import java.text.SimpleDateFormat
import java.util.*


/**
 * Pantalla de Calendario Mensual con vista de clases
 *
 * UBICACIÓN: app/src/main/java/com/academitrack/app/ui/CalendarioMensualScreen.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioMensualScreen(
    gestorHorario: GestorHorario,
    gestorAsistencia: GestorAsistencia,
    cursos: List<Curso>,
    onVolverClick: () -> Unit,
    onRegistrarHorario: () -> Unit,
    onVerHorarioSemanal: () -> Unit
) {
    var mesActual by remember { mutableStateOf(Calendar.getInstance()) }
    var diaSeleccionado by remember { mutableStateOf<Calendar?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario Académico") },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onVerHorarioSemanal) {
                        Icon(Icons.Default.CalendarViewWeek, "Ver Horario")
                    }
                    IconButton(onClick = onRegistrarHorario) {
                        Icon(Icons.Default.Add, "Registrar Horario")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Selector de mes
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        mesActual = (mesActual.clone() as Calendar).apply {
                            add(Calendar.MONTH, -1)
                        }
                    }) {
                        Icon(Icons.Default.ChevronLeft, "Mes anterior")
                    }

                    Text(
                        text = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
                            .format(mesActual.time),
                        style = MaterialTheme.typography.titleLarge
                    )

                    IconButton(onClick = {
                        mesActual = (mesActual.clone() as Calendar).apply {
                            add(Calendar.MONTH, 1)
                        }
                    }) {
                        Icon(Icons.Default.ChevronRight, "Mes siguiente")
                    }
                }
            }

            // Calendario
            CalendarioMensual(
                mes = mesActual,
                gestorHorario = gestorHorario,
                gestorAsistencia = gestorAsistencia,
                onDiaClick = { dia -> diaSeleccionado = dia }
            )

            // Detalles del día seleccionado
            diaSeleccionado?.let { dia ->
                DetallesDiaCard(
                    dia = dia,
                    gestorHorario = gestorHorario,
                    gestorAsistencia = gestorAsistencia,
                    cursos = cursos,
                    onCerrar = { diaSeleccionado = null }
                )
            }
        }
    }
}

@Composable
fun CalendarioMensual(
    mes: Calendar,
    gestorHorario: GestorHorario,
    gestorAsistencia: GestorAsistencia,
    onDiaClick: (Calendar) -> Unit
) {
    val primerDia = (mes.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val ultimoDia = mes.getActualMaximum(Calendar.DAY_OF_MONTH)
    val primerDiaSemana = primerDia.get(Calendar.DAY_OF_WEEK)

    // Ajustar para que Lunes sea el primer día
    val offset = if (primerDiaSemana == Calendar.SUNDAY) 6 else primerDiaSemana - 2

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        // Cabecera con días de la semana
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom").forEach { dia ->
                Text(
                    text = dia,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Días del mes
        var diaActual = 1
        for (semana in 0..5) {
            if (diaActual > ultimoDia) break

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (diaSemana in 0..6) {
                    if (semana == 0 && diaSemana < offset || diaActual > ultimoDia) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val diaCalendario = (mes.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, diaActual)
                        }

                        DiaCelda(
                            dia = diaActual,
                            esHoy = esHoy(diaCalendario),
                            tieneClases = tieneClasesEseDia(diaCalendario, gestorHorario),
                            porcentajeAsistencia = obtenerAsistenciaDia(diaCalendario, gestorAsistencia),
                            onClick = { onDiaClick(diaCalendario) },
                            modifier = Modifier.weight(1f)
                        )
                        diaActual++
                    }
                }
            }
        }
    }
}

@Composable
fun DiaCelda(
    dia: Int,
    esHoy: Boolean,
    tieneClases: Boolean,
    porcentajeAsistencia: Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(
                color = when {
                    esHoy -> MaterialTheme.colorScheme.primaryContainer
                    porcentajeAsistencia != null && porcentajeAsistencia < 0.75f ->
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    tieneClases -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    else -> Color.Transparent
                },
                shape = MaterialTheme.shapes.small
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dia.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (esHoy) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )

            if (tieneClases) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun DetallesDiaCard(
    dia: Calendar,
    gestorHorario: GestorHorario,
    gestorAsistencia: GestorAsistencia,
    cursos: List<Curso>,
    onCerrar: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
    val diaSemana = DiaSemana.fromCalendar(dia.get(Calendar.DAY_OF_WEEK))
    val clases = gestorHorario.obtenerClasesPorDia(diaSemana)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(dia.time),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onCerrar) {
                    Icon(Icons.Default.Close, "Cerrar")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            if (clases.isEmpty()) {
                Text(
                    text = "No hay clases programadas este día",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                clases.forEach { clase ->
                    ClaseItem(clase)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ClaseItem(clase: ClaseHorario) {
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
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clase.nombreCurso,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = clase.obtenerHorarioCompleto(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${clase.sala} • ${clase.profesor}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = clase.tipoClase.descripcion,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun esHoy(dia: Calendar): Boolean {
    val hoy = Calendar.getInstance()
    return dia.get(Calendar.YEAR) == hoy.get(Calendar.YEAR) &&
            dia.get(Calendar.DAY_OF_YEAR) == hoy.get(Calendar.DAY_OF_YEAR)
}

fun tieneClasesEseDia(dia: Calendar, gestorHorario: GestorHorario): Boolean {
    val diaSemana = DiaSemana.fromCalendar(dia.get(Calendar.DAY_OF_WEEK))
    return gestorHorario.obtenerClasesPorDia(diaSemana).isNotEmpty()
}

fun obtenerAsistenciaDia(dia: Calendar, gestorAsistencia: GestorAsistencia): Float? {
    // Implementar lógica para obtener asistencia del día
    return null
}