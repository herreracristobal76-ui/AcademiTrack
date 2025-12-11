package com.academitrack.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.academitrack.app.domain.*
import com.academitrack.app.services.GestorAsistencia
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarioMensualScreen(
    gestorHorario: GestorHorario,
    gestorAsistencia: GestorAsistencia,
    cursos: List<Curso>,
    onVolverClick: () -> Unit,
    onRegistrarHorario: () -> Unit,
    onLimpiarHorario: () -> Unit,
    onRegistrarAsistencia: (String, Long, EstadoAsistencia) -> Unit,
    onEliminarClase: (ClaseHorario) -> Unit,
    esPantallaPrincipal: Boolean = false // NUEVO PARAMETRO
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val titles = listOf("Mes", "Semana")

    var mesActual by remember { mutableStateOf(Calendar.getInstance()) }
    var diaSeleccionado by remember { mutableStateOf<Calendar?>(null) }
    var mostrarMenuOpciones by remember { mutableStateOf(false) }
    var mostrarDialogoLimpiar by remember { mutableStateOf(false) }

    var claseAEliminar by remember { mutableStateOf<ClaseHorario?>(null) }
    var claseParaAsistenciaSemanal by remember { mutableStateOf<ClaseHorario?>(null) }

    val refreshTrigger = remember { mutableStateOf(0L) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Calendario", fontWeight = FontWeight.Bold) },
                    // Solo muestra el botón volver si NO es pantalla principal
                    navigationIcon = {
                        if (!esPantallaPrincipal) {
                            IconButton(onClick = onVolverClick) {
                                Icon(Icons.Default.ArrowBack, "Volver")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    actions = {
                        IconButton(onClick = { mostrarMenuOpciones = true }) {
                            Icon(Icons.Default.MoreVert, "Opciones")
                        }
                        DropdownMenu(
                            expanded = mostrarMenuOpciones,
                            onDismissRequest = { mostrarMenuOpciones = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🗑️ Borrar todo el horario") },
                                onClick = {
                                    mostrarMenuOpciones = false
                                    mostrarDialogoLimpiar = true
                                }
                            )
                        }
                    }
                )

                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(title, fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRegistrarHorario,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.UploadFile, null)
                Spacer(Modifier.width(8.dp))
                Text("Subir Horario")
            }
        }
    ) { paddingValues ->

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        // Selector de Mes
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(0.dp) // Diseño plano
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { mesActual = (mesActual.clone() as Calendar).apply { add(Calendar.MONTH, -1) } }) {
                                    Icon(Icons.Default.ChevronLeft, "Anterior")
                                }
                                Text(
                                    text = SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(mesActual.time).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { mesActual = (mesActual.clone() as Calendar).apply { add(Calendar.MONTH, 1) } }) {
                                    Icon(Icons.Default.ChevronRight, "Siguiente")
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            LeyendaColor(Color(0xFF4CAF50), "Bien")
                            LeyendaColor(Color(0xFFFFC107), "Regular")
                            LeyendaColor(Color(0xFFF44336), "Mal")
                        }

                        key(refreshTrigger.value) {
                            CalendarioGrid(
                                mes = mesActual,
                                gestorHorario = gestorHorario,
                                gestorAsistencia = gestorAsistencia,
                                onDiaClick = { dia -> diaSeleccionado = dia }
                            )
                        }
                    }
                }

                1 -> {
                    val todasClases = gestorHorario.obtenerTodasClases()
                    if (todasClases.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay horarios registrados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            DiaSemana.values().forEach { dia ->
                                val clasesDia = gestorHorario.obtenerClasesPorDia(dia)
                                if (clasesDia.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = dia.nombre,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    }
                                    items(clasesDia) { clase ->
                                        ItemClaseCalendario(
                                            clase = clase,
                                            onClick = { claseParaAsistenciaSemanal = clase },
                                            onEliminar = { claseAEliminar = clase }
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGOS Y COMPONENTES (Igual que antes) ---
    // (Incluye los diálogos de asistencia, eliminar, etc. que ya estaban)
    // Para brevedad, el código sigue la misma lógica interna de la versión anterior
    // solo se modificó el Scaffold y los parámetros.

    diaSeleccionado?.let { dia ->
        val fechaStr = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES")).format(dia.time)
        val diaSemana = DiaSemana.fromCalendar(dia.get(Calendar.DAY_OF_WEEK))
        val clasesDelDia = gestorHorario.obtenerClasesPorDia(diaSemana)

        Dialog(onDismissRequest = { diaSeleccionado = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = fechaStr.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))

                    if (clasesDelDia.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No hay clases este día", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text("Asistencia:", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        clasesDelDia.forEach { clase ->
                            val timestamp = dia.timeInMillis
                            val asistencias = gestorAsistencia.obtenerAsistenciasPorFecha(clase.idCurso, timestamp, timestamp + 86400000)
                            val estadoActual = asistencias.firstOrNull()?.getEstado()

                            ClassAttendanceItem(
                                clase = clase,
                                estadoActual = estadoActual,
                                onMarcar = { estado ->
                                    onRegistrarAsistencia(clase.idCurso, dia.timeInMillis, estado)
                                    refreshTrigger.value = System.currentTimeMillis()
                                    diaSeleccionado = null
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { diaSeleccionado = null }, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
                }
            }
        }
    }

    claseParaAsistenciaSemanal?.let { clase ->
        AlertDialog(
            onDismissRequest = { claseParaAsistenciaSemanal = null },
            icon = { Icon(Icons.Default.CheckCircle, null) },
            title = { Text("Registrar Asistencia") },
            text = { Text("¿Registrar asistencia para hoy (${clase.nombreCurso})?") },
            confirmButton = {
                Button(onClick = {
                    onRegistrarAsistencia(clase.idCurso, System.currentTimeMillis(), EstadoAsistencia.PRESENTE)
                    claseParaAsistenciaSemanal = null
                }) { Text("Sí, Asistí") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        onRegistrarAsistencia(clase.idCurso, System.currentTimeMillis(), EstadoAsistencia.AUSENTE)
                        claseParaAsistenciaSemanal = null
                    }) { Text("Falté") }
                    TextButton(onClick = { claseParaAsistenciaSemanal = null }) { Text("Cancelar") }
                }
            }
        )
    }

    claseAEliminar?.let { clase ->
        AlertDialog(
            onDismissRequest = { claseAEliminar = null },
            title = { Text("¿Eliminar clase?") },
            text = { Text("Se eliminará ${clase.nombreCurso} del horario.") },
            confirmButton = {
                Button(onClick = { onEliminarClase(clase); claseAEliminar = null }, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { claseAEliminar = null }) { Text("Cancelar") } }
        )
    }

    if (mostrarDialogoLimpiar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoLimpiar = false },
            title = { Text("¿Limpiar Horario?") },
            text = { Text("Se eliminarán todas las clases.") },
            confirmButton = {
                Button(onClick = { onLimpiarHorario(); mostrarDialogoLimpiar = false }, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)) { Text("Eliminar Todo") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoLimpiar = false }) { Text("Cancelar") } }
        )
    }
}

// Funciones privadas (para evitar conflicto de nombres)
@Composable
private fun ItemClaseCalendario(clase: ClaseHorario, onClick: () -> Unit, onEliminar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(50.dp)) {
            Text(clase.horaInicio, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.height(16.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Text(clase.horaFin, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(android.graphics.Color.parseColor(clase.color)).copy(alpha = 0.15f))
                .padding(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(clase.nombreCurso, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Room, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(clase.sala, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onEliminar, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun LeyendaColor(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(texto, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CalendarioGrid(
    mes: Calendar,
    gestorHorario: GestorHorario,
    gestorAsistencia: GestorAsistencia,
    onDiaClick: (Calendar) -> Unit
) {
    val diasSemana = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    val primerDiaMes = mes.clone() as Calendar
    primerDiaMes.set(Calendar.DAY_OF_MONTH, 1)
    val diasEnMes = mes.getActualMaximum(Calendar.DAY_OF_MONTH)

    var dayOfWeekOffset = primerDiaMes.get(Calendar.DAY_OF_WEEK) - 2
    if (dayOfWeekOffset < 0) dayOfWeekOffset = 6

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            diasSemana.forEach { dia ->
                Text(text = dia, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp), // Espacio para la BottomBar
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dayOfWeekOffset) { Box(modifier = Modifier.aspectRatio(1f)) }

            items(diasEnMes) { i ->
                val diaNum = i + 1
                val fechaActual = mes.clone() as Calendar
                fechaActual.set(Calendar.DAY_OF_MONTH, diaNum)
                fechaActual.set(Calendar.HOUR_OF_DAY, 0)
                fechaActual.set(Calendar.MINUTE, 0)
                fechaActual.set(Calendar.SECOND, 0)
                fechaActual.set(Calendar.MILLISECOND, 0)

                val esHoy = esMismaFecha(fechaActual, Calendar.getInstance())
                val diaSemana = DiaSemana.fromCalendar(fechaActual.get(Calendar.DAY_OF_WEEK))
                val clasesHoy = gestorHorario.obtenerClasesPorDia(diaSemana)

                var colorFondo = if (esHoy) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                var colorTexto = if (esHoy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                if (clasesHoy.isNotEmpty()) {
                    val asistenciasHoy = gestorAsistencia.obtenerAsistenciasPorFecha("", fechaActual.timeInMillis, fechaActual.timeInMillis)
                    val asistenciasRelevantes = asistenciasHoy.filter { asist -> clasesHoy.any { it.idCurso == asist.getIdCurso() } }

                    if (asistenciasRelevantes.isNotEmpty()) {
                        val efectivas = asistenciasRelevantes.filter { it.getEstado() != EstadoAsistencia.CLASE_CANCELADA }
                        if (efectivas.isNotEmpty()) {
                            val presentes = efectivas.count { it.getEstado() == EstadoAsistencia.PRESENTE }
                            val totalEfectivas = efectivas.size
                            if (presentes == totalEfectivas) { colorFondo = Color(0xFF4CAF50); colorTexto = Color.White }
                            else if (presentes == 0) { colorFondo = Color(0xFFF44336); colorTexto = Color.White }
                            else { colorFondo = Color(0xFFFFC107); colorTexto = Color.Black }
                        }
                    }
                }

                Box(
                    modifier = Modifier.aspectRatio(1f).clip(CircleShape).background(colorFondo).clickable { onDiaClick(fechaActual) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = diaNum.toString(), style = MaterialTheme.typography.bodyMedium, color = colorTexto, fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassAttendanceItem(clase: ClaseHorario, estadoActual: EstadoAsistencia?, onMarcar: (EstadoAsistencia) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(4.dp).height(32.dp).background(Color(android.graphics.Color.parseColor(clase.color)), CircleShape))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(clase.nombreCurso, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("${clase.horaInicio} - ${clase.horaFin}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FilterChip(selected = estadoActual == EstadoAsistencia.PRESENTE, onClick = { onMarcar(EstadoAsistencia.PRESENTE) }, label = { Text("Sí") }, leadingIcon = { Icon(Icons.Default.Check, null) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFDCFCE7), selectedLabelColor = Color(0xFF166534)))
                FilterChip(selected = estadoActual == EstadoAsistencia.AUSENTE, onClick = { onMarcar(EstadoAsistencia.AUSENTE) }, label = { Text("No") }, leadingIcon = { Icon(Icons.Default.Close, null) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFEE2E2), selectedLabelColor = Color(0xFF991B1B)))
                FilterChip(selected = estadoActual == EstadoAsistencia.CLASE_CANCELADA, onClick = { onMarcar(EstadoAsistencia.CLASE_CANCELADA) }, label = { Text("Cancel") }, leadingIcon = { Icon(Icons.Default.Block, null) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFEF3C7), selectedLabelColor = Color(0xFF92400E)))
            }
        }
    }
}

@Composable
private fun esMismaFecha(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}