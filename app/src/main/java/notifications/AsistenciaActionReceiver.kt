package com.academitrack.app.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.academitrack.app.domain.Asistencia
import com.academitrack.app.domain.EstadoAsistencia
import com.academitrack.app.persistence.PersistenciaLocal
import com.academitrack.app.services.GestorAsistencia
import java.util.*

class AsistenciaActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val cursoId = intent.getStringExtra(AsistenciaNotificationWorker.EXTRA_CURSO_ID) ?: return
        val estadoStr = intent.getStringExtra(AsistenciaNotificationWorker.EXTRA_ESTADO_ASISTENCIA) ?: return

        try {
            val estado = EstadoAsistencia.valueOf(estadoStr)


            registrarAsistencia(context, cursoId, estado)


            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(cursoId.hashCode())


            val mensaje = when (estado) {
                EstadoAsistencia.PRESENTE -> "✅ Asistencia registrada"
                EstadoAsistencia.AUSENTE -> "❌ Falta registrada"
                else -> "Registrado"
            }
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al registrar asistencia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registrarAsistencia(context: Context, cursoId: String, estado: EstadoAsistencia) {
        val persistencia = PersistenciaLocal(context)
        val gestorAsistencia = GestorAsistencia()


        val asistenciasExistentes = persistencia.cargarAsistencias()
        gestorAsistencia.cargarAsistencias(asistenciasExistentes)


        val hoy = obtenerFechaHoy()
        val asistenciaHoy = asistenciasExistentes.values.find {
            it.getIdCurso() == cursoId && esMismaFecha(it.getFecha(), hoy)
        }

        if (asistenciaHoy != null) {

            gestorAsistencia.actualizarEstadoAsistencia(asistenciaHoy.getId(), estado)
        } else {

            val nuevaAsistencia = Asistencia(
                idAsistencia = "asist_${System.currentTimeMillis()}",
                idCurso = cursoId,
                fecha = hoy,
                estado = estado
            )
            gestorAsistencia.registrarAsistencia(nuevaAsistencia)
        }


        persistencia.guardarAsistencias(gestorAsistencia.obtenerTodasAsistencias())
    }

    private fun obtenerFechaHoy(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun esMismaFecha(fecha1: Long, fecha2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = fecha1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = fecha2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}