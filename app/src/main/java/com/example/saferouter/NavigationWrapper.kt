package com.example.saferouter

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.saferouter.data.ContactosScreen
import com.example.saferouter.data.MapaComunitarioScreen
import com.example.saferouter.data.MiDashboardScreen
import com.example.saferouter.data.NotificationsScreen
import com.example.saferouter.data.PerfilScreen
import com.example.saferouter.data.ReportarIncidenteScreen
import com.example.saferouter.data.ReportesComunitarios
import com.example.saferouter.data.RutasSegurasScreen
import com.example.saferouter.data.SeguimientoViajeScreen
import com.example.saferouter.firebase.AuthManager
import com.example.saferouter.presentation.home.HomeScreen
import com.example.saferouter.presentation.initial.InitialScreen
import com.example.saferouter.presentation.login.LoginScreen
import com.example.saferouter.presentation.reset.ResetPasswordScreen
import com.example.saferouter.presentation.signup.SignUpScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.saferouter.presentation.settings.NotificationSettingsScreen
import com.example.saferouter.presentation.bot.BotScreen
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavigationWrapper(
    navHostController: NavHostController,
    auth: FirebaseAuth,
    db: FirebaseFirestore
) {
    val context = LocalContext.current
    var startDestination by remember { mutableStateOf<String?>(null) }

    // Estado para la ubicación actual
    var currentLat by remember { mutableStateOf<Double?>(null) }
    var currentLng by remember { mutableStateOf<Double?>(null) }

    // Obtener ubicación actual
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLat = it.latitude
                    currentLng = it.longitude
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            startDestination = if (user != null) "home" else "initial"
        }

        auth.addAuthStateListener(authStateListener)

        onDispose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    if (startDestination == null) return

    NavHost(navController = navHostController, startDestination = startDestination!!) {
        composable(route = "notificationSettings") {
            NotificationSettingsScreen(
                navigateBack = { navHostController.popBackStack() }
            )
        }

        composable("initial") {
            InitialScreen(
                navigateToLogin = { navHostController.navigate("logIn") },
                navigateToSignUp = { navHostController.navigate("signUp") }
            )
        }

        composable("logIn") {
            LoginScreen(
                auth = auth,
                navigateToHome = {
                    navHostController.navigate("home") {
                        popUpTo("initial") { inclusive = true }
                    }
                },
                navigateToReset = {
                    navHostController.navigate("resetPassword")
                },
                navigateBack = { navHostController.popBackStack() }
            )
        }

        composable("signUp") {
            SignUpScreen(auth)
        }

        composable("home") {
            HomeScreen(
                db = FirebaseFirestore.getInstance(),
                navigateToProfile = { navHostController.navigate("profile") },
                navigateToContacts = { navHostController.navigate("contacts") },
                navigateToTripTracking = { navHostController.navigate("trip_tracking") },
                navigateToNotifications = { navHostController.navigate("notifications") },
                navigateToBot = { navHostController.navigate("bot") },
                navigateToReportarIncidente = { navHostController.navigate("reportar_incidente") },
                navigateToMapaComunitario = { navHostController.navigate("mapaComunitario") },
                navigateToMiDashboard = { navHostController.navigate("mi_dashboard") },
                navigateToNotificationSettings = { navHostController.navigate("notificationSettings") },
                onLogout = {
                    AuthManager.logout()
                    Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                    navHostController.navigate("logIn") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("profile") {
            PerfilScreen(
                db = FirebaseFirestore.getInstance(),
                navigateToContacts = { navHostController.navigate("contacts") },
                navigateBack = { navHostController.popBackStack() }
            )
        }

        composable("contacts") {
            ContactosScreen(
                db = FirebaseFirestore.getInstance(),
                navigateBack = { navHostController.popBackStack() }
            )
        }

        composable("trip_tracking") {
            SeguimientoViajeScreen(
                db = FirebaseFirestore.getInstance(),
                navigateBack = { navHostController.popBackStack() },
                navigateToRutasSeguras = { navHostController.navigate("rutas_seguras") },
                context = context
            )
        }

        composable("rutas_seguras") {
            RutasSegurasScreen(
                navigateBack = { navHostController.popBackStack() }
            )
        }

        composable("mi_dashboard") {
            MiDashboardScreen(
                db = db,
                auth = auth,
                navigateBack = { navHostController.popBackStack() },
                navigateToHeatmap = { navHostController.navigate("heatmap") },
                context = context
            )
        }

        composable("reportar_incidente") {
            ReportarIncidenteScreen(
                db = FirebaseFirestore.getInstance(),
                navigateBack = { navHostController.popBackStack() },
                context = context
            )
        }

        composable("mapaComunitario") {
            MapaComunitarioScreen(
                db = FirebaseFirestore.getInstance(),
                navigateBack = { navHostController.popBackStack() },
                navigateToReportes = { navHostController.navigate("reportesComunitarios") },
                context = context
            )
        }

        composable("reportesComunitarios") {
            ReportesComunitarios(
                db = FirebaseFirestore.getInstance(),
                navigateBack = { navHostController.popBackStack() },
                context = context
            )
        }

        composable("resetPassword") {
            ResetPasswordScreen(
                auth = auth,
                navigateBack = { navHostController.popBackStack() }
            )
        }

        composable("notifications") {
            NotificationsScreen(
                navigateBack = { navHostController.popBackStack() }
            )
        }

        // 🔥 CORRECCIÓN: Pasar los parámetros de ubicación
        composable("bot") {
            BotScreen(
                navigateBack = { navHostController.popBackStack() },
                currentLat = currentLat,
                currentLng = currentLng
            )
        }
    }
}