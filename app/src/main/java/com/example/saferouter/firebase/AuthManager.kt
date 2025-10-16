package com.example.saferouter.firebase

import android.util.Log
import androidx.compose.runtime.Composable
import com.google.firebase.auth.FirebaseAuth

object AuthManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private const val TAG = "AuthManager"

    fun register(
        email: String,
        password: String,
        name: String,
        lastname: String,
        callback: (Boolean, String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName("$name $lastname")
                        .build()
                    user?.updateProfile(profileUpdates)

                    user?.sendEmailVerification()
                        ?.addOnSuccessListener {
                            callback(true, "Correo de verificación enviado correctamente.")
                        }
                        ?.addOnFailureListener { e ->
                            callback(false, "Cuenta creada, pero error al enviar verificación: ${e.message}")
                        }
                } else {
                    callback(false, task.exception?.message ?: "Error al registrar.")
                }
            }
    }

    fun login(email: String, password: String, callback: (Boolean, String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        callback(true, "Inicio de sesión exitoso.")
                    } else {
                        auth.signOut()
                        callback(false, "Verifica tu correo antes de iniciar sesión.")
                    }
                } else {
                    callback(false, task.exception?.message ?: "Error al iniciar sesión.")
                }
            }
    }

    fun logout() {
        auth.signOut()
        Log.d(TAG, "Usuario cerró sesión.")
    }

    // ✅ ESTA ES LA FUNCIÓN CORRECTA Y VISIBLE
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentUserName(): String? {
        return auth.currentUser?.displayName
    }

    fun getAuthInstance(): FirebaseAuth {
        return auth
    }

}
