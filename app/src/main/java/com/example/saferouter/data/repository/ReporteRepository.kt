package com.example.saferouter.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await

/**
 * Repository para obtener reportes de Firestore para el análisis predictivo
 */
class ReporteRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getReportesInArea(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): List<Map<String, Any>> {
        return try {
            // Consulta reportes en el área específica
            val query = db.collection("reportes")
                .whereGreaterThanOrEqualTo("ubicacion.latitud", minLat)
                .whereLessThanOrEqualTo("ubicacion.latitud", maxLat)
                .whereGreaterThanOrEqualTo("ubicacion.longitud", minLng)
                .whereLessThanOrEqualTo("ubicacion.longitud", maxLng)
                .get()
                .await()

            query.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllReportes(): List<Map<String, Any>> {
        return try {
            val query = db.collection("reportes").get().await()
            query.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

