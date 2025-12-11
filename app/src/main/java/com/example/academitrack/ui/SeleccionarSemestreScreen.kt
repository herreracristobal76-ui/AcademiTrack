package com.academitrack.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.academitrack.app.domain.Semestre
import com.academitrack.app.domain.TipoSemestre
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeleccionarSemestreScreen(
    onVolverClick: () -> Unit,
    onSemestreSeleccionado: (Semestre) -> Unit
) {
    val anioActual = Calendar.getInstance().get(Calendar.YEAR)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar Semestre") },
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📅 Elige el semestre",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Selecciona el semestre para el que deseas registrar el horario",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Semestres del año actual
            Text(
                text = "Año $anioActual",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            SemestreCard(
                semestre = Semestre.crearSemestre1(anioActual),
                onClick = { onSemestreSeleccionado(Semestre.crearSemestre1(anioActual)) }
            )

            SemestreCard(
                semestre = Semestre.crearSemestre2(anioActual),
                onClick = { onSemestreSeleccionado(Semestre.crearSemestre2(anioActual)) }
            )

            // Próximo año
            Text(
                text = "Año ${anioActual + 1}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            SemestreCard(
                semestre = Semestre.crearSemestre1(anioActual + 1),
                onClick = { onSemestreSeleccionado(Semestre.crearSemestre1(anioActual + 1)) }
            )
        }
    }
}

@Composable
fun SemestreCard(
    semestre: Semestre,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = semestre.obtenerNombre(),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = semestre.tipo.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Seleccionar"
            )
        }
    }
}