package com.academitrack.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.academitrack.app.domain.Curso
import com.academitrack.app.domain.EvaluacionFotografica
import com.academitrack.app.services.IAService
import com.academitrack.app.services.ResultadoIA
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    curso: Curso,
    apiKey: String,
    onVolverClick: () -> Unit,
    onGuardar: (EvaluacionFotografica) -> Unit
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
    var resultado by remember { mutableStateOf<ResultadoIA?>(null) }

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
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
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
                title = { Text("Capturar Nota con IA") },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("📷 Se necesita permiso de cámara")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Dar Permiso")
                }
            }
        } else if (imagenCapturada == null) {
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
                        Text(
                            text = "📸 Captura tu Nota",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Elige cómo obtener la imagen",
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
                    Text("📷 Tomar Foto", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { pickImageLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("🖼️ Elegir de Galería", style = MaterialTheme.typography.titleMedium)
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
                            text = "💡 Consejos:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text("• Buena iluminación", style = MaterialTheme.typography.bodySmall)
                        Text("• Foto enfocada y clara", style = MaterialTheme.typography.bodySmall)
                        Text("• Toda la información visible", style = MaterialTheme.typography.bodySmall)
                        Text("• Puedes usar una foto existente", style = MaterialTheme.typography.bodySmall)
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
                        "🤖 Procesando con IA...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                resultado?.let { res ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (res.exito)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (res.exito) "✅ Datos Extraídos por IA" else "❌ Error",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            res.nombreEvaluacion?.let {
                                Text("📝 Nombre: $it")
                            }
                            res.nota?.let {
                                Text("📊 Nota: $it", style = MaterialTheme.typography.titleLarge)
                            }
                            res.porcentaje?.let {
                                Text("📈 Porcentaje: $it%")
                            }
                            res.fecha?.let {
                                Text("📅 Fecha: $it")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "🎯 Confianza IA: ${String.format("%.0f", res.confianza)}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (!res.mensaje.isNullOrEmpty() && !res.exito) {
                                Text(
                                    res.mensaje,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (res.exito && res.nota != null && res.porcentaje != null) {
                        Button(
                            onClick = {
                                val eval = EvaluacionFotografica(
                                    id = "eval_foto_${System.currentTimeMillis()}",
                                    nombreEval = res.nombreEvaluacion ?: "Evaluación",
                                    porcentajeEval = res.porcentaje!!,
                                    fechaEval = System.currentTimeMillis(),
                                    idCursoEval = curso.getId(),
                                    rutaImagen = "",
                                    confianzaIA = res.confianza
                                )
                                eval.setNotaObtenida(res.nota!!)
                                onGuardar(eval)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("💾 Guardar Evaluación")
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
                                    val iaService = IAService(apiKey)
                                    val stream = ByteArrayOutputStream()
                                    imagenCapturada!!.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                                    val base64 = iaService.convertirABase64(stream.toByteArray())

                                    val res = iaService.procesarImagenNota(base64)
                                    resultado = res
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    resultado = ResultadoIA(
                                        exito = false,
                                        nombreEvaluacion = null,
                                        nota = null,
                                        porcentaje = null,
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
                        Text("🤖 IA")
                    }
                }
            }
        }
    }
}