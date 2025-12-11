package com.academitrack.app.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.academitrack.app.persistence.PersistenciaLocal
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    modoOscuro: Boolean,
    onCambiarModo: (Boolean) -> Unit,
    onVolverClick: () -> Unit,
    onConfigNotificaciones: () -> Unit = {}
) {
    val context = LocalContext.current
    val persistencia = remember { PersistenciaLocal(context) }

    // Launcher para GUARDAR archivo (Exportar)
    val exportarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            try {
                val jsonDatos = persistencia.exportarDatosGlobales()
                context.contentResolver.openOutputStream(it)?.use { output ->
                    output.write(jsonDatos.toByteArray())
                }
                Toast.makeText(context, "✅ Respaldo guardado exitosamente", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "❌ Error al guardar respaldo", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    // Launcher para ABRIR archivo (Importar)
    val importarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val stringBuilder = StringBuilder()
                context.contentResolver.openInputStream(it)?.use { input ->
                    BufferedReader(InputStreamReader(input)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }

                val exito = persistencia.importarDatosGlobales(stringBuilder.toString())
                if (exito) {
                    Toast.makeText(context, "✅ Datos restaurados. Reinicia la app para ver cambios.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "❌ El archivo está dañado o no es válido", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "❌ Error al leer archivo", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección General
            Text(
                "General",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🌙 Modo Oscuro",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Cambia el tema de la aplicación",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = modoOscuro,
                        onCheckedChange = onCambiarModo
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onConfigNotificaciones
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔔 Notificaciones",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Configura recordatorios de asistencia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Ir",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sección de Datos (NUEVA)
            Text(
                "Datos y Seguridad",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    // Botón Exportar
                    ListItem(
                        headlineContent = { Text("Exportar Copia de Seguridad") },
                        supportingContent = { Text("Guarda todos tus datos en un archivo") },
                        leadingContent = { Icon(Icons.Default.Upload, null) },
                        modifier = Modifier.clickable {
                            val fecha = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            exportarLauncher.launch("AcademiTrack_Backup_$fecha.json")
                        }
                    )
                    Divider()
                    // Botón Importar
                    ListItem(
                        headlineContent = { Text("Restaurar Datos") },
                        supportingContent = { Text("Recupera tus datos desde un archivo") },
                        leadingContent = { Icon(Icons.Default.Download, null) },
                        modifier = Modifier.clickable {
                            importarLauncher.launch("application/json")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Información App
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ℹ️ Acerca de",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AcademiTrack v1.1",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Gestión académica potenciada por IA",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}