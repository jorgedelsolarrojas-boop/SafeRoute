package com.example.saferouter

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.saferouter.firebase.AuthManager
import com.example.saferouter.ui.theme.SafeRouterTheme
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Firebase.firestore

        setContent {
            SafeRouterTheme {

                // 🔹 Ahora sí: esto debe ir *dentro* del bloque Compose
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    // 🔹 Usamos AuthManager como fuente de verdad
                    NavigationWrapper(
                        navHostController = navController,
                        auth = AuthManager.getAuthInstance(),
                        db = db
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (AuthManager.isUserLoggedIn()) {
            Log.i("AuthCheck", "✅ Usuario sigue logueado: ${AuthManager.getCurrentUserName()}")
        } else {
            Log.i("AuthCheck", "🚫 No hay sesión activa.")
        }
    }
}
