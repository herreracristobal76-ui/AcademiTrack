package com.academitrack.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.academitrack.app.domain.Curso
import com.academitrack.app.notifications.AsistenciaNotificationWorker
import com.academitrack.app.persistence.PersistenciaLocal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurarNotificacionesScreen(
    cursos: List<Curso>,
    onVolverClick: () -> Unit,
    persistencia: PersistenciaLocal
) {
    val context = LocalContext.current

    var tienePermiso by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        tienePermiso = isGranted
    }

    // Cargar configuraciones guardadas
    val configuraciones = remember {
        mutableStateMapOf<String, NotificacionConfig>().apply {
            cursos.forEach { curso ->
                val config = persistencia.cargarConfigNotificacion(curso.getId())
                this[curso.getId()] = config
            }
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !tienePermiso) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones de Asistencia") },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!tienePermiso) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Permiso de Notificaciones",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Necesitamos permiso para enviarte recordatorios de asistencia",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                ) {
                    Text("Dar Permiso")
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
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "ℹ️ Cómo funciona",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Activa notificaciones para cada curso y te recordaremos registrar tu asistencia todos los días a la hora configurada.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (cursos.isEmpty()) {
                    item {
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
                                Text("No tienes cursos")
                                Text(
                                    "Agrega cursos para configurar notificaciones",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else {
                    items(cursos) { curso ->
                        NotificacionCursoCard(
                            curso = curso,
                            config = configuraciones[curso.getId()] ?: NotificacionConfig(),
                            onConfigChange = { newConfig ->
                                configuraciones[curso.getId()] = newConfig
                                persistencia.guardarConfigNotificacion(curso.getId(), newConfig)

                                if (newConfig.activo) {
                                    AsistenciaNotificationWorker.programarNotificacionDiaria(
                                        context = context,
                                        cursoId = curso.getId(),
                                        cursoNombre = curso.getNombre(),
                                        hora = newConfig.hora,
                                        minuto = newConfig.minuto
                                    )
                                } else {
                                    AsistenciaNotificationWorker.cancelarNotificaciones(
                                        context,
                                        curso.getId()
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificacionCursoCard(
    curso: Curso,
    config: NotificacionConfig,
    onConfigChange: (NotificacionConfig) -> Unit
) {
    var mostrarTimePicker by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                }
                Switch(
                    checked = config.activo,
                    onCheckedChange = { activo ->
                        onConfigChange(config.copy(activo = activo))
                    }
                )
            }

            if (config.activo) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { mostrarTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Notifications, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hora: ${String.format("%02d:%02d", config.hora, config.minuto)}")
                }
            }
        }
    }

    if (mostrarTimePicker) {
        TimePickerDialog(
            horaInicial = config.hora,
            minutoInicial = config.minuto,
            onConfirm = { hora, minuto ->
                onConfigChange(config.copy(hora = hora, minuto = minuto))
                mostrarTimePicker = false
            },
            onDismiss = { mostrarTimePicker = false }
        )
    }
}

@Composable
fun TimePickerDialog(
    horaInicial: Int,
    minutoInicial: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hora by remember { mutableStateOf(horaInicial) }
    var minuto by remember { mutableStateOf(minutoInicial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Hora") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Hora de notificación diaria")
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Selector de hora
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { hora = (hora + 1) % 24 }) {
                            Text("▲")
                        }
                        Text(
                            text = String.format("%02d", hora),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        IconButton(onClick = { hora = if (hora > 0) hora - 1 else 23 }) {
                            Text("▼")
                        }
                    }

                    Text(":", style = MaterialTheme.typography.headlineMedium)

                    // Selector de minuto
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { minuto = (minuto + 15) % 60 }) {
                            Text("▲")
                        }
                        Text(
                            text = String.format("%02d", minuto),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        IconButton(onClick = {
                            minuto = if (minuto >= 15) minuto - 15 else 45
                        }) {
                            Text("▼")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(hora, minuto) }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

data class NotificacionConfig(
    val activo: Boolean = false,
    val hora: Int = 20,
    val minuto: Int = 0
)