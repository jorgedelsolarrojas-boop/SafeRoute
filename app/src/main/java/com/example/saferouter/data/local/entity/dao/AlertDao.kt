
package com.example.saferouter.data.local.dao

import androidx.room.*
import com.example.saferouter.data.local.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE isActive = 1 ORDER BY predictedRisk DESC")
    fun getActiveAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE locationLat BETWEEN :minLat AND :maxLat AND locationLng BETWEEN :minLng AND :maxLng AND isActive = 1")
    fun getAlertsInArea(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity)

    @Update
    suspend fun updateAlert(alert: AlertEntity)

    @Query("UPDATE alerts SET isRead = 1 WHERE id = :alertId")
    suspend fun markAsRead(alertId: String)

    @Query("DELETE FROM alerts WHERE id = :alertId")
    suspend fun deleteAlert(alertId: String)
}