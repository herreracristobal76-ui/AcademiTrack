package com.academitrack.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.academitrack.app.domain.*
import com.academitrack.app.services.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

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
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCameraPermission = it }

    var imagenCapturada by remember { mutableStateOf<Bitmap?>(null) }
    var archivoBase64 by remember { mutableStateOf<String?>(null) }
    var mimeTypeArchivo by remember { mutableStateOf<String?>(null) }
    var nombreArchivo by remember { mutableStateOf<String?>(null) }

    var procesando by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<ResultadoHorarioConCursos?>(null) }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val contentResolver = context.contentResolver
                val type = contentResolver.getType(it) ?: "application/octet-stream"
                mimeTypeArchivo = type

                contentResolver.openInputStream(it)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    archivoBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                }

                imagenCapturada = null
                nombreArchivo = it.lastPathSegment ?: "Archivo seleccionado"

                if (type.startsWith("image")) {
                    try {
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(
                            Base64.decode(archivoBase64, Base64.DEFAULT), 0, Base64.decode(archivoBase64, Base64.DEFAULT).size
                        )
                        imagenCapturada = bitmap
                    } catch (e: Exception) { e.printStackTrace() }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            imagenCapturada = it
            val stream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            archivoBase64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            mimeTypeArchivo = "image/jpeg"
            nombreArchivo = "Foto de cámara"
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subir Horario") },
                navigationIcon = { IconButton(onClick = onVolverClick) { Icon(Icons.Default.ArrowBack, "Volver") } }
            )
        }
    ) { paddingValues ->
        if (archivoBase64 == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Sube tu Horario", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Acepta Imágenes y PDF", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = { takePictureLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tomar Foto")
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { pickFileLauncher.launch(arrayOf("image/*", "application/pdf")) },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.AttachFile, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Subir Archivo (PDF/IMG)")
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
                Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (imagenCapturada != null) {
                            Image(
                                bitmap = imagenCapturada!!.asImageBitmap(),
                                contentDescription = "Preview",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (mimeTypeArchivo?.contains("pdf") == true) {
                                    Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.error)
                                } else {
                                    Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(nombreArchivo ?: "Archivo cargado", style = MaterialTheme.typography.titleMedium)
                                Text("Listo para procesar", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (procesando) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("🤖 Analizando horario...", modifier = Modifier.padding(vertical = 8.dp))
                }

                resultado?.let { res ->
                    if (res.exito) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("✅ Éxito", fontWeight = FontWeight.Bold)
                                Text(res.mensaje)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { onGuardarHorario(res) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Guardar Horario")
                                }
                            }
                        }
                    } else {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("❌ Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Text(res.mensaje, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            archivoBase64 = null
                            imagenCapturada = null
                            resultado = null
                            mimeTypeArchivo = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            procesando = true
                            scope.launch {
                                try {
                                    val iaService = HorarioIAService(apiKey)
                                    val res = iaService.procesarImagenHorario(
                                        imagenBase64 = archivoBase64!!,
                                        mimeType = mimeTypeArchivo!!,
                                        cursosExistentes = cursos,
                                        semestre = semestre
                                    )
                                    resultado = res
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    resultado = ResultadoHorarioConCursos(false, emptyList(), emptyList(), 0.0, "Error: ${e.message}")
                                } finally {
                                    procesando = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !procesando && archivoBase64 != null
                    ) {
                        Text("Procesar con IA")
                    }
                }
            }
        }
    }
}