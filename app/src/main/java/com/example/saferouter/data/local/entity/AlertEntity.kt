package com.example.saferouter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad para almacenar alertas proactivas generadas por el modelo de IA
 */
@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val severity: String, // "low", "medium", "high"
    val locationLat: Double,
    val locationLng: Double,
    val radius: Double, // Radio en metros donde aplica la alerta
    val predictedRisk: Double, // Probabilidad de riesgo (0.0 - 1.0)
    val incidentType: String,
    val timestamp: Date,
    val isActive: Boolean = true,
    val isRead: Boolean = false
)