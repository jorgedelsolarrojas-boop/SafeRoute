package com.example.saferouter.data

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // <-- Importante: usa 'items'
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saferouter.R
import com.example.saferouter.model.Reporte
import com.example.saferouter.ui.theme.*
import com.github.tehras.charts.bar.BarChart
import com.github.tehras.charts.bar.BarChartData
import com.google.firebase.auth.FirebaseAuth // <-- AÑADIDO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query // <-- AÑADIDO
import java.text.SimpleDateFormat // <-- AÑADIDO
import java.util.Date // <-- AÑADIDO
import java.util.Locale // <-- AÑADIDO


// Data class para el nuevo gráfico de fechas
data class FechaChartData(
    val mes: String,
    val cantidad: Int,
    val color: Color
)

@Composable
fun MiDashboardScreen(
    db: FirebaseFirestore,
    auth: FirebaseAuth,
    navigateBack: () -> Unit,
    navigateToHeatmap: () -> Unit,   // <-- AÑADIDO
    context: Context
)
 {
    // Estados para los datos filtrados
    val reportes = remember { mutableStateOf<List<Reporte>>(emptyList()) }
    val misPuntos = remember { mutableStateOf(0) } // <-- AÑADIDO: Para total de puntos
    var listenerRegistration: ListenerRegistration? by remember { mutableStateOf(null) }
    val currentUser = auth.currentUser // <-- AÑADIDO: Obtenemos el usuario

    // Obtener reportes en tiempo real (SOLO DEL USUARIO ACTUAL)
    LaunchedEffect(currentUser) { // Se activa si el usuario cambia
        if (currentUser == null) {
            // Si no hay usuario, limpiar todo
            reportes.value = emptyList()
            misPuntos.value = 0
            return@LaunchedEffect
        }

        // <-- MODIFICADO: Añadido .whereEqualTo() y .orderBy()
        listenerRegistration = db.collection("reportes")
            .whereEqualTo("usuarioId", currentUser.uid) // <-- ¡LA CLAVE! Filtra por ID de usuario
            .orderBy("fecha", Query.Direction.DESCENDING) // Ordena por fecha
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener // Manejo de error

                val reportesList = snapshot?.documents?.mapNotNull { doc ->
                    val reporte = doc.toObject(Reporte::class.java)
                    reporte?.copy(id = doc.id)
                } ?: emptyList()

                // Actualiza los estados
                reportes.value = reportesList
                misPuntos.value = reportesList.sumBy { it.puntos } // <-- AÑADIDO: Suma los puntos
            }
    }

    // Limpiar listener al desmontar
    DisposableEffect(Unit) {
        onDispose {
            listenerRegistration?.remove()
        }
    }

    // Procesar datos para gráficos (solo los del usuario)
    val datosGraficoTipos by remember(reportes.value) {
        derivedStateOf {
            procesarDatosTipos(reportes.value) // Reutilizamos esta función
        }
    }

    // <-- AÑADIDO: Procesar datos para gráfico de fechas
    val datosGraficoFecha by remember(reportes.value) {
        derivedStateOf {
            procesarDatosFecha(reportes.value)
        }
    }

    Box(
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = navigateBack,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back_24),
                            contentDescription = "Back",
                            tint = PrimaryBlueDark // Color corregido
                        )
                    }
                    Text(
                        text = "Mi Dashboard Personal", // <-- MODIFICADO: Título
                        color = PrimaryBlueDark,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            // <-- MODIFICADO: Estadísticas personales
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
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
                        // Stat: Mis Reportes
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = reportes.value.size.toString(),
                                color = PrimaryBlueDark,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Mis Reportes",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        // Stat: Mis Puntos
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = misPuntos.value.toString(),
                                color = SuccessGreen, // Color distintivo para puntos
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Mis Puntos",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        // Stat: Tipos reportados
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = datosGraficoTipos.size.toString(),
                                color = PrimaryBlueDark,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tipos Reportados",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // <-- ELIMINADO: Gráfico Pie "Reportes por Usuario" (no es necesario aquí)

            // <-- AÑADIDO: Gráfico de Barras - Reportes por Mes (HU07)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp), // Altura ajustada
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = BackgroundWhite
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Mi Actividad por Mes",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (datosGraficoFecha.isNotEmpty()) {
                            BarChart(
                                barChartData = BarChartData(
                                    bars = datosGraficoFecha.map { data ->
                                        BarChartData.Bar(
                                            value = data.cantidad.toFloat(),
                                            color = data.color,
                                            label = data.mes
                                        )
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        } else {
                            // Mensaje de "Sin datos"
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Aún no tienes reportes",
                                    color = TextSecondary,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Gráfico de barras - Reportes por tipo (¡Este se queda!)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = BackgroundWhite
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Mis Reportes por Tipo", // <-- MODIFICADO: Título
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (datosGraficoTipos.isNotEmpty()) {
                            BarChart(
                                barChartData = BarChartData(
                                    bars = datosGraficoTipos.map { data ->
                                        BarChartData.Bar(
                                            value = data.cantidad.toFloat(),
                                            color = data.color,
                                            label = data.tipo
                                        )
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_report),
                                        contentDescription = "Sin datos",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Text(
                                        text = "No hay datos de tipos",
                                        color = TextSecondary,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // <-- ELIMINADO: Leyenda "Top Usuarios" (no es necesario aquí)

            // --- AÑADIDO: Historial de Reportes Personales (HU07) ---
            item {
                Text(
                    text = "Mi Historial de Reportes",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            if (reportes.value.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aún no has hecho reportes.",
                            color = TextSecondary,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                // Usamos 'items' para la lista de reportes
                items(reportes.value) { reporte ->
                    // Asumimos que ReporteComunitarioCard está visible en este paquete
                    // Esta es la Card que diseñaste en "MapaComunitarioScreen.kt"
                    ReporteComunitarioCard(reporte = reporte, context = context)
                }
            }

            // Espacio final para mejor scroll
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}


// --- FUNCIONES HELPER ---

// Función para procesar datos por tipo (Esta se queda, es útil)
private fun procesarDatosTipos(reportes: List<Reporte>): List<TipoChartData> {
    val tiposPredefinidos = listOf(
        "Robo",
        "Asalto",
        "Alta congestión vehicular",
        "Baja iluminación de la zona",
        "Huelga"
    )

    val tiposMap = mutableMapOf<String, Int>()

    reportes.forEach { reporte ->
        val tipo = if (reporte.tipo in tiposPredefinidos) {
            reporte.tipo
        } else {
            "Otro"
        }
        tiposMap[tipo] = tiposMap.getOrDefault(tipo, 0) + 1
    }

    val coloresPorTipo = mapOf(
        "Robo" to Color.Red,
        "Asalto" to Color(0xFF3B82F6),
        "Alta congestión vehicular" to WarningYellow,
        "Baja iluminación de la zona" to SuccessGreen,
        "Huelga" to Color(0xFF059669),
        "Otro" to PrimaryBlueLight
    )

    return tiposMap.map { (tipo, cantidad) ->
        TipoChartData(
            tipo = tipo,
            cantidad = cantidad,
            color = coloresPorTipo[tipo] ?: PrimaryBlue
        )
    }
}

// --- CÓPIGO CORREGIDO ---
// Pega esto al final de tu archivo, reemplazando la función vieja

private fun procesarDatosFecha(reportes: List<Reporte>): List<FechaChartData> {
    // Colores para el gráfico de fechas
    val colores = listOf(
        PrimaryBlue,
        ChartPurple,
        SuccessGreen,
        ChartPink,
        PrimaryBlueDark,
        WarningYellow
    )

    // Formato para agrupar por Mes y Año (Ej: "Nov 2025")
    val formatoMes = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    // --- 👇 AQUÍ ESTÁ LA CORRECCIÓN 👇 ---
    // 1. Filtra reportes que no tengan fecha ANTES de procesarlos
    val reportesConFechaValida = reportes.filter { it.fecha != null }

    // 2. Ahora agrupamos usando la lista filtrada y segura
    val reportesPorMes = reportesConFechaValida
        .groupBy { formatoMes.format(it.fecha!!) } // Agrupa por "Nov 2025" (ahora es seguro usar !!)
        .map { (mes, lista) ->
            // Usamos un Triple para guardar la fecha real (para ordenar), el string del mes y la cantidad
            Triple(lista.first().fecha!!, mes, lista.size) // También es seguro aquí
        }
        .sortedByDescending { it.first } // Ordena por fecha (más reciente primero)
        .take(6) // Toma los últimos 6 meses de actividad
        .reversed() // Invierte para que el gráfico muestre (Ej: Jun, Jul, Ago...)

    // Convierte al formato que necesita el gráfico (FechaChartData)
    return reportesPorMes.mapIndexed { index, (_, mes, cantidad) ->
        FechaChartData(
            mes = mes,
            cantidad = cantidad,
            color = colores[index % colores.size] // Asigna un color cíclico
        )
    }
}