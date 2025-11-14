package com.example.saferouter.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.material.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saferouter.R
import com.example.saferouter.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen(
    db: FirebaseFirestore,
    navigateToProfile: () -> Unit,
    navigateToContacts: () -> Unit,
    navigateToTripTracking: () -> Unit,
    navigateToNotifications: () -> Unit,
    navigateToReportarIncidente: () -> Unit,
    navigateToMapaComunitario: () -> Unit,
    navigateToMiDashboard: () -> Unit, // <-- 1. PARÁMETRO NUEVO AÑADIDO
    onLogout: () -> Unit,
    navigateToNotificationSettings: () -> Unit

) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PrimaryBlueLight, BackgroundWhite),
                    startY = 0f,
                    endY = 800f
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo de la app
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "SafeRoute Logo",
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(20.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Título de bienvenida
        Text(
            text = "SafeRoute",
            color = PrimaryBlueDark,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
            text = "Muevete Seguro",
            color = PrimaryBlueDark,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        // Card para opciones
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            elevation = 12.dp,
            shape = RoundedCornerShape(20.dp),
            backgroundColor = BackgroundWhite.copy(alpha = 0.9f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Botón Ir a mi perfil
                Button(
                    onClick = navigateToProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Text(
                        text = "Mi perfil",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón Contactos
                Button(
                    onClick = navigateToContacts,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Text(
                        text = "Contactos de emergencia",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón Seguimiento de Viaje
                Button(
                    onClick = navigateToTripTracking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Text(
                        text = "Seguimiento de Viaje",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón Reportar Incidente
                Button(
                    onClick = navigateToReportarIncidente,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Text(
                        text = "📝 Reportar Incidente",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón Mapa Comunitario
                Button(
                    onClick = navigateToMapaComunitario,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Text(
                        text = "🗺️ Mapa Comunitario",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- 👇 2. BOTÓN NUEVO AÑADIDO 👇 ---
                Button(
                    onClick = navigateToMiDashboard, // <-- Usa el nuevo parámetro
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = SuccessGreen), // Color diferente
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Text(
                        text = "📊 Mi Dashboard",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                // --- FIN DEL BOTÓN NUEVO ---

                Spacer(modifier = Modifier.height(12.dp))

                // Botón Notificaciones
                Button(
                    onClick = navigateToNotifications,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Text(
                        text = "Notificaciones",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- 🔔 NUEVO BOTÓN: Configurar Notificaciones ---
                Button(
                    onClick = navigateToNotificationSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp)  // 👈 ya tienes padding general, evita doble padding
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_notifications),
                        contentDescription = "Configuración Notificaciones"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⚙️ Configurar Notificaciones", color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Boton de salir
                Button(onClick = { onLogout() }) {
                    Text("Cerrar sesión")
                }
            }
        }

        // Texto informativo adicional
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Tu seguridad es nuestra prioridad",
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 20.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        db = FirebaseFirestore.getInstance(),
        navigateToProfile = {},
        navigateToContacts = {},
        navigateToTripTracking = {},
        navigateToNotifications = {},
        navigateToReportarIncidente = {},
        navigateToMapaComunitario = {},
        navigateToMiDashboard = {},
        navigateToNotificationSettings = {}, // 👈 AGREGA ESTO
        onLogout = {}
    )
}
