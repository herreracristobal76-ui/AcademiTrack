package com.academitrack.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.academitrack.app.persistence.PersistenciaLocal

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            reprogramarNotificaciones(context)
        }
    }

    private fun reprogramarNotificaciones(context: Context) {
        try {
            val persistencia = PersistenciaLocal(context)
            val cursos = persistencia.cargarCursos()

            cursos.forEach { curso ->
                val config = persistencia.cargarConfigNotificacion(curso.getId())

                if (config.activo) {
                    AsistenciaNotificationWorker.programarNotificacionDiaria(
                        context = context,
                        cursoId = curso.getId(),
                        cursoNombre = curso.getNombre(),
                        hora = config.hora,
                        minuto = config.minuto
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}