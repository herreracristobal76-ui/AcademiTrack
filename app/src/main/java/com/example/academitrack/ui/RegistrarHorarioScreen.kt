package com.academitrack.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.academitrack.app.domain.*
import com.academitrack.app.services.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Pantalla para registrar horario con semestre
 * UBICACIÓN: app/src/main/java/com/academitrack/app/ui/RegistrarHorarioScreen.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarHorarioScreen(
    apiKey: String,
    cursos: List<Curso>,
    semestre: Semestre,
    onVolverClick: () -> Unit,
    onGuardarHorario: (ResultadoHorarioConCursos) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var imagenCapturada by remember { mutableStateOf<Bitmap?>(null) }
    var procesando by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<ResultadoHorarioConCursos?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(context.contentResolver, it)
                    android.graphics.ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                imagenCapturada = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            imagenCapturada = it
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Horario") },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (imagenCapturada == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "📸 Fotografía tu Horario",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = semestre.obtenerNombre(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = semestre.tipo.descripcion,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { takePictureLauncher.launch(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tomar Foto", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { pickImageLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Image, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Elegir de Galería", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💡 La IA hará:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text("• Detectar todos los cursos", style = MaterialTheme.typography.bodySmall)
                        Text("• Crear cursos automáticamente", style = MaterialTheme.typography.bodySmall)
                        Text("• Extraer horarios y salas", style = MaterialTheme.typography.bodySmall)
                        Text("• Asignar al ${semestre.obtenerNombre()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Image(
                        bitmap = imagenCapturada!!.asImageBitmap(),
                        contentDescription = "Imagen capturada",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (procesando) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        "🤖 Analizando horario...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                resultado?.let { res ->
                    if (res.exito) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "✅ Procesado exitosamente",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(text = res.mensaje)
                                Spacer(modifier = Modifier.height(8.dp))

                                if (res.cursosNuevos.isNotEmpty()) {
                                    Text(
                                        text = "🆕 Cursos nuevos:",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    res.cursosNuevos.forEach { curso ->
                                        Text(
                                            text = "• ${curso.getNombre()} (${curso.getCodigo()})",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                Text(
                                    text = "📅 ${res.clases.size} clases detectadas",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Confianza: ${String.format("%.0f", res.confianza)}%",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { onGuardarHorario(res) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Save, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar Horario y Cursos")
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "❌ No se pudo procesar",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(text = res.mensaje)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            imagenCapturada = null
                            resultado = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🔄 Nueva")
                    }

                    Button(
                        onClick = {
                            procesando = true
                            scope.launch {
                                try {
                                    val iaService = HorarioIAService(apiKey)
                                    val stream = ByteArrayOutputStream()
                                    imagenCapturada!!.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                                    val base64 = iaService.convertirABase64(stream.toByteArray())

                                    val res = iaService.procesarImagenHorario(base64, cursos, semestre)
                                    resultado = res
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    resultado = ResultadoHorarioConCursos(
                                        exito = false,
                                        cursosNuevos = emptyList(),
                                        clases = emptyList(),
                                        confianza = 0.0,
                                        mensaje = "Error: ${e.message}"
                                    )
                                } finally {
                                    procesando = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !procesando
                    ) {
                        Text("🤖 Procesar")
                    }
                }
            }
        }
    }
}