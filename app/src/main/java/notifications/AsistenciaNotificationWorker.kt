package com.academitrack.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.academitrack.app.MainActivity
import com.academitrack.app.R
import com.academitrack.app.domain.EstadoAsistencia
import java.util.concurrent.TimeUnit

/**
 * Worker que se ejecuta diariamente para recordar registrar asistencia
 *
 * UBICACIÓN: app/src/main/java/com/academitrack/app/notifications/AsistenciaNotificationWorker.kt
 */
class AsistenciaNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // Obtener el nombre del curso desde los datos del worker
        val cursoNombre = inputData.getString(KEY_CURSO_NOMBRE) ?: "tu curso"
        val cursoId = inputData.getString(KEY_CURSO_ID) ?: ""

        // Crear y mostrar la notificación
        mostrarNotificacion(cursoNombre, cursoId)

        return Result.success()
    }

    private fun mostrarNotificacion(cursoNombre: String, cursoId: String) {
        // Crear el canal de notificación (necesario para Android 8.0+)
        crearCanalNotificacion()

        // Intent para abrir la app cuando se toque la notificación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_CURSO_ID, cursoId)
            putExtra(EXTRA_ABRIR_ASISTENCIA, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            cursoId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Construir la notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Necesitarás crear este icono
            .setContentTitle("📚 $cursoNombre")
            .setContentText("¿Asististe a clase hoy?")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("¿Asististe a clase hoy? Registra tu asistencia ahora.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_check,
                "Presente",
                crearAccionPendingIntent(cursoId, EstadoAsistencia.PRESENTE)
            )
            .addAction(
                R.drawable.ic_close,
                "Ausente",
                crearAccionPendingIntent(cursoId, EstadoAsistencia.AUSENTE)
            )
            .build()

        // Mostrar la notificación
        with(NotificationManagerCompat.from(context)) {
            notify(cursoId.hashCode(), notification)
        }
    }

    private fun crearAccionPendingIntent(cursoId: String, estado: EstadoAsistencia): PendingIntent {
        val intent = Intent(context, AsistenciaActionReceiver::class.java).apply {
            putExtra(EXTRA_CURSO_ID, cursoId)
            putExtra(EXTRA_ESTADO_ASISTENCIA, estado.name)
        }

        return PendingIntent.getBroadcast(
            context,
            "${cursoId}_${estado.name}".hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nombre = "Recordatorios de Asistencia"
            val descripcion = "Notificaciones para registrar tu asistencia a clases"
            val importancia = NotificationManager.IMPORTANCE_HIGH

            val canal = NotificationChannel(CHANNEL_ID, nombre, importancia).apply {
                description = descripcion
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(canal)
        }
    }

    companion object {
        const val CHANNEL_ID = "asistencia_channel"
        const val KEY_CURSO_NOMBRE = "curso_nombre"
        const val KEY_CURSO_ID = "curso_id"
        const val EXTRA_CURSO_ID = "extra_curso_id"
        const val EXTRA_ABRIR_ASISTENCIA = "extra_abrir_asistencia"
        const val EXTRA_ESTADO_ASISTENCIA = "extra_estado_asistencia"
        const val WORK_TAG = "asistencia_notification"

        /**
         * Programa una notificación diaria para un curso específico
         */
        fun programarNotificacionDiaria(
            context: Context,
            cursoId: String,
            cursoNombre: String,
            hora: Int = 20, // 8 PM por defecto
            minuto: Int = 0
        ) {
            val data = Data.Builder()
                .putString(KEY_CURSO_ID, cursoId)
                .putString(KEY_CURSO_NOMBRE, cursoNombre)
                .build()

            // Calcular el delay inicial hasta la hora programada
            val currentTime = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hora)
                set(java.util.Calendar.MINUTE, minuto)
                set(java.util.Calendar.SECOND, 0)

                // Si ya pasó la hora de hoy, programar para mañana
                if (timeInMillis <= currentTime) {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }

            val initialDelay = calendar.timeInMillis - currentTime

            // Crear la solicitud de trabajo periódico
            val workRequest = PeriodicWorkRequestBuilder<AsistenciaNotificationWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("$WORK_TAG-$cursoId")
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "asistencia_$cursoId",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    workRequest
                )
        }

        /**
         * Cancela las notificaciones para un curso específico
         */
        fun cancelarNotificaciones(context: Context, cursoId: String) {
            WorkManager.getInstance(context)
                .cancelUniqueWork("asistencia_$cursoId")
        }

        /**
         * Cancela todas las notificaciones de asistencia
         */
        fun cancelarTodasNotificaciones(context: Context) {
            WorkManager.getInstance(context)
                .cancelAllWorkByTag(WORK_TAG)
        }
    }
}