package com.example.saferouter.presentation.home

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.saferouter.R
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class TravelLocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val channelId = "travel_tracking_channel"
    private val db = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                for (location in result.locations) {
                    sendLocationToFirebase(location)
                    notifyContactsWithLocation(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TravelLocationService", "Servicio iniciado")

        val notification = createForegroundNotification()
        startForeground(1, notification)

        // Notificar a contactos que se inició el viaje
        notifyContactsTripStarted()

        startLocationUpdates()

        return START_STICKY
    }

    private fun notifyContactsTripStarted() {
        val user = auth.currentUser ?: return
        val userUid = user.uid

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener información del usuario
                val userDoc = db.collection("users").document(userUid).get().await()
                val userName = userDoc.getString("name") ?: "Usuario SafeRoute"

                // Obtener contactos de emergencia
                val contacts = userDoc.get("contacts") as? List<Map<String, String>> ?: emptyList()

                // Obtener información del viaje actual
                val viajeSnapshot = realtimeDb.getReference("viajes/$userUid/info").get().await()
                val destino = viajeSnapshot.child("destino").getValue(String::class.java) ?: "Destino no especificado"
                val tiempoEstimado = viajeSnapshot.child("tiempoEstimado").getValue(String::class.java) ?: "Tiempo no especificado"

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val startTime = dateFormat.format(Date())

                // NOTIFICACIÓN DE PRUEBA - Crear una notificación para el usuario actual también
                val selfNotificationData = mapOf(
                    "type" to "trip_started",
                    "userName" to userName,
                    "userUid" to userUid,
                    "destination" to destino,
                    "estimatedTime" to tiempoEstimado,
                    "startTime" to startTime,
                    "timestamp" to System.currentTimeMillis(),
                    "read" to false
                )

                // Guardar notificación para el usuario actual (usando su UID como identificador)
                realtimeDb.getReference("notifications/$userUid")
                    .push()
                    .setValue(selfNotificationData)

                Log.d("TravelLocationService", "Notificación de prueba creada para el usuario")

                // Notificar a cada contacto (funcionalidad original)
                for (contact in contacts) {
                    val contactName = contact["nombre"] ?: "Contacto"
                    val contactPhone = contact["telefono"] ?: ""

                    val notificationData = mapOf(
                        "type" to "trip_started",
                        "userName" to userName,
                        "userUid" to userUid,
                        "destination" to destino,
                        "estimatedTime" to tiempoEstimado,
                        "startTime" to startTime,
                        "timestamp" to System.currentTimeMillis(),
                        "read" to false
                    )

                    realtimeDb.getReference("notifications/$contactPhone")
                        .push()
                        .setValue(notificationData)

                    Log.d("TravelLocationService", "Notificado: $contactName ($contactPhone)")
                }

            } catch (e: Exception) {
                Log.e("TravelLocationService", "Error notificando contactos: ${e.message}")
            }
        }
    }

    private fun sendSmsNotification(phone: String, userName: String, destination: String, estimatedTime: String, startTime: String) {
        // Aquí puedes integrar un servicio de SMS como Twilio o usar el intent de SMS nativo
        // Por ahora solo lo logueamos
        val message = """
            🔔 SafeRoute - Notificación de Viaje
            $userName ha iniciado un viaje.
            🎯 Destino: $destination
            ⏱️ Tiempo estimado: $estimatedTime
            🕐 Hora de inicio: $startTime
            Su ubicación será compartida durante el viaje.
        """.trimIndent()

        Log.d("TravelLocationService", "SMS para $phone: $message")

        // Para enviar SMS real, necesitarías permisos y una implementación de SMS Manager
        /*
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phone, null, message, null, null)
        } catch (e: Exception) {
            Log.e("TravelLocationService", "Error enviando SMS: ${e.message}")
        }
        */
    }

    private fun notifyContactsWithLocation(location: Location) {
        val user = auth.currentUser ?: return
        val userUid = user.uid

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener contactos del usuario
                val userDoc = db.collection("users").document(userUid).get().await()
                val contacts = userDoc.get("contacts") as? List<Map<String, String>> ?: emptyList()
                val userName = userDoc.getString("name") ?: "Usuario SafeRoute"

                // Crear datos de ubicación para compartir
                val locationData = mapOf(
                    "type" to "location_update",
                    "userName" to userName,
                    "userUid" to userUid,
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "timestamp" to System.currentTimeMillis(),
                    "speed" to location.speed,
                    "accuracy" to location.accuracy
                )

                // Compartir ubicación con cada contacto
                for (contact in contacts) {
                    val contactPhone = contact["telefono"] ?: ""

                    realtimeDb.getReference("shared_locations/$contactPhone/$userUid")
                        .setValue(locationData)
                }

            } catch (e: Exception) {
                Log.e("TravelLocationService", "Error compartiendo ubicación: ${e.message}")
            }
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L
        ).setMinUpdateIntervalMillis(3000L).build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("TravelLocationService", "Permisos de ubicación no concedidos")
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    private fun sendLocationToFirebase(location: Location) {
        val user = auth.currentUser ?: return
        val dbRef = realtimeDb.reference
            .child("viajes")
            .child(user.uid)
            .child("ubicacion")

        val data = mapOf(
            "lat" to location.latitude,
            "lng" to location.longitude,
            "timestamp" to System.currentTimeMillis(),
            "speed" to location.speed,
            "accuracy" to location.accuracy
        )

        dbRef.push().setValue(data)
            .addOnSuccessListener {
                Log.d("TravelLocationService", "Ubicación enviada: $data")
            }
            .addOnFailureListener {
                Log.e("TravelLocationService", "Error al enviar ubicación: ${it.message}")
            }
    }

    private fun createForegroundNotification(): Notification {
        val notificationIntent = Intent(this, Class.forName("com.example.saferouter.MainActivity"))
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("🚗 Viaje en curso - SafeRoute")
            .setContentText("Compartiendo ubicación con tus contactos de emergencia")
            .setSmallIcon(R.drawable.viaje)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Seguimiento de Viaje",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("TravelLocationService", "Servicio detenido")

        // Notificar a contactos que el viaje finalizó
        notifyContactsTripEnded()
    }

    private fun notifyContactsTripEnded() {
        val user = auth.currentUser ?: return
        val userUid = user.uid

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userDoc = db.collection("users").document(userUid).get().await()
                val contacts = userDoc.get("contacts") as? List<Map<String, String>> ?: emptyList()
                val userName = userDoc.getString("name") ?: "Usuario SafeRoute"

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val endTime = dateFormat.format(Date())

                for (contact in contacts) {
                    val contactPhone = contact["telefono"] ?: ""

                    val endNotification = mapOf(
                        "type" to "trip_ended",
                        "userName" to userName,
                        "userUid" to userUid,
                        "endTime" to endTime,
                        "timestamp" to System.currentTimeMillis(),
                        "read" to false
                    )

                    realtimeDb.getReference("notifications/$contactPhone")
                        .push()
                        .setValue(endNotification)
                }

            } catch (e: Exception) {
                Log.e("TravelLocationService", "Error notificando fin de viaje: ${e.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}