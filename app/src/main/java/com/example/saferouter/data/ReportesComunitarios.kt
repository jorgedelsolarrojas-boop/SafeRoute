package com.example.saferouter.data

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saferouter.R
import com.example.saferouter.model.Reporte
import com.example.saferouter.ui.theme.*
import com.github.tehras.charts.bar.BarChart
import com.github.tehras.charts.bar.BarChartData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.github.tehras.charts.piechart.PieChart
import com.github.tehras.charts.piechart.PieChartData
import com.github.tehras.charts.piechart.animation.simpleChartAnimation


@Composable
fun ReportesComunitarios(
    db: FirebaseFirestore,
    navigateBack: () -> Unit,
    context: Context
) {
    val reportes = remember { mutableStateOf<List<Reporte>>(emptyList()) }
    var listenerRegistration: ListenerRegistration? by remember { mutableStateOf(null) }

    // Obtener reportes en tiempo real
    LaunchedEffect(Unit) {
        listenerRegistration = db.collection("reportes")    // coleccion creada en firestore
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val reportesList = snapshot?.documents?.mapNotNull { doc ->
                    val reporte = doc.toObject(Reporte::class.java)
                    reporte?.copy(id = doc.id)
                } ?: emptyList()

                reportes.value = reportesList
            }
    }

    // Limpiar listener al desmontar
    DisposableEffect(Unit) {
        onDispose {
            listenerRegistration?.remove()
        }
    }

    // Procesar datos para gráficos
    val datosGraficoUsuarios by remember(reportes.value) {
        derivedStateOf {
            procesarDatosUsuarios(reportes.value)
        }
    }

    val datosGraficoTipos by remember(reportes.value) {
        derivedStateOf {
            procesarDatosTipos(reportes.value)
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
                            tint = DisabledGray
                        )
                    }
                    Text(
                        text = "Dashboard de Reportes",
                        color = PrimaryBlueDark,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            // Estadísticas rápidas
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = reportes.value.size.toString(),
                                color = PrimaryBlueDark,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total Reportes",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = datosGraficoUsuarios.size.toString(),
                                color = PrimaryBlueDark,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Usuarios Activos",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = datosGraficoTipos.size.toString(),
                                color = PrimaryBlueDark,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tipos de Incidente",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Gráfico circular - Reportes por usuario
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
                            text = "Reportes por Usuario",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        //armado de piechart
                        if (datosGraficoUsuarios.isNotEmpty()) {
                            PieChart(
                                pieChartData = PieChartData(
                                    slices = datosGraficoUsuarios.map { data ->
                                        PieChartData.Slice(
                                            value = data.cantidad.toFloat(),
                                            color = data.color
                                        )
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                animation = simpleChartAnimation()
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
                                        text = "No hay datos de reportes",
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

            // Gráfico de barras - Reportes por tipo
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
                            text = "Reportes por Tipo de Incidente",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        // aramado del grafico de barras

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

            // Leyenda de usuarios (solo mostrar top 5)
            item {
                if (datosGraficoUsuarios.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = BackgroundWhite
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Top Usuarios por Reportes",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            val topUsuarios = datosGraficoUsuarios.sortedByDescending { it.cantidad }.take(5)
                            topUsuarios.forEachIndexed { index, data ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(data.color, RoundedCornerShape(4.dp))
                                        )
                                        Text(
                                            text = "${index + 1}. ${data.usuario}",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(start = 12.dp)
                                        )
                                    }
                                    Text(
                                        text = "${data.cantidad} reportes",
                                        color = SuccessGreen,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Espacio final para mejor scroll
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// Data classes para los gráficos
data class UsuarioChartData(
    val usuario: String,
    val cantidad: Int,
    val color: Color
)

data class TipoChartData(
    val tipo: String,
    val cantidad: Int,
    val color: Color
)

// Función para procesar datos de usuarios - VERSIÓN MÁS SEGURA
private fun procesarDatosUsuarios(reportes: List<Reporte>): List<UsuarioChartData> {
    val usuariosMap = mutableMapOf<String, Int>()

    reportes.forEach { reporte ->
        val usuario = reporte.usuarioNombre
        usuariosMap[usuario] = usuariosMap.getOrDefault(usuario, 0) + 1
    }

    // Colores predefinidos para el gráfico
    val colores = listOf(
        PrimaryBlue,
        ChartPurple,
        SuccessGreen,
        ChartPink,
        PrimaryBlueDark,
        DisabledGray,
        PrimaryBlueLight,
        WarningYellow,
        Color.Red, // En lugar de IconDanger
        TextSecondary,
        Color(0xFF3B82F6),
        Color(0xFF009688),
        Color(0xFF059669),
        Color(0xFFFF9800)
    )

    val result = mutableListOf<UsuarioChartData>()
    var colorIndex = 0

    usuariosMap.forEach { (usuario, cantidad) ->
        val displayName = if (usuario.length > 15) "${usuario.take(12)}..." else usuario
        val color = colores[colorIndex % colores.size]

        result.add(UsuarioChartData(usuario = displayName, cantidad = cantidad, color = color))
        colorIndex++
    }

    return result
}

// Función para procesar datos por tipo
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

    // Colores específicos por tipo - usando Color.Red en lugar de IconDanger
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