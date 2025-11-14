package com.example.saferouter.data

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.saferouter.R
import com.example.saferouter.model.Reporte
import com.example.saferouter.ui.theme.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MapaComunitarioScreen(
    db: FirebaseFirestore,
    navigateBack: () -> Unit,
    navigateToReportes: () -> Unit, // Nuevo parámetro para navegación
    context: Context
) {
    val reportes = remember { mutableStateOf<List<Reporte>>(emptyList()) }
    val ubicacionActual = remember { mutableStateOf<Location?>(null) }
    val scope = rememberCoroutineScope()
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Estado para puntos totales del usuario
    val puntosTotales = remember { mutableStateOf(0) }

    // Estado para manejar errores
    val errorMessage = remember { mutableStateOf<String?>(null) }

    // Obtener reportes en tiempo real
    var listenerRegistration: ListenerRegistration? by remember { mutableStateOf(null) }

    // Obtener ubicación actual para calcular distancias
    LaunchedEffect(Unit) {
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        // Ubicación fija de referencia (Centro de Lima)
        ubicacionActual.value = Location("").apply {
            latitude = -12.046374
            longitude = -77.042793
        }

        // Escuchar reportes en tiempo real
        listenerRegistration = db.collection("reportes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val reportesList = snapshot?.documents?.mapNotNull { doc ->
                    val reporte = doc.toObject(Reporte::class.java)
                    reporte?.copy(id = doc.id)
                } ?: emptyList()

                // Calcular distancias y ordenar por proximidad
                val reportesConDistancia = reportesList.map { reporte ->
                    val distancia = calcularDistancia(
                        ubicacionActual.value?.latitude ?: 0.0,
                        ubicacionActual.value?.longitude ?: 0.0,
                        reporte.ubicacion.latitud,
                        reporte.ubicacion.longitud
                    )
                    reporte.copy(distancia = distancia)
                }.sortedBy { it.distancia }

                // Calcular puntos totales del usuario actual
                val puntosUsuario = reportesConDistancia
                    .filter { it.usuarioId == currentUser?.uid }
                    .sumBy { it.puntos }

                puntosTotales.value = puntosUsuario
                reportes.value = reportesConDistancia
            }
    }

    // Limpiar listener al desmontar
    DisposableEffect(Unit) {
        onDispose {
            listenerRegistration?.remove()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(PrimaryBlueLight, BackgroundWhite),
                        startY = 0f,
                        endY = 1000f
                    )
                )
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
                    text = "Mapa Comunitario",
                    color = PrimaryBlueDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Estadísticas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                backgroundColor = BackgroundWhite.copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "30 minutos",
                            color = PrimaryBlueDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tiempo activo",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = reportes.value.size.toString(),
                            color = PrimaryBlueDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Reportes",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = puntosTotales.value.toString(),
                            color = PrimaryBlueDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Puntos",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Lista de reportes
            Text(
                text = "Reportes cercanos",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (reportes.value.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_report),
                            contentDescription = "Sin reportes",
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No hay reportes cercanos",
                            color = TextSecondary,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Text(
                            text = "Sé el primero en reportar un incidente",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp) // Espacio para el botón
                ) {
                    items(reportes.value) { reporte ->
                        ReporteComunitarioCard(reporte = reporte, context = context)
                    }
                }
            }
        }

        // Botón "Ver reportes" en la parte inferior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            BackgroundWhite.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Button(
                onClick = navigateToReportes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = PrimaryBlueDark
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Text(
                    text = "📊 Ver Reportes",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun ReporteComunitarioCard(reporte: Reporte, context: Context) {

    val isMine = reporte.usuarioId == FirebaseAuth.getInstance().currentUser?.uid
    var mostrarEvidencia by remember { mutableStateOf(false) }

    // Color según dueño
    val cardBorder = if (isMine) SuccessGreen else Color.Transparent
    val cardBackground = if (isMine) BackgroundWhite.copy(alpha = 0.95f) else BackgroundWhite

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = if (isMine) 8.dp else 4.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = cardBackground,
        border = if (isMine) BorderStroke(2.dp, SuccessGreen) else null
    ) {

        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reporte.tipo,
                        color = PrimaryBlueDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Etiqueta “Mi reporte”
                    if (isMine) {
                        Text(
                            text = " (Mi reporte)",
                            color = SuccessGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }

                Text(
                    text = "${reporte.distancia.toInt()}m",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Descripción
            Text(
                text = reporte.descripcion,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Botones (igual que antes)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val gmmIntentUri = Uri.parse("geo:${reporte.ubicacion.latitud},${reporte.ubicacion.longitud}?q=${reporte.ubicacion.latitud},${reporte.ubicacion.longitud}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                val webIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.google.com/maps/search/?api=1&query=${reporte.ubicacion.latitud},${reporte.ubicacion.longitud}")
                                )
                                context.startActivity(webIntent)
                            }
                        },
                        modifier = Modifier.weight(1f).height(32.dp).padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlueLight.copy(alpha = 0.2f)),
                        elevation = ButtonDefaults.elevation(0.dp, 2.dp)
                    ) {
                        Text("🗺️ Ver ubicación", color = PrimaryBlue, fontSize = 12.sp)
                    }

                    // Botón evidencia
                    if (reporte.evidenciaUrl.isNotEmpty()) {
                        Button(
                            onClick = { mostrarEvidencia = true },
                            modifier = Modifier.weight(1f).height(32.dp).padding(start = 8.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = SuccessGreen.copy(alpha = 0.2f)),
                            elevation = ButtonDefaults.elevation(0.dp, 2.dp)
                        ) {
                            Text("📸 Ver evidencia", color = SuccessGreen, fontSize = 12.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tiempo + puntos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(calcularTiempoTranscurrido(reporte.fecha), color = TextSecondary, fontSize = 12.sp)

                    if (isMine) {
                        Text(
                            text = "+${reporte.puntos} puntos",
                            color = SuccessGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Diálogo evidencia (igual que antes)
    if (mostrarEvidencia && reporte.evidenciaUrl.isNotEmpty()) {


        Dialog(
            onDismissRequest = { mostrarEvidencia = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Header del diálogo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Evidencia del reporte",
                            color = PrimaryBlueDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { mostrarEvidencia = false }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Cerrar",
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Imagen de la evidencia
                    Image(
                        painter = rememberAsyncImagePainter(reporte.evidenciaUrl),
                        contentDescription = "Evidencia del reporte",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(Color.LightGray.copy(alpha = 0.2f)),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Información adicional
                    Column {
                        Text(
                            text = "Tipo: ${reporte.tipo}",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reportado por: ${reporte.usuarioNombre}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Fecha: ${formatearFecha(reporte.fecha)}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón para cerrar
                    Button(
                        onClick = { mostrarEvidencia = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = PrimaryBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cerrar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Función para formatear fecha
private fun formatearFecha(fecha: Date): String {
    val formato = SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale.getDefault())
    return formato.format(fecha)
}

// Función para calcular distancia entre dos puntos
private fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0].toDouble()
}

// Función para calcular tiempo transcurrido
private fun calcularTiempoTranscurrido(fecha: Date): String {
    val ahora = Date()
    val diferencia = ahora.time - fecha.time

    val segundos = diferencia / 1000
    val minutos = segundos / 60
    val horas = minutos / 60
    val dias = horas / 24

    return when {
        minutos < 1 -> "Hace momentos"
        minutos < 60 -> "Hace ${minutos.toInt()} min"
        horas < 24 -> "Hace ${horas.toInt()} h"
        else -> "Hace ${dias.toInt()} días"
    }
}