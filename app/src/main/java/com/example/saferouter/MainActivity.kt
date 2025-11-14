package com.example.saferouter

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.saferouter.firebase.AuthManager
import com.example.saferouter.ui.theme.SafeRouterTheme
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Firebase.firestore

        // 🔥 Guardar el FCM token del usuario logueado
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d("FCM", "Token guardado: $token")
                    }
                    .addOnFailureListener {
                        Log.e("FCM", "Error guardando token: ${it.message}")
                    }
            } else {
                Log.e("FCM", "No hay usuario logueado: no se guarda token")
            }
        }


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
