// Versión mejorada del Dashboard con:
// ✔ Actividad por día en vez de por mes
// ✔ Dashboard más limpio
// ✔ Comentarios explicando dónde se usa Tehras Charts
// ✔ Código optimizado y reestructurado

package com.example.saferouter.data

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saferouter.R
import com.example.saferouter.model.Reporte
import com.example.saferouter.ui.theme.*

// 📌 Librería TEHRAS → usada para los gráficos de barras
//    https://github.com/tehras/charts
import com.github.tehras.charts.bar.BarChart
import com.github.tehras.charts.bar.BarChartData

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- DATA CLASS: Datos para el gráfico por día ---
data class DiaChartData(
    val dia: String,      // Ej: "12 Nov"
    val cantidad: Int,
    val color: Color
)

@Composable
fun MiDashboardScreen(
    db: FirebaseFirestore,
    auth: FirebaseAuth,
    navigateBack: () -> Unit,
    navigateToHeatmap: () -> Unit,
    context: Context
) {
    // --- ESTADOS ---
    val reportes = remember { mutableStateOf<List<Reporte>>(emptyList()) }
    val misPuntos = remember { mutableStateOf(0) }
    var listenerRegistration: ListenerRegistration? by remember { mutableStateOf(null) }
    val currentUser = auth.currentUser

    // --- FIREBASE LISTENER: Reportes del usuario actual ---
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            reportes.value = emptyList()
            misPuntos.value = 0
            return@LaunchedEffect
        }

        listenerRegistration = db.collection("reportes")
            .whereEqualTo("usuarioId", currentUser.uid)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val lista = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Reporte::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                reportes.value = lista
                misPuntos.value = lista.sumBy { it.puntos }
            }
    }

    DisposableEffect(Unit) {
        onDispose { listenerRegistration?.remove() }
    }

    // --- DATOS PROCESADOS PARA GRÁFICOS ---
    val datosPorTipo by remember(reportes.value) {
        derivedStateOf { procesarDatosTipos(reportes.value) }
    }

    val datosPorDia by remember(reportes.value) {
        derivedStateOf { procesarDatosPorDia(reportes.value) }
    }

    // --- UI ---
    Box(
        modifier = Modifier.fillMaxSize().background(
            brush = Brush.verticalGradient(
                colors = listOf(PrimaryBlueLight, BackgroundWhite)
            )
        )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- HEADER ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painterResource(id = R.drawable.ic_back_24),
                            contentDescription = "Back",
                            tint = PrimaryBlueDark
                        )
                    }

                    Text(
                        text = "Mi Dashboard Personal",
                        color = PrimaryBlueDark,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            // --- ESTADÍSTICAS ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = BackgroundWhite
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DashboardStat(reportes.value.size, "Mis Reportes")
                        DashboardStat(misPuntos.value, "Mis Puntos", SuccessGreen)
                        DashboardStat(datosPorTipo.size, "Tipos Reportados")
                    }
                }
            }

            // --- GRAFICO: Actividad por día (TEHRAS) ---
            item {
                DashboardCard(title = "Mi Actividad por Día") {
                    if (datosPorDia.isNotEmpty()) {
                        BarChart(
                            barChartData = BarChartData(
                                bars = datosPorDia.map { d ->
                                    BarChartData.Bar(
                                        value = d.cantidad.toFloat(),
                                        label = d.dia,
                                        color = d.color
                                    )
                                }
                            ),
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    } else {
                        NoDataMessage()
                    }
                }
            }

            // --- GRAFICO: Mis tipos reportados ---
            item {
                DashboardCard(title = "Mis Reportes por Tipo") {
                    if (datosPorTipo.isNotEmpty()) {
                        BarChart(
                            barChartData = BarChartData(
                                bars = datosPorTipo.map { d ->
                                    BarChartData.Bar(
                                        value = d.cantidad.toFloat(),
                                        label = d.tipo,
                                        color = d.color
                                    )
                                }
                            ),
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    } else {
                        NoDataMessage()
                    }
                }
            }

            // --- HISTORIAL ---
            item {
                Text(
                    text = "Mi Historial de Reportes",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (reportes.value.isEmpty()) {
                item { NoDataMessage() }
            } else {
                items(reportes.value) { reporte ->
                    ReporteComunitarioCard(reporte = reporte, context = context)
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// --- COMPOSABLES REUTILIZABLES ---

@Composable
fun DashboardCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = BackgroundWhite
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun DashboardStat(value: Int, label: String, color: Color = PrimaryBlueDark) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun NoDataMessage() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Sin datos disponibles", color = TextSecondary)
    }
}

// --- PROCESAMIENTO: Tipos ---
private fun procesarDatosTipos(reportes: List<Reporte>): List<TipoChartData> {
    val tiposPredef = listOf("Robo", "Asalto", "Alta congestión vehicular", "Baja iluminación de la zona", "Huelga")
    val tiposMap = mutableMapOf<String, Int>()

    reportes.forEach {
        val tipo = if (it.tipo in tiposPredef) it.tipo else "Otro"
        tiposMap[tipo] = tiposMap.getOrDefault(tipo, 0) + 1
    }

    val colores = mapOf(
        "Robo" to Color.Red,
        "Asalto" to PrimaryBlue,
        "Alta congestión vehicular" to WarningYellow,
        "Baja iluminación de la zona" to SuccessGreen,
        "Huelga" to ChartPurple,
        "Otro" to PrimaryBlueLight
    )

    return tiposMap.map { (tipo, cant) ->
        TipoChartData(tipo, cant, colores[tipo] ?: PrimaryBlue)
    }
}

// --- NUEVO: Procesar actividad por día ---
private fun procesarDatosPorDia(reportes: List<Reporte>): List<DiaChartData> {
    val formatoDia = SimpleDateFormat("dd MMM", Locale.getDefault())

    val colores = listOf(PrimaryBlue, ChartPurple, SuccessGreen, ChartPink, PrimaryBlueDark, WarningYellow)

    val validos = reportes.filter { it.fecha != null }

    val agrupado = validos
        .groupBy { formatoDia.format(it.fecha!!) }
        .map { (dia, lista) ->
            Triple(lista.first().fecha!!, dia, lista.size)
        }
        .sortedByDescending { it.first }
        .take(7)
        .reversed()

    return agrupado.mapIndexed { i, (_, dia, cant) ->
        DiaChartData(dia, cant, colores[i % colores.size])
    }
}