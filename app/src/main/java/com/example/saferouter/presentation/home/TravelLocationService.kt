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
        Log.d("TravelLocationService", "onCreate() - Inicializando servicio")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                for (location in result.locations) {
                    Log.d("TravelLocationService", "Nueva ubicación: ${location.latitude}, ${location.longitude}")

                    // ⬇️ AQUI MISMO METES EL UPDATE A FIRESTORE
                    updateUserLastLocation(location)

                    sendLocationToFirebase(location)
                    notifyContactsWithLocation(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TravelLocationService", "onStartCommand() - Servicio iniciado")

        val notification = createForegroundNotification()
        startForeground(1, notification)

        // Notificar a contactos que se inició el viaje
        notifyContactsTripStarted()

        startLocationUpdates()

        return START_STICKY
    }

    private fun notifyContactsTripStarted() {
        val user = auth.currentUser ?: run {
            Log.w("TravelLocationService", "Usuario no autenticado")
            return
        }
        val userUid = user.uid

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("TravelLocationService", "Iniciando notificación de viaje para UID: $userUid")

                // DEBUG: Ver qué contactos hay en Firestore
                debugUserContacts(userUid)

                // Obtener información del usuario
                val userDoc = db.collection("users").document(userUid).get().await()
                val userName = userDoc.getString("name") ?: "Usuario SafeRoute"

                Log.d("TravelLocationService", "Nombre de usuario: $userName")

                // Obtener contactos de emergencia SELECCIONADOS
                val contactsList = userDoc.get("contacts") as? List<Map<String, String>> ?: emptyList()

                Log.d("TravelLocationService", "Contactos obtenidos desde Firestore: ${contactsList.size}")

                // Obtener información del viaje actual
                val viajeSnapshot = realtimeDb.getReference("viajes/$userUid/info").get().await()
                val destino = viajeSnapshot.child("destino").getValue(String::class.java) ?: "Destino no especificado"
                val tiempoEstimado = viajeSnapshot.child("tiempoEstimado").getValue(String::class.java) ?: "Tiempo no especificado"

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val startTime = dateFormat.format(Date())

                Log.d("TravelLocationService", "Info viaje - Destino: $destino, Tiempo: $tiempoEstimado")
                Log.d("TravelLocationService", "Contactos a notificar: ${contactsList.size}")

                // 🔔 NOTIFICACIÓN PARA EL USUARIO ACTUAL (TÚ)
                val selfNotificationData = mapOf(
                    "type" to "trip_started",
                    "userName" to "Tú",
                    "userUid" to userUid,
                    "destination" to destino,
                    "estimatedTime" to tiempoEstimado,
                    "startTime" to startTime,
                    "timestamp" to System.currentTimeMillis(),
                    "read" to false
                )

                realtimeDb.getReference("notifications/$userUid")
                    .push()
                    .setValue(selfNotificationData)
                    .await()

                Log.d("TravelLocationService", "✅ Notificación creada para el usuario actual")

                // 🔔 NOTIFICAR A CONTACTOS SELECCIONADOS CON ENLACE DE MAPA
                for ((index, contact) in contactsList.withIndex()) {
                    val contactName = contact["nombre"] ?: "Contacto"
                    val contactPhone = contact["telefono"] ?: ""

                    Log.d("TravelLocationService", "Procesando contacto ${index + 1}/${contactsList.size}: $contactName - $contactPhone")

                    // Validar que el contacto tenga teléfono
                    if (contactPhone.isNotEmpty()) {
                        // Generar enlace de Google Maps (se actualizará con la ubicación real más tarde)
                        val mapsLink = "https://maps.google.com/maps?q=ubicacion+en+tiempo+real"

                        val notificationData = mapOf(
                            "type" to "trip_started",
                            "userName" to userName,
                            "userUid" to userUid,
                            "destination" to destino,
                            "estimatedTime" to tiempoEstimado,
                            "startTime" to startTime,
                            "timestamp" to System.currentTimeMillis(),
                            "read" to false,
                            "mapsLink" to mapsLink,
                            "hasLiveLocation" to true
                        )

                        val notificationRef = realtimeDb.getReference("notifications/$contactPhone")
                            .push()

                        notificationRef.setValue(notificationData).await()

                        Log.d("TravelLocationService", "✅ Notificación enviada a: $contactName ($contactPhone)")
                        Log.d("TravelLocationService", "   Ruta: notifications/$contactPhone/${notificationRef.key}")
                    } else {
                        Log.w("TravelLocationService", "⚠️ Contacto sin teléfono: $contactName")
                    }
                }

                // Si no hay contactos seleccionados, loguear advertencia
                if (contactsList.isEmpty()) {
                    Log.w("TravelLocationService", "ℹ️ No hay contactos de emergencia seleccionados")
                } else {
                    Log.d("TravelLocationService", "✅ Proceso de notificación completado. Total notificados: ${contactsList.size}")
                }

            } catch (e: Exception) {
                Log.e("TravelLocationService", "❌ Error notificando contactos: ${e.message}", e)
            }
        }
    }

    /**
     * Función de debug para verificar contactos en Firestore
     */
    private suspend fun debugUserContacts(uid: String) {
        try {
            Log.d("DebugUtils", "========================================")
            Log.d("DebugUtils", "=== DEBUG CONTACTOS DE USUARIO ===")
            Log.d("DebugUtils", "========================================")
            Log.d("DebugUtils", "Usuario UID: $uid")

            val userDoc = db.collection("users").document(uid).get().await()

            if (!userDoc.exists()) {
                Log.e("DebugUtils", "❌ El documento del usuario NO EXISTE en Firestore")
                return
            }

            Log.d("DebugUtils", "✅ Documento de usuario encontrado")

            val userName = userDoc.getString("name")
            val userEmail = userDoc.getString("email")
            Log.d("DebugUtils", "Nombre: $userName")
            Log.d("DebugUtils", "Email: $userEmail")

            val contacts = userDoc.get("contacts")

            if (contacts == null) {
                Log.w("DebugUtils", "⚠️ Campo 'contacts' es NULL")
                return
            }

            when (contacts) {
                is List<*> -> {
                    Log.d("DebugUtils", "✅ Campo 'contacts' es una Lista")
                    Log.d("DebugUtils", "Total contactos en Firestore: ${contacts.size}")
                    Log.d("DebugUtils", "----------------------------------------")

                    if (contacts.isEmpty()) {
                        Log.w("DebugUtils", "⚠️ La lista de contactos está VACÍA")
                    } else {
                        contacts.forEachIndexed { index, contact ->
                            try {
                                val contactMap = contact as? Map<*, *>
                                if (contactMap != null) {
                                    val nombre = contactMap["nombre"] as? String
                                    val telefono = contactMap["telefono"] as? String

                                    Log.d("DebugUtils", "Contacto ${index + 1}:")
                                    Log.d("DebugUtils", "  • Nombre: $nombre")
                                    Log.d("DebugUtils", "  • Teléfono: $telefono")

                                    if (nombre.isNullOrEmpty()) {
                                        Log.w("DebugUtils", "  ⚠️ Nombre vacío o null")
                                    }
                                    if (telefono.isNullOrEmpty()) {
                                        Log.w("DebugUtils", "  ⚠️ Teléfono vacío o null")
                                    }
                                } else {
                                    Log.e("DebugUtils", "  ❌ Contacto ${index + 1} NO es un Map válido")
                                }
                            } catch (e: Exception) {
                                Log.e("DebugUtils", "  ❌ Error procesando contacto ${index + 1}: ${e.message}")
                            }
                        }
                    }
                }
                else -> {
                    Log.e("DebugUtils", "❌ Campo 'contacts' NO es una Lista. Tipo: ${contacts::class.simpleName}")
                }
            }

            Log.d("DebugUtils", "========================================")
            Log.d("DebugUtils", "=== FIN DEBUG ===")
            Log.d("DebugUtils", "========================================")
        } catch (e: Exception) {
            Log.e("DebugUtils", "❌ Error en debugUserContacts: ${e.message}", e)
        }
    }

    private fun notifyContactsWithLocation(location: Location) {
        val user = auth.currentUser ?: return
        val userUid = user.uid

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener contactos del usuario
                val userDoc = db.collection("users").document(userUid).get().await()
                val contactsList = userDoc.get("contacts") as? List<Map<String, String>> ?: emptyList()
                val userName = userDoc.getString("name") ?: "Usuario SafeRoute"

                Log.d("TravelLocationService", "Actualizando ubicación para ${contactsList.size} contactos")

                // Generar enlace de Google Maps con la ubicación actual
                val mapsLink = "https://maps.google.com/maps?q=${location.latitude},${location.longitude}&z=15"

                // 🔔 NOTIFICACIÓN DE UBICACIÓN PARA EL USUARIO ACTUAL (TÚ)
                val selfLocationNotification = mapOf(
                    "type" to "location_update",
                    "userName" to "Tú",
                    "userUid" to userUid,
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "timestamp" to System.currentTimeMillis(),
                    "speed" to location.speed,
                    "accuracy" to location.accuracy,
                    "read" to false,
                    "mapsLink" to mapsLink
                )

                realtimeDb.getReference("notifications/$userUid")
                    .push()
                    .setValue(selfLocationNotification)

                Log.d("TravelLocationService", "✅ Notificación de ubicación creada para usuario actual")

                // 🔔 COMPARTIR UBICACIÓN CON CONTACTOS SELECCIONADOS
                for (contact in contactsList) {
                    val contactPhone = contact["telefono"] ?: ""
                    val contactName = contact["nombre"] ?: "Contacto"

                    if (contactPhone.isNotEmpty()) {
                        val locationNotification = mapOf(
                            "type" to "location_update",
                            "userName" to userName,
                            "userUid" to userUid,
                            "latitude" to location.latitude,
                            "longitude" to location.longitude,
                            "timestamp" to System.currentTimeMillis(),
                            "speed" to location.speed,
                            "accuracy" to location.accuracy,
                            "read" to false,
                            "mapsLink" to mapsLink
                        )

                        // Guardar como notificación
                        realtimeDb.getReference("notifications/$contactPhone")
                            .push()
                            .setValue(locationNotification)

                        // También guardar en shared_locations para acceso rápido
                        val locationData = mapOf(
                            "type" to "location_update",
                            "userName" to userName,
                            "userUid" to userUid,
                            "latitude" to location.latitude,
                            "longitude" to location.longitude,
                            "timestamp" to System.currentTimeMillis(),
                            "speed" to location.speed,
                            "accuracy" to location.accuracy,
                            "mapsLink" to mapsLink
                        )

                        realtimeDb.getReference("shared_locations/$contactPhone/$userUid")
                            .setValue(locationData)
                            .addOnSuccessListener {
                                Log.d("TravelLocationService", "Ubicación compartida con: $contactName ($contactPhone)")
                            }
                            .addOnFailureListener { e ->
                                Log.e("TravelLocationService", "Error compartiendo ubicación con $contactName: ${e.message}")
                            }
                    }
                }

            } catch (e: Exception) {
                Log.e("TravelLocationService", "Error compartiendo ubicación: ${e.message}", e)
            }
        }
    }

    private fun startLocationUpdates() {
        Log.d("TravelLocationService", "Iniciando actualizaciones de ubicación")

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
            Log.w("TravelLocationService", "❌ Permisos de ubicación no concedidos")
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        Log.d("TravelLocationService", "✅ Actualizaciones de ubicación configuradas")
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
                Log.d("TravelLocationService", "✅ Ubicación enviada a Firebase: (${location.latitude}, ${location.longitude})")
            }
            .addOnFailureListener {
                Log.e("TravelLocationService", "❌ Error al enviar ubicación: ${it.message}")
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
            Log.d("TravelLocationService", "Canal de notificación creado")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("TravelLocationService", "onDestroy() - Servicio detenido")

        // Notificar a contactos que el viaje finalizó
        notifyContactsTripEnded()
    }

    private fun notifyContactsTripEnded() {
        val user = auth.currentUser ?: return
        val userUid = user.uid

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("TravelLocationService", "Notificando fin de viaje para UID: $userUid")

                val userDoc = db.collection("users").document(userUid).get().await()
                val contactsList = userDoc.get("contacts") as? List<Map<String, String>> ?: emptyList()
                val userName = userDoc.getString("name") ?: "Usuario SafeRoute"

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val endTime = dateFormat.format(Date())

                Log.d("TravelLocationService", "Notificando fin de viaje a ${contactsList.size} contactos")

                // 🔔 NOTIFICACIÓN DE FIN DE VIAJE PARA EL USUARIO ACTUAL (TÚ)
                val selfEndNotification = mapOf(
                    "type" to "trip_ended",
                    "userName" to "Tú",
                    "userUid" to userUid,
                    "endTime" to endTime,
                    "timestamp" to System.currentTimeMillis(),
                    "read" to false
                )

                realtimeDb.getReference("notifications/$userUid")
                    .push()
                    .setValue(selfEndNotification)
                    .await()

                Log.d("TravelLocationService", "✅ Notificación de fin creada para el usuario actual")

                // 🔔 NOTIFICAR A CONTACTOS SELECCIONADOS
                for ((index, contact) in contactsList.withIndex()) {
                    val contactPhone = contact["telefono"] ?: ""
                    val contactName = contact["nombre"] ?: "Contacto"

                    if (contactPhone.isNotEmpty()) {
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
                            .addOnSuccessListener {
                                Log.d("TravelLocationService", "✅ Notificación de fin enviada a: $contactName ($contactPhone)")
                            }
                            .addOnFailureListener { e ->
                                Log.e("TravelLocationService", "❌ Error enviando notificación a $contactName: ${e.message}")
                            }
                    }
                }

                Log.d("TravelLocationService", "✅ Proceso de notificación de fin completado")

            } catch (e: Exception) {
                Log.e("TravelLocationService", "❌ Error notificando fin de viaje: ${e.message}", e)
            }
        }
    }

    private fun updateUserLastLocation(location: Location) {
        val uid = auth.currentUser?.uid ?: return

        val updates = mapOf(
            "lastLat" to location.latitude,
            "lastLon" to location.longitude
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update(updates)
            .addOnSuccessListener {
                Log.d("TravelLocationService", "lastLat/lastLon actualizados en Firestore")
            }
            .addOnFailureListener {
                Log.e("TravelLocationService", "Error actualizando ubicación en Firestore: ${it.message}")
            }
    }



    override fun onBind(intent: Intent?): IBinder? = null
}