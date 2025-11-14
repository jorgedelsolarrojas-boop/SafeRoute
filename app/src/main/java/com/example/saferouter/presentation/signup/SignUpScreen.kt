package com.example.saferouter.presentation.signup

import android.widget.Toast
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saferouter.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SignUpScreen(
    auth: FirebaseAuth,
    onNavigateToLogin: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var lastname by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            "Crear cuenta",
            color = PrimaryBlueDark,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 🔹 Nombre
        Text("Nombre", color = TextPrimary, fontWeight = FontWeight.Medium)
        TextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ingresa tu nombre") },
            colors = textFieldColors()
        )

        Spacer(Modifier.height(16.dp))

        // 🔹 Apellido
        Text("Apellido", color = TextPrimary, fontWeight = FontWeight.Medium)
        TextField(
            value = lastname,
            onValueChange = { lastname = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ingresa tu apellido") },
            colors = textFieldColors()
        )

        Spacer(Modifier.height(16.dp))

        // 🔹 Edad
        Text("Edad", color = TextPrimary, fontWeight = FontWeight.Medium)
        TextField(
            value = age,
            onValueChange = { age = it.filter { c -> c.isDigit() } }, // solo números
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ingresa tu edad") },
            colors = textFieldColors()
        )

        Spacer(Modifier.height(16.dp))

// 🔹 Teléfono
        Text("Número de teléfono", color = TextPrimary, fontWeight = FontWeight.Medium)
        TextField(
            value = phone,
            onValueChange = {
                phone = it.filter { c -> c.isDigit() || c == '+' }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ej: +51987654321") },
            colors = textFieldColors()
        )

        Spacer(Modifier.height(16.dp))


        // 🔹 Correo
        Text("Correo electrónico", color = TextPrimary, fontWeight = FontWeight.Medium)
        TextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ingresa tu correo") },
            colors = textFieldColors()
        )

        Spacer(Modifier.height(16.dp))

        // 🔹 Contraseña
        Text("Contraseña", color = TextPrimary, fontWeight = FontWeight.Medium)
        TextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ingresa tu contraseña") },
            colors = textFieldColors()
        )

        Spacer(Modifier.height(32.dp))

        // 🔹 Botón de registro
        Button(
            onClick = {
                if (
                    name.isNotBlank() &&
                    lastname.isNotBlank() &&
                    age.isNotBlank() &&
                    phone.isNotBlank() &&
                    email.isNotBlank() &&
                    password.isNotBlank()
                )
                {
                    isLoading = true
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                val uid = user?.uid ?: return@addOnCompleteListener

                                val db = FirebaseFirestore.getInstance()
                                val userData = hashMapOf(
                                    "name" to name,
                                    "lastname" to lastname,
                                    "age" to age.toInt(),
                                    "email" to email,
                                    "imageUrl" to ""
                                )

                                db.collection("users").document(uid).set(userData)
                                    .addOnSuccessListener {
                                        Log.i("SafeRoute", "✅ Usuario guardado en Firestore: $uid")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("SafeRoute", "❌ Error guardando usuario", e)
                                    }

                                user?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                                    if (verifyTask.isSuccessful) {
                                        Toast.makeText(
                                            context,
                                            "✅ Registro exitoso. Verifica tu correo antes de iniciar sesión.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        auth.signOut()
                                        onNavigateToLogin()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "❌ Error al enviar correo de verificación.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "❌ Error al registrar: ${task.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                Log.e("SafeRoute", "Error de registro", task.exception)
                            }
                        }
                } else {
                    Toast.makeText(context, "⚠️ Completa todos los campos", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
            elevation = ButtonDefaults.elevation(4.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "Registrarse",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { onNavigateToLogin() }) {
            Text("¿Ya tienes cuenta? Inicia sesión", color = PrimaryBlue)
        }
    }
}

// 🔹 Helper para estilo de TextField
@Composable
private fun textFieldColors() = TextFieldDefaults.textFieldColors(
    backgroundColor = BackgroundWhite,
    textColor = TextPrimary,
    cursorColor = PrimaryBlue,
    focusedIndicatorColor = PrimaryBlue,
    unfocusedIndicatorColor = TextSecondary,
    placeholderColor = TextSecondary
)
