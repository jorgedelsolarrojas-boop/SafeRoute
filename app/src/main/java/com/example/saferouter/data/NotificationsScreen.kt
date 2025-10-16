package com.example.saferouter.data

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saferouter.R
import com.example.saferouter.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class Notification(
    val id: String = "",
    val type: String = "",
    val userName: String = "",
    val userUid: String = "",
    val destination: String = "",
    val estimatedTime: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val timestamp: Long = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val read: Boolean = false,
    val mapsLink: String = "",
    val hasLiveLocation: Boolean = false
)

@Composable
fun NotificationsScreen(
    navigateBack: () -> Unit
) {
    val context = LocalContext.current
    val notifications = remember { mutableStateOf<List<Notification>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // Usar el UID del usuario actual para buscar notificaciones
    val currentUserId = remember {
        mutableStateOf(FirebaseAuth.getInstance().currentUser?.uid ?: "")
    }

    LaunchedEffect(Unit) {
        loadNotifications(currentUserId.value, notifications, isLoading)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = navigateBack,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back_24),
                    contentDescription = "Back",
                    tint = PrimaryBlueDark
                )
            }
            Text(
                text = "Notificaciones de Viajes",
                color = PrimaryBlueDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        if (isLoading.value) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (notifications.value.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_notifications),
                        contentDescription = "Sin notificaciones",
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay notificaciones",
                        color = TextSecondary,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Recibirás alertas cuando tus contactos inicien viajes",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications.value.sortedByDescending { it.timestamp }) { notification ->
                    NotificationCard(
                        notification = notification,
                        onMarkAsRead = { notificationId ->
                            coroutineScope.launch {
                                markNotificationAsRead(currentUserId.value, notificationId)
                            }
                        }
                    )
                }
            }
        }

        // Botón para limpiar todas las notificaciones
        if (notifications.value.isNotEmpty()) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        clearAllNotifications(currentUserId.value, notifications)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = AlertRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Limpiar Todas las Notificaciones",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: Notification,
    onMarkAsRead: (String) -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = if (notification.read) BackgroundWhite else PrimaryBlueLight.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            // Header con tipo de notificación y tiempo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (notification.type) {
                        "trip_started" -> "🚗 Viaje Iniciado"
                        "trip_ended" -> "✅ Viaje Finalizado"
                        "location_update" -> "📍 Ubicación Actualizada"
                        else -> "🔔 Notificación"
                    },
                    color = PrimaryBlueDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = formatTimestamp(notification.timestamp),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contenido de la notificación
            when (notification.type) {
                "trip_started" -> {
                    Text(
                        text = "${notification.userName} ha iniciado un viaje",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🎯 Destino: ${notification.destination}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "⏱️ Tiempo estimado: ${notification.estimatedTime}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "🕐 Hora de inicio: ${notification.startTime}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    // Mostrar botón de Maps si está disponible
                    if (notification.mapsLink.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(notification.mapsLink))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No se pudo abrir Google Maps", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = SuccessGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "🗺️ Ver en Google Maps",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                "trip_ended" -> {
                    Text(
                        text = "${notification.userName} ha finalizado su viaje",
                        color = SuccessGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🕐 Hora de finalización: ${notification.endTime}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                "location_update" -> {
                    Text(
                        text = "${notification.userName} actualizó su ubicación",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📍 Lat: ${"%.6f".format(notification.latitude)}, Lng: ${"%.6f".format(notification.longitude)}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    // Botón para ver en Maps
                    if (notification.mapsLink.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(notification.mapsLink))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No se pudo abrir Google Maps", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "🗺️ Ver ubicación en Maps",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Botón para marcar como leído si no lo está
            if (!notification.read) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onMarkAsRead(notification.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Marcar como leído",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun loadNotifications(
    userPhone: String,
    notifications: androidx.compose.runtime.MutableState<List<Notification>>,
    isLoading: androidx.compose.runtime.MutableState<Boolean>
) {
    if (userPhone.isEmpty()) {
        isLoading.value = false
        return
    }

    val database = FirebaseDatabase.getInstance()
    val notificationsRef = database.getReference("notifications/$userPhone")

    notificationsRef.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val notificationList = mutableListOf<Notification>()

            for (child in snapshot.children) {
                try {
                    val notification = Notification(
                        id = child.key ?: "",
                        type = child.child("type").getValue(String::class.java) ?: "",
                        userName = child.child("userName").getValue(String::class.java) ?: "Usuario",
                        userUid = child.child("userUid").getValue(String::class.java) ?: "",
                        destination = child.child("destination").getValue(String::class.java) ?: "",
                        estimatedTime = child.child("estimatedTime").getValue(String::class.java) ?: "",
                        startTime = child.child("startTime").getValue(String::class.java) ?: "",
                        endTime = child.child("endTime").getValue(String::class.java) ?: "",
                        timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0,
                        latitude = child.child("latitude").getValue(Double::class.java) ?: 0.0,
                        longitude = child.child("longitude").getValue(Double::class.java) ?: 0.0,
                        read = child.child("read").getValue(Boolean::class.java) ?: false,
                        mapsLink = child.child("mapsLink").getValue(String::class.java) ?: "",
                        hasLiveLocation = child.child("hasLiveLocation").getValue(Boolean::class.java) ?: false
                    )
                    notificationList.add(notification)
                } catch (e: Exception) {
                    // Ignorar notificaciones con formato incorrecto
                }
            }

            notifications.value = notificationList
            isLoading.value = false
        }

        override fun onCancelled(error: DatabaseError) {
            isLoading.value = false
        }
    })
}

private fun markNotificationAsRead(userPhone: String, notificationId: String) {
    val database = FirebaseDatabase.getInstance()
    val notificationRef = database.getReference("notifications/$userPhone/$notificationId/read")
    notificationRef.setValue(true)
}

private fun clearAllNotifications(
    userPhone: String,
    notifications: androidx.compose.runtime.MutableState<List<Notification>>
) {
    val database = FirebaseDatabase.getInstance()
    val notificationsRef = database.getReference("notifications/$userPhone")
    notificationsRef.removeValue()
    notifications.value = emptyList()
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return format.format(date)
}

@Preview
@Composable
fun NotificationsScreenPreview() {
    NotificationsScreen(navigateBack = {})
}

@Preview
@Composable
fun NotificationCardPreview() {
    val sampleNotification = Notification(
        type = "trip_started",
        userName = "María García",
        destination = "Centro Comercial",
        estimatedTime = "15 minutos",
        startTime = "13:45",
        timestamp = System.currentTimeMillis(),
        read = false,
        mapsLink = "https://maps.google.com/maps?q=-12.0464,-77.0428&z=15"
    )

    NotificationCard(
        notification = sampleNotification,
        onMarkAsRead = {}
    )
}