package com.example.saferouter.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.saferouter.MainActivity
import com.example.saferouter.R

/**
 * Helper para gestionar notificaciones locales
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID_DANGER_ZONE = "danger_zone_alerts"
        private const val CHANNEL_ID_ARRIVAL = "arrival_notifications"
        private const val CHANNEL_NAME_DANGER_ZONE = "Alertas de Zonas Peligrosas"
        private const val CHANNEL_NAME_ARRIVAL = "Notificaciones de Llegada"
        private const val NOTIFICATION_ID_DANGER_ZONE = 1001
        private const val NOTIFICATION_ID_ARRIVAL = 1002
    }

    init {
        createNotificationChannels()
    }

    /**
     * Crear canales de notificación (Android 8.0+)
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Canal para alertas de zonas peligrosas
            val dangerZoneChannel = NotificationChannel(
                CHANNEL_ID_DANGER_ZONE,
                CHANNEL_NAME_DANGER_ZONE,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas cuando te acercas a zonas reportadas como peligrosas"
                enableVibration(true)
                enableLights(true)
            }

            // Canal para notificaciones de llegada
            val arrivalChannel = NotificationChannel(
                CHANNEL_ID_ARRIVAL,
                CHANNEL_NAME_ARRIVAL,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones cuando llegas a tu destino"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(dangerZoneChannel)
            notificationManager.createNotificationChannel(arrivalChannel)
        }
    }

    /**
     * Mostrar notificación de zona peligrosa cercana
     */
    fun showDangerZoneAlert(
        tipo: String,
        distancia: Int,
        descripcion: String = "",
        withSound: Boolean = true,
        withVibration: Boolean = true
    ) {
        // Verificar permiso de notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Intent para abrir la app al tocar la notificación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Emoji según tipo de incidente
        val emoji = when (tipo) {
            "Robo" -> "🚨"
            "Asalto" -> "⚠️"
            "Alta congestión vehicular" -> "🚗"
            "Baja iluminación de la zona" -> "💡"
            "Huelga" -> "✊"
            else -> "⚠️"
        }

        // Construir notificación
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_DANGER_ZONE)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("$emoji Alerta: $tipo cerca")
            .setContentText("A $distancia metros de ti${if (descripcion.isNotEmpty()) " - $descripcion" else ""}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        // Agregar sonido si está habilitado
        if (withSound) {
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(defaultSoundUri)
        }

        // Agregar vibración si está habilitada
        if (withVibration) {
            builder.setVibrate(longArrayOf(0, 500, 250, 500))
        }

        // Estilo expandido con más información
        if (descripcion.isNotEmpty()) {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$tipo reportado a $distancia metros\n\n$descripcion\n\nMantente alerta y considera tomar una ruta alternativa.")
            )
        }

        // Mostrar notificación
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_DANGER_ZONE + tipo.hashCode(), builder.build())
        }
    }

    /**
     * Mostrar notificación de llegada segura
     */
    fun showArrivalNotification(destination: String) {
        // Verificar permiso de notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ARRIVAL)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("✅ Llegada Segura")
            .setContentText("Has llegado a $destination")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 200, 100, 200))

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_ARRIVAL, builder.build())
        }
    }

    /**
     * Cancelar todas las notificaciones de zonas peligrosas
     */
    fun cancelAllDangerZoneNotifications() {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancelAll()
    }
}