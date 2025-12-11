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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioMensualScreen(
    gestorHorario: GestorHorario,
    gestorAsistencia: GestorAsistencia,
    cursos: List<Curso>,
    onVolverClick: () -> Unit,
    onRegistrarHorario: () -> Unit,
    onVerHorarioSemanal: () -> Unit,
    onLimpiarHorario: () -> Unit = {}
) {
    var mesActual by remember { mutableStateOf(Calendar.getInstance()) }
    var diaSeleccionado by remember { mutableStateOf<Calendar?>(null) }
    var mostrarMenuOpciones by remember { mutableStateOf(false) }
    var mostrarDialogoLimpiar by remember { mutableStateOf(false) }

    val tieneHorarios = gestorHorario.obtenerTodasClases().isNotEmpty()

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

                    if (tieneHorarios) {
                        IconButton(onClick = { mostrarMenuOpciones = true }) {
                            Icon(Icons.Default.MoreVert, "Opciones")
                        }
                        DropdownMenu(
                            expanded = mostrarMenuOpciones,
                            onDismissRequest = { mostrarMenuOpciones = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🗑️ Limpiar todo el horario") },
                                onClick = {
                                    mostrarMenuOpciones = false
                                    mostrarDialogoLimpiar = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        // ... resto del contenido igual

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

            CalendarioMensual(
                mes = mesActual,
                gestorHorario = gestorHorario,
                gestorAsistencia = gestorAsistencia,
                onDiaClick = { dia -> diaSeleccionado = dia }
            )

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

    // Diálogo confirmar limpiar horario
    if (mostrarDialogoLimpiar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoLimpiar = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("¿Limpiar todo el horario?") },
            text = {
                Text(
                    """
                    Se eliminarán todas las clases registradas (${gestorHorario.obtenerTodasClases().size} clases).
                    
                    Esta acción no se puede deshacer.
                    
                    Los cursos no se eliminarán, solo las clases del horario.
                    """.trimIndent()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLimpiarHorario()
                        mostrarDialogoLimpiar = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Limpiar Todo")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoLimpiar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}