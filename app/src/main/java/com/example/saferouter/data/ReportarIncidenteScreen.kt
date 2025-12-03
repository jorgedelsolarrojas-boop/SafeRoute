package com.example.saferouter.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.saferouter.R
import com.example.saferouter.model.UbicacionReporte
import com.example.saferouter.ui.theme.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportarIncidenteScreen(
    db: FirebaseFirestore,
    navigateBack: () -> Unit,
    context: Context
) {
    val context = LocalContext.current   // get actual context para acceder a recursos del sistema, start actividades, get servicios
    val scope = rememberCoroutineScope()   // Permite lanzar corutinas (tareas asincrónicas)
    val scrollState = rememberScrollState()  // crea un objeto ScrollState q mantiene Estado para el desplazamiento vertical

    // Estados del formulario
    val tipoIncidente = remember { mutableStateOf("") }
    val descripcion = remember { mutableStateOf("") }
    val evidenciaUri = remember { mutableStateOf<Uri?>(null) }
    val ubicacion = remember { mutableStateOf<UbicacionReporte?>(null) }

    // Estados para UI
    var expanded by remember { mutableStateOf(false) }
    val isLoading = remember { mutableStateOf(false) }

    // Estado para el nombre del usuario
    val nombreUsuario = remember { mutableStateOf("Cargando...") }

    // Configuración de Cloudinary
    val cloudName = "djh5bpb3l"
    val uploadPreset = "android_unsigned"

    // Lista de tipos de incidente
    val tiposIncidente = listOf(
        "Robo",
        "Asalto",
        "Alta congestión vehicular",
        "Baja iluminación de la zona",
        "Huelga",
        "Otro"
    )

    // Permisos de ubicación
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            obtenerUbicacionActual(context) { loc ->
                ubicacion.value = UbicacionReporte(
                    latitud = loc.latitude,
                    longitud = loc.longitude,
                    direccion = "Ubicación detectada",
                    timestamp = System.currentTimeMillis()
                )
            }
        } else {
            Toast.makeText(context, "Se necesita permiso de ubicación", Toast.LENGTH_SHORT).show()
        }
    }

    // Permisos de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Se necesita permiso de cámara", Toast.LENGTH_SHORT).show()
        }
    }

    // Archivo temporal para la foto
    val tempFile = remember { createImageFile(context) }

    // Launcher para cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            evidenciaUri.value = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            Toast.makeText(context, "Foto capturada exitosamente", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para galería
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            evidenciaUri.value = it
            Toast.makeText(context, "Imagen seleccionada", Toast.LENGTH_SHORT).show()
        }
    }

    // Obtener información del usuario actual y ubicación al cargar la pantalla

    LaunchedEffect(Unit) {     //Corutina permite manejar operaciones asincronas
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { currentUser ->
            // Buscar el usuario en la colección "users" de Firestore
            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Obtener el nombre del documento usando la estructura correcta
                        val nombre = document.getString("name") ?: ""
                        val apellido = document.getString("lastname") ?: ""

                        // Combinar nombre y apellido si ambos existen
                        nombreUsuario.value = if (nombre.isNotEmpty() && apellido.isNotEmpty()) {
                            "$nombre $apellido"
                        } else if (nombre.isNotEmpty()) {
                            nombre
                        } else if (apellido.isNotEmpty()) {
                            apellido
                        } else {
                            // Si no hay nombre, usar el email
                            currentUser.email ?: "Usuario"
                        }

                        Log.d("ReportarIncidente", "Nombre de usuario obtenido: ${nombreUsuario.value}")
                    } else {
                        // Si no existe en users, usar displayName de Auth o email
                        nombreUsuario.value = currentUser.displayName ?:
                                currentUser.email ?:
                                "Usuario"
                        Log.d("ReportarIncidente", "Usuario no encontrado en Firestore, usando: ${nombreUsuario.value}")
                    }
                }
                .addOnFailureListener { e ->
                    // En caso de error, usar información de Auth
                    nombreUsuario.value = currentUser.displayName ?:
                            currentUser.email ?:
                            "Usuario"
                    Log.e("ReportarIncidente", "Error al obtener usuario: ${e.message}")
                }
        } ?: run {
            // Si no hay usuario autenticado
            nombreUsuario.value = "Usuario Anónimo"
            Log.w("ReportarIncidente", "No hay usuario autenticado")
        }

        // Obtener ubicación
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            obtenerUbicacionActual(context) { loc ->
                ubicacion.value = UbicacionReporte(
                    latitud = loc.latitude,
                    longitud = loc.longitude,
                    direccion = "Ubicación actual",
                    timestamp = System.currentTimeMillis()
                )
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
            // Header (no scrolleable)
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
                    text = "Reportar Incidente",
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
                // Tipo de incidente
                Text(
                    text = "Tipo de incidente *",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Dropdown para tipo de incidente
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = Color.White
                        )
                    ) {
                        Text(
                            text = tipoIncidente.value.ifEmpty { "Selecciona un tipo" },
                            color = if (tipoIncidente.value.isEmpty()) TextSecondary else TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back_24),
                            contentDescription = "Dropdown",
                            tint = PrimaryBlue
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        tiposIncidente.forEach { tipo ->
                            DropdownMenuItem(
                                onClick = {
                                    tipoIncidente.value = tipo
                                    expanded = false
                                }
                            ) {
                                Text(text = tipo)
                            }
                        }
                    }
                }

                // Descripción
                Text(
                    text = "Descripción *",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = descripcion.value,
                    onValueChange = { descripcion.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(bottom = 20.dp),
                    placeholder = { Text("Describe qué pasó...", color = TextSecondary) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        backgroundColor = Color.White,
                        textColor = TextPrimary,
                        cursorColor = PrimaryBlue,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                // Ubicación
                Text(
                    text = "Ubicación *",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📍",
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = ubicacion.value?.direccion ?: "Obteniendo ubicación...",
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        }

                        if (ubicacion.value != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Lat: ${String.format("%.6f", ubicacion.value!!.latitud)}, " +
                                        "Lng: ${String.format("%.6f", ubicacion.value!!.longitud)}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val gmmIntentUri = Uri.parse(
                                            "geo:${ubicacion.value!!.latitud},${ubicacion.value!!.longitud}" +
                                                    "?q=${ubicacion.value!!.latitud},${ubicacion.value!!.longitud}"
                                        )
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                        mapIntent.setPackage("com.google.android.apps.maps")
                                        try {
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "No se pudo abrir Google Maps", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        backgroundColor = Color.White
                                    )
                                ) {
                                    Text(
                                        text = "🗺️ Ver mapa",
                                        color = PrimaryBlue,
                                        fontSize = 13.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.ACCESS_FINE_LOCATION
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            obtenerUbicacionActual(context) { loc ->
                                                ubicacion.value = UbicacionReporte(
                                                    latitud = loc.latitude,
                                                    longitud = loc.longitude,
                                                    direccion = "Ubicación actualizada",
                                                    timestamp = System.currentTimeMillis()
                                                )
                                                Toast.makeText(context, "Ubicación actualizada", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        backgroundColor = Color.White
                                    )
                                ) {
                                    Text(
                                        text = "🔄 Actualizar",
                                        color = PrimaryBlue,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Evidencia
                Text(
                    text = "Evidencia",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            when {
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED -> {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        tempFile
                                    )
                                    cameraLauncher.launch(uri)
                                }
                                else -> {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "📷 Foto",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "🖼️ Galería",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                // Mostrar imagen seleccionada
                evidenciaUri.value?.let { uri ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(bottom = 20.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            //// En la sección de evidencia - para mostrar imagen seleccionada
                            // usamos COIL
                            Image(
                                painter = rememberAsyncImagePainter(uri), // ✅ COIL para cargar la imagen y mostrarla en un composable
                                contentDescription = "Evidencia",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            IconButton(
                                onClick = { evidenciaUri.value = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Red.copy(alpha = 0.7f), RoundedCornerShape(50))
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_back_24),
                                    contentDescription = "Eliminar",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // Botón enviar (fijo en la parte inferior)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Button(
                    onClick = {
                        // Validaciones
                        when {
                            tipoIncidente.value.isEmpty() -> {
                                Toast.makeText(context, "Selecciona el tipo de incidente", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            descripcion.value.isEmpty() -> {
                                Toast.makeText(context, "Escribe una descripción", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            ubicacion.value == null -> {
                                Toast.makeText(context, "Espera a que se detecte la ubicación", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                        }

                        isLoading.value = true

                        scope.launch {      // Corutina para operaciones de red
                            try {
                                // Subir evidencia a Cloudinary si existe
                                var evidenciaUrl = ""
                                evidenciaUri.value?.let { uri ->
                                    Log.d("ReportarIncidente", "Subiendo evidencia a Cloudinary...")
                                    evidenciaUrl = subirImagenACloudinary(    // ✅ Función suspend
                                        uri = uri,
                                        cloudName = cloudName,
                                        uploadPreset = uploadPreset,
                                        context = context
                                    ) ?: ""
                                    Log.d("ReportarIncidente", "Evidencia subida: $evidenciaUrl")

                                    if (evidenciaUrl.isEmpty()) {
                                        withContext(Dispatchers.Main) {    // CORUTINA para actualizar UI, // ✅ Vuelve al hilo principal
                                            Toast.makeText(context, "❌ Error al subir la evidencia", Toast.LENGTH_SHORT).show()
                                            isLoading.value = false
                                        }
                                        return@launch
                                    }
                                }

                                // Crear reporte con el nombre del usuario obtenido de Firestore
                                val user = FirebaseAuth.getInstance().currentUser
                                val reporte = hashMapOf(
                                    "tipo" to tipoIncidente.value,
                                    "descripcion" to descripcion.value,
                                    "ubicacion" to hashMapOf(
                                        "latitud" to ubicacion.value!!.latitud,
                                        "longitud" to ubicacion.value!!.longitud,
                                        "direccion" to ubicacion.value!!.direccion,
                                        "timestamp" to ubicacion.value!!.timestamp
                                    ),
                                    "evidenciaUrl" to evidenciaUrl,
                                    "usuarioId" to (user?.uid ?: "anonimo"),
                                    "usuarioNombre" to nombreUsuario.value, // ✅ Usar el nombre obtenido de Firestore
                                    "fecha" to com.google.firebase.Timestamp.now(),
                                    "puntos" to 10,
                                    "verificado" to false
                                )

                                Log.d("ReportarIncidente", "Guardando reporte con usuario: ${nombreUsuario.value}")

                                // Guardar en Firestore
                                db.collection("reportes")    // se crea la coleccion reportes en firestore
                                    .add(reporte)
                                    .addOnSuccessListener {
                                        isLoading.value = false
                                        Toast.makeText(context, "✅ Reporte enviado exitosamente", Toast.LENGTH_LONG).show()
                                        navigateBack()
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading.value = false
                                        Log.e("ReportarIncidente", "Error al guardar: ${e.message}")
                                        Toast.makeText(context, "❌ Error al enviar: ${e.message}", Toast.LENGTH_LONG).show()
                                    }

                            } catch (e: Exception) {
                                isLoading.value = false
                                Log.e("ReportarIncidente", "Error general: ${e.message}")
                                Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isLoading.value) TextSecondary else PrimaryBlueDark
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading.value,
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    if (isLoading.value) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enviando...",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        Text(
                            text = "✓ Enviar Reporte",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// Función para obtener ubicación actual
private fun obtenerUbicacionActual(
    context: Context,
    onLocationObtained: (Location) -> Unit
) {
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    onLocationObtained(it)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Ubicacion", "Error obteniendo ubicación: ${e.message}")
            }
    }
}

// Función para crear archivo temporal de imagen
private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir("SafeRouter")
    if (storageDir != null && !storageDir.exists()) {
        storageDir.mkdirs()
    }
    return File.createTempFile(
        "JPEG_${timeStamp}_",
        ".jpg",
        storageDir
    )
}

// 🌥️ Función para subir imagen a Cloudinary
// Subida manual a Cloudinary - NO Retrofit
private suspend fun subirImagenACloudinary(
    uri: Uri,
    cloudName: String,
    uploadPreset: String,
    context: Context
): String? = withContext(Dispatchers.IO) {    // corutina para operaciones bloqueantes
    // ✅ Cambia al dispatcher de IO para operaciones de red/archivos
    try {
        // Operaciones que bloquean el hilo:
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes() ?: return@withContext null
        inputStream.close()

        val boundary = "Boundary-${System.currentTimeMillis()}"
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        val url = URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
        val connection = url.openConnection() as HttpURLConnection    //SE usa HttpURLConnection manual, en lugar de retrofit

        // ... operaciones de red,
        //conexion manual

        connection.doOutput = true
        connection.useCaches = false
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

        val outputStream = DataOutputStream(connection.outputStream)

        // Upload preset
        outputStream.writeBytes(twoHyphens + boundary + lineEnd)
        outputStream.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"$lineEnd$lineEnd")
        outputStream.writeBytes(uploadPreset + lineEnd)

        // File
        outputStream.writeBytes(twoHyphens + boundary + lineEnd)
        outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"$lineEnd")
        outputStream.writeBytes("Content-Type: image/jpeg$lineEnd$lineEnd")
        outputStream.write(bytes)
        outputStream.writeBytes(lineEnd)
        outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
        outputStream.flush()
        outputStream.close()

        val responseCode = connection.responseCode
        Log.d("Cloudinary", "Response code: $responseCode")

        if (responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d("Cloudinary", "Response: $response")

            val urlStart = response.indexOf("\"secure_url\":\"") + 14
            val urlEnd = response.indexOf("\"", urlStart)
            return@withContext response.substring(urlStart, urlEnd).replace("\\/", "/")
        } else {
            val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
            Log.e("Cloudinary", "Error response: $errorResponse")
            null
        }
    } catch (e: Exception) {
        Log.e("Cloudinary", "Error subiendo imagen: ${e.message}", e)
        null
    }
}