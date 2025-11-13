package com.example.saferouter.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.saferouter.data.preferences.NotificationPreferencesManager
import com.example.saferouter.model.Reporte
import com.example.saferouter.notification.NotificationHelper
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.math.*


class ZoneMonitorWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ZoneMonitorWorker"
        private const val WORK_NAME = "zone_monitoring_work"

        /**
         * Iniciar monitoreo periódico
         */
        fun startPeriodicMonitoring(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ZoneMonitorWorker>(
                15, TimeUnit.MINUTES // Ejecutar cada 15 minutos
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )

            Log.d(TAG, "Monitoreo periódico iniciado")
        }

        /**
         * Detener monitoreo periódico
         */
        fun stopPeriodicMonitoring(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Monitoreo periódico detenido")
        }
    }

    private val preferencesManager = NotificationPreferencesManager(applicationContext)
    private val notificationHelper = NotificationHelper(applicationContext)
    private val db = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Iniciando verificación de zonas peligrosas...")

            // Verificar si las notificaciones están habilitadas
            val prefs = preferencesManager.getAllPreferences()
            if (!prefs.notificationsEnabled) {
                Log.d(TAG, "Notificaciones deshabilitadas")
                return Result.success()
            }

            // Obtener ubicación actual
            val currentLocation = getCurrentLocation() ?: run {
                Log.w(TAG, "No se pudo obtener la ubicación")
                return Result.retry()
            }

            // Obtener reportes de Firestore
            val reportes = getRecentReports()
            Log.d(TAG, "Reportes obtenidos: ${reportes.size}")

            // Verificar proximidad a zonas peligrosas
            checkProximityToReports(currentLocation, reportes, prefs)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error en monitoreo: ${e.message}", e)
            Result.retry()
        }
    }

    /**
     * Obtener ubicación actual del usuario
     */
    private suspend fun getCurrentLocation(): Location? {
        return try {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Permiso de ubicación no otorgado")
                return null
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo ubicación: ${e.message}")
            null
        }
    }

    /**
     * Obtener reportes recientes de Firestore (últimas 24 horas)
     */
    private suspend fun getRecentReports(): List<Reporte> {
        return try {
            val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)

            val snapshot = db.collection("reportes")
                .whereGreaterThan("fecha", com.google.firebase.Timestamp(java.util.Date(oneDayAgo)))
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Reporte::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo reportes: ${e.message}")
            emptyList()
        }
    }

    /**
     * Verificar proximidad a reportes y enviar notificaciones
     */
    private suspend fun checkProximityToReports(
        currentLocation: Location,
        reportes: List<Reporte>,
        prefs: com.example.saferouter.data.preferences.NotificationPreferences
    ) {
        val alertRadius = prefs.alertRadius // en metros

        reportes.forEach { reporte ->
            // Verificar si debemos notificar este tipo de incidente
            if (!preferencesManager.shouldNotifyForType(reporte.tipo)) {
                return@forEach
            }

            val distancia = calculateDistance(
                currentLocation.latitude,
                currentLocation.longitude,
                reporte.ubicacion.latitud,
                reporte.ubicacion.longitud
            )

            // Si está dentro del radio de alerta
            if (distancia <= alertRadius) {
                Log.d(TAG, "Zona peligrosa cerca: ${reporte.tipo} a ${distancia.toInt()}m")

                notificationHelper.showDangerZoneAlert(
                    tipo = reporte.tipo,
                    distancia = distancia.toInt(),
                    descripcion = reporte.descripcion,
                    withSound = prefs.soundEnabled,
                    withVibration = prefs.vibrationEnabled
                )
            }
        }
    }

    /**
     * Calcular distancia entre dos puntos usando fórmula de Haversine
     * Adaptado del RouteCalculator existente
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Radio de la Tierra en metros

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c // Distancia en metros
    }
}