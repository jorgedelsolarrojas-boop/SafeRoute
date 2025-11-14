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
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class Notification(
    val id: String = "",
    val ownerId: String = "",   // ★ NUEVO: usuario dueño real de esta notificación
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
    val notifications = remember { mutableStateOf<List<Notification>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loadNotifications(notifications, isLoading)
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
                text = "Notificaciones",
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
            EmptyNotificationsUI()
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
                        onMarkAsRead = { ownerId, notificationId ->
                            coroutineScope.launch {
                                markNotificationAsRead(ownerId, notificationId)
                            }
                        }
                    )
                }
            }
        }

        // Botón para limpiar TODAS las notificaciones del servidor
        if (notifications.value.isNotEmpty()) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        clearAllNotifications(notifications)
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
                    text = "ELIMINAR TODAS LAS NOTIFICACIONES",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyNotificationsUI() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.ic_notifications),
                contentDescription = "Sin notificaciones",
                tint = TextSecondary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("No hay notificaciones", color = TextSecondary, fontSize = 18.sp)
        }
    }
}

@Composable
fun NotificationCard(
    notification: Notification,
    onMarkAsRead: (String, String) -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = if (notification.read)
            BackgroundWhite
        else PrimaryBlueLight.copy(alpha = 0.1f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
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

            Spacer(Modifier.height(8.dp))

            // Contenido (igual que antes)
            NotificationContent(notification, context)

            if (!notification.read) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onMarkAsRead(notification.ownerId, notification.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Marcar como leído", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun NotificationContent(notification: Notification, context: android.content.Context) {
    when (notification.type) {
        "trip_started" -> {
            Text("${notification.userName} ha iniciado un viaje",
                color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(4.dp))
            Text("🎯 Destino: ${notification.destination}", color = TextSecondary, fontSize = 12.sp)
            Text("⏱️ Tiempo estimado: ${notification.estimatedTime}", color = TextSecondary, fontSize = 12.sp)
            Text("🕐 Inicio: ${notification.startTime}", color = TextSecondary, fontSize = 12.sp)
        }

        "trip_ended" -> {
            Text("${notification.userName} ha finalizado su viaje",
                color = SuccessGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(4.dp))
            Text("🕐 Fin: ${notification.endTime}", color = TextSecondary, fontSize = 12.sp)
        }

        "location_update" -> {
            Text("${notification.userName} actualizó su ubicación",
                color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(4.dp))
            Text(
                "📍 Lat: ${"%.6f".format(notification.latitude)}, Lng: ${"%.6f".format(notification.longitude)}",
                color = TextSecondary, fontSize = 12.sp
            )
        }
    }

    if (notification.mapsLink.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(notification.mapsLink))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
            Text("🗺️ Ver en Google Maps", color = Color.White, fontSize = 12.sp)
        }
    }
}

private fun loadNotifications(
    notifications: MutableState<List<Notification>>,
    isLoading: MutableState<Boolean>
) {
    val ref = FirebaseDatabase.getInstance().getReference("notifications")

    ref.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val list = mutableListOf<Notification>()

            for (userNode in snapshot.children) {          // userId
                for (child in userNode.children) {         // notificationId
                    try {
                        val n = Notification(
                            id = child.key ?: "",
                            ownerId = userNode.key ?: "",  // ★ quién es el dueño real
                            type = child.child("type").getValue(String::class.java) ?: "",
                            userName = child.child("userName").getValue(String::class.java) ?: "",
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
                        list.add(n)
                    } catch (e: Exception) { }
                }
            }

            notifications.value = list
            isLoading.value = false
        }

        override fun onCancelled(error: DatabaseError) {
            isLoading.value = false
        }
    })
}

private fun markNotificationAsRead(ownerId: String, notificationId: String) {
    val ref = FirebaseDatabase.getInstance()
        .getReference("notifications/$ownerId/$notificationId/read")

    ref.setValue(true)
}

private fun clearAllNotifications(
    notifications: MutableState<List<Notification>>
) {
    val ref = FirebaseDatabase.getInstance().getReference("notifications")
    ref.removeValue()
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
