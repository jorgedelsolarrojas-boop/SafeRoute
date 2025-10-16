package com.example.saferouter

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.saferouter.data.ContactosScreen
import com.example.saferouter.data.PerfilScreen
import com.example.saferouter.data.ReportarIncidenteScreen
import com.example.saferouter.data.SeguimientoViajeScreen
import com.example.saferouter.firebase.AuthManager
import com.example.saferouter.presentation.home.HomeScreen
import com.example.saferouter.presentation.initial.InitialScreen
import com.example.saferouter.presentation.login.LoginScreen
import com.example.saferouter.presentation.reset.ResetPasswordScreen
import com.example.saferouter.presentation.signup.SignUpScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun NavigationWrapper(
    navHostController: NavHostController,
    auth: FirebaseAuth,
    db: FirebaseFirestore
) {
    val context = LocalContext.current
    var startDestination by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            startDestination = if (user != null) "home" else "initial"
        }

        // 🔹 Agregamos el listener
        auth.addAuthStateListener(authStateListener)

        // 🔹 Limpiamos al salir del composable
        onDispose {
            auth.removeAuthStateListener(authStateListener)
        }
    }



    // Mientras Firebase aún carga el estado del usuario (unos ms), no dibujamos nada
    if (startDestination == null) return

    NavHost(navController = navHostController, startDestination = startDestination!!) {

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
                }
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
                navigateToIncidentReport = { navHostController.navigate("incident_report") },
                onLogout = {
                    AuthManager.logout()
                    Toast.makeText(context, "✅ Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
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
                context = context // PASAR EL CONTEXTO AQUÍ
            )
        }

        composable("incident_report") {
            ReportarIncidenteScreen(
                navigateBack = { navHostController.popBackStack() }
            )
        }

        composable("resetPassword") {
            ResetPasswordScreen(
                auth = auth,
                navigateBack = { navHostController.popBackStack() }
            )
        }
    }
}
