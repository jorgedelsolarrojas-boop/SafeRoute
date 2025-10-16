package com.example.saferouter.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import com.example.saferouter.R
import com.example.saferouter.presentation.home.TravelLocationService
import com.example.saferouter.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SeguimientoViajeScreen(
    db: FirebaseFirestore,
    navigateBack: () -> Unit,
    context: Context
) {
    val destination = remember { mutableStateOf("") }
    val estimatedTime = remember { mutableStateOf("Calculando...") }
    var isTraveling by remember { mutableStateOf(false) }
    val currentLocation = remember { mutableStateOf<Location?>(null) }

    val routeCalculator = remember { RouteCalculator(context) }

    // Obtener ubicación actual para calcular tiempo
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!granted) {
            Toast.makeText(context, "Necesitas dar permiso de ubicación para iniciar viaje", Toast.LENGTH_SHORT).show()
        }
    }

    // Calcular tiempo estimado cuando cambia el destino
    LaunchedEffect(destination.value) {
        if (destination.value.isNotEmpty() && !isTraveling) {
            // Calcular tiempo estimado cuando se ingresa un destino
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        currentLocation.value = it
                        val calculatedTime = routeCalculator.calculateTravelTimeSimple(
                            it,
                            destination.value
                        )
                        estimatedTime.value = calculatedTime
                    }
                }
            } else {
                estimatedTime.value = "Permiso de ubicación necesario"
            }
        } else if (destination.value.isEmpty()) {
            estimatedTime.value = "Ingresa un destino"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        // Header con botón de retroceso
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
                text = "Seguimiento de Viaje",
                color = PrimaryBlueDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Estado del viaje
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                backgroundColor = BackgroundWhite
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isTraveling) "🚗 Viaje en curso" else "🛑 No hay viaje activo",
                        color = if (isTraveling) Color.Green else Color.Red,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isTraveling) "Tu ubicación está siendo compartida" else "Ingresa un destino para comenzar",
                        color = PrimaryBlue,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Campo de destino
            Text(
                text = "Destino",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TextField(
                value = destination.value,
                onValueChange = { destination.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                placeholder = { Text("e.j. Centro de Lima, Miraflores, San Isidro") },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = BackgroundWhite,
                    textColor = TextPrimary,
                    cursorColor = PrimaryBlue,
                    focusedIndicatorColor = PrimaryBlue,
                    unfocusedIndicatorColor = TextSecondary
                ),
                enabled = !isTraveling
            )

            // Tiempo estimado
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                backgroundColor = PrimaryBlueLight.copy(alpha = 0.1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tiempo Estimado",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = estimatedTime.value,
                        color = PrimaryBlueDark,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botón Iniciar/Finalizar viaje
            Button(
                onClick = {
                    val fineLocationGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                    if (!fineLocationGranted) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        return@Button
                    }

                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid == null) {
                        Toast.makeText(context, "Debes iniciar sesión para usar esta función", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (destination.value.isEmpty() && !isTraveling) {
                        Toast.makeText(context, "Por favor ingresa un destino", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val database = FirebaseDatabase.getInstance().getReference("viajes/$uid/info")

                    if (!isTraveling) {
                        // Iniciar viaje
                        val viajeInfo = mapOf(
                            "destino" to destination.value,
                            "horaInicio" to System.currentTimeMillis(),
                            "tiempoEstimado" to estimatedTime.value,
                            "estado" to "en_curso",
                            "userUid" to uid,
                            "userName" to (FirebaseAuth.getInstance().currentUser?.displayName ?: "Usuario")
                        )

                        database.setValue(viajeInfo).addOnSuccessListener {
                            Toast.makeText(context, "✅ Viaje iniciado - Notificando contactos", Toast.LENGTH_SHORT).show()
                            isTraveling = true
                            val intent = Intent(context, TravelLocationService::class.java)
                            startForegroundService(context, intent)
                        }.addOnFailureListener {
                            Toast.makeText(context, "❌ Error al iniciar viaje", Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        // Finalizar viaje
                        val finInfo = mapOf(
                            "estado" to "finalizado",
                            "horaFin" to System.currentTimeMillis()
                        )

                        database.updateChildren(finInfo).addOnSuccessListener {
                            Toast.makeText(context, "✅ Viaje finalizado correctamente", Toast.LENGTH_SHORT).show()
                            isTraveling = false
                            destination.value = ""
                            estimatedTime.value = "Calculando..."
                            val intent = Intent(context, TravelLocationService::class.java)
                            context.stopService(intent)
                        }.addOnFailureListener {
                            Toast.makeText(context, "❌ Error al finalizar viaje", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isTraveling) AlertRed else SuccessGreen
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = if (!isTraveling) destination.value.isNotEmpty() else true
            ) {
                Text(
                    text = if (isTraveling) "Finalizar Viaje" else "✓ Iniciar Viaje",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Preview
@Composable
fun SeguimientoViajeScreenPreview() {
    SeguimientoViajeScreen(
        db = FirebaseFirestore.getInstance(),
        navigateBack = {},
        context = androidx.compose.ui.platform.LocalContext.current
    )
}