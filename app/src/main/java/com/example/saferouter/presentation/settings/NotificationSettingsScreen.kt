package com.example.saferouter.presentation.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.saferouter.R
import com.example.saferouter.data.preferences.NotificationPreferencesManager
import com.example.saferouter.ui.theme.*
import com.example.saferouter.worker.ZoneMonitorWorker
import kotlinx.coroutines.launch

@Composable
fun NotificationSettingsScreen(
    navigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val preferencesManager = remember { NotificationPreferencesManager(context) }

    // Estados para las preferencias
    val notificationsEnabled by preferencesManager.notificationsEnabled.collectAsState(initial = true)
    val alertRadius by preferencesManager.alertRadius.collectAsState(initial = 500)
    val soundEnabled by preferencesManager.soundEnabled.collectAsState(initial = true)
    val vibrationEnabled by preferencesManager.vibrationEnabled.collectAsState(initial = true)
    val arrivalNotificationEnabled by preferencesManager.arrivalNotificationEnabled.collectAsState(initial = true)

    // Estados para tipos de incidentes
    val notifyRobo by preferencesManager.notifyRobo.collectAsState(initial = true)
    val notifyAsalto by preferencesManager.notifyAsalto.collectAsState(initial = true)
    val notifyCongestion by preferencesManager.notifyCongestion.collectAsState(initial = true)
    val notifyIluminacion by preferencesManager.notifyIluminacion.collectAsState(initial = true)
    val notifyHuelga by preferencesManager.notifyHuelga.collectAsState(initial = true)
    val notifyOtro by preferencesManager.notifyOtro.collectAsState(initial = true)

    // Launcher para permiso de notificaciones (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(context, "Permiso de notificaciones otorgado", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // Verificar y solicitar permiso al inicio
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD),
                        Color(0xFFFFFFFF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
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
                    text = "Configuración de Notificaciones",
                    color = PrimaryBlueDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Contenido scrolleable
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                // Sección: Activar/Desactivar Notificaciones
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🔔 Notificaciones Generales",
                            color = PrimaryBlueDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Activar notificaciones",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Recibe alertas de zonas peligrosas",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        preferencesManager.setNotificationsEnabled(enabled)
                                        if (enabled) {
                                            ZoneMonitorWorker.startPeriodicMonitoring(context)
                                            Toast.makeText(
                                                context,
                                                "Monitoreo activado",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            ZoneMonitorWorker.stopPeriodicMonitoring(context)
                                            Toast.makeText(
                                                context,
                                                "Monitoreo desactivado",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SuccessGreen,
                                    checkedTrackColor = SuccessGreen.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }

                // Sección: Radio de Alerta
                if (notificationsEnabled) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = Color.White
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "📍 Radio de Alerta",
                                color = PrimaryBlueDark,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = "Notificar cuando esté a: ${alertRadius}m",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Slider(
                                value = alertRadius.toFloat(),
                                onValueChange = { newValue ->
                                    scope.launch {
                                        preferencesManager.setAlertRadius(newValue.toInt())
                                    }
                                },
                                valueRange = 100f..1000f,
                                steps = 8,
                                colors = SliderDefaults.colors(
                                    thumbColor = PrimaryBlue,
                                    activeTrackColor = PrimaryBlue
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("100m", color = TextSecondary, fontSize = 12.sp)
                                Text("1000m", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    // Sección: Tipos de Notificaciones
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = Color.White
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "⚠️ Tipos de Incidentes",
                                color = PrimaryBlueDark,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = "Selecciona qué tipos de incidentes deseas recibir",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Lista de tipos
                            NotificationTypeSwitch(
                                emoji = "🚨",
                                label = "Robo",
                                checked = notifyRobo,
                                onCheckedChange = { scope.launch { preferencesManager.setNotifyRobo(it) } }
                            )
                            NotificationTypeSwitch(
                                emoji = "⚠️",
                                label = "Asalto",
                                checked = notifyAsalto,
                                onCheckedChange = { scope.launch { preferencesManager.setNotifyAsalto(it) } }
                            )
                            NotificationTypeSwitch(
                                emoji = "🚗",
                                label = "Alta congestión vehicular",
                                checked = notifyCongestion,
                                onCheckedChange = { scope.launch { preferencesManager.setNotifyCongestion(it) } }
                            )
                            NotificationTypeSwitch(
                                emoji = "💡",
                                label = "Baja iluminación de la zona",
                                checked = notifyIluminacion,
                                onCheckedChange = { scope.launch { preferencesManager.setNotifyIluminacion(it) } }
                            )
                            NotificationTypeSwitch(
                                emoji = "✊",
                                label = "Huelga",
                                checked = notifyHuelga,
                                onCheckedChange = { scope.launch { preferencesManager.setNotifyHuelga(it) } }
                            )
                            NotificationTypeSwitch(
                                emoji = "📌",
                                label = "Otro",
                                checked = notifyOtro,
                                onCheckedChange = { scope.launch { preferencesManager.setNotifyOtro(it) } }
                            )
                        }
                    }

                    // Sección: Alertas Sonoras y Vibración
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = Color.White
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "🔊 Alertas Personalizables",
                                color = PrimaryBlueDark,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔔 Sonido de alerta",
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                                Switch(
                                    checked = soundEnabled,
                                    onCheckedChange = {
                                        scope.launch { preferencesManager.setSoundEnabled(it) }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PrimaryBlue,
                                        checkedTrackColor = PrimaryBlue.copy(alpha = 0.5f)
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📳 Vibración",
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                                Switch(
                                    checked = vibrationEnabled,
                                    onCheckedChange = {
                                        scope.launch { preferencesManager.setVibrationEnabled(it) }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PrimaryBlue,
                                        checkedTrackColor = PrimaryBlue.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }

                    // Sección: Notificación de Llegada
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = Color.White
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "✅ Llegada Segura",
                                color = PrimaryBlueDark,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Notificación de llegada",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Te notificaremos al llegar a tu destino",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Switch(
                                    checked = arrivalNotificationEnabled,
                                    onCheckedChange = {
                                        scope.launch { preferencesManager.setArrivalNotificationEnabled(it) }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = SuccessGreen,
                                        checkedTrackColor = SuccessGreen.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun NotificationTypeSwitch(
    emoji: String,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$emoji $label",
            color = TextPrimary,
            fontSize = 14.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PrimaryBlue,
                checkedTrackColor = PrimaryBlue.copy(alpha = 0.5f)
            )
        )
    }
}