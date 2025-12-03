package com.example.saferouter.data.repository

import com.example.saferouter.data.local.dao.AlertDao
import com.example.saferouter.data.local.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository para manejar las operaciones de alertas con Room
 */
class AlertRepository(private val alertDao: AlertDao) {

    fun getAllAlerts(): Flow<List<AlertEntity>> = alertDao.getAllAlerts()

    fun getActiveAlerts(): Flow<List<AlertEntity>> = alertDao.getActiveAlerts()

    fun getAlertsInArea(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): Flow<List<AlertEntity>> {
        return alertDao.getAlertsInArea(minLat, maxLat, minLng, maxLng)
    }

    suspend fun insertAlert(alert: AlertEntity) = alertDao.insertAlert(alert)

    suspend fun updateAlert(alert: AlertEntity) = alertDao.updateAlert(alert)

    suspend fun markAsRead(alertId: String) = alertDao.markAsRead(alertId)

    suspend fun deleteAlert(alertId: String) = alertDao.deleteAlert(alertId)
}