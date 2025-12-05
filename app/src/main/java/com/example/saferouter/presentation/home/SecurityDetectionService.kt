package com.example.saferouter.presentation.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.saferouter.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class SecurityDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    // --- VARIABLES DE ESTADO Y UMBRALES (Corregidos por física) ---
    private var isFalling = false
    private var fallDetectedTime: Long = 0
    private var emergencyJob: Job? = null

    // 🚨 Simulación de inyección, si decides usar Retrofit/Room
    // private val reporteRepository: ReporteRepository by inject()

    private companion object {
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "SECURITY_CHANNEL"

        // 🟢 UMBRALES CORREGIDOS: Más realistas para evitar falsas alarmas (1G ≈ 9.8)
        const val THRESHOLD_FREE_FALL = 7.0 // Bajo (Caída libre)
        const val THRESHOLD_IMPACT = 12.5  // Alto (Golpe contra el suelo)
        const val FALL_WINDOW_MS = 500L
        const val EMERGENCY_DELAY_MS = 10000L
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Monitoreo de Seguridad"
            val descriptionText = "Notificación obligatoria para servicios en primer plano."
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Seguridad Activa")
            .setContentText("Monitoreando riesgos de caída y emergencia.")
            .setSmallIcon(R.drawable.ic_security_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val aTotal = sqrt((x * x + y * y + z * z))
            val currentTime = System.currentTimeMillis()

            if (aTotal < THRESHOLD_FREE_FALL && !isFalling) {
                isFalling = true
                fallDetectedTime = currentTime
            } else if (aTotal > THRESHOLD_IMPACT) {
                if (isFalling && (currentTime - fallDetectedTime < FALL_WINDOW_MS)) {
                    isFalling = false
                    startEmergencyCountdown()
                } else {
                    isFalling = false
                }
            }
        }
    }

    private fun startEmergencyCountdown() {
        if (emergencyJob?.isActive == true) return

        sensorManager.unregisterListener(this)

        Toast.makeText(this, "🚨 Caída Detectada! Enviando alerta en 10s...", Toast.LENGTH_LONG).show()

        emergencyJob = serviceScope.launch(Dispatchers.Main) {
            delay(EMERGENCY_DELAY_MS)
            sendActualEmergencyReport()
        }
    }

    // 🚨 MÉTODO FINAL SIMPLIFICADO: SÓLO AVISA Y DETIENE 🚨
    private fun sendActualEmergencyReport() {
        // En un proyecto real, aquí va la llamada a reporteRepository.saveAndSendEmergency()
        // Esa llamada usa Retrofit para enviar el JSON a Cloud Run.

        Toast.makeText(this, "🔴 EMERGENCIA ENVIADA. Deteniendo monitoreo.", Toast.LENGTH_LONG).show()
        stopSelf()
    }

    fun cancelEmergency() {
        emergencyJob?.cancel()
        Toast.makeText(this, "✅ Emergencia CANCELADA por el usuario.", Toast.LENGTH_SHORT).show()
        // Reiniciar el monitoreo
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?): IBinder? { return null }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        sensorManager.unregisterListener(this)
    }
}