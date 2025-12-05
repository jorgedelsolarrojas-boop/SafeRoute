package com.example.saferouter.data.dao

import androidx.room.*
import com.example.saferouter.model.Reporte // Usa tu modelo existente

@Dao
interface ReporteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReporte(reporte: Reporte): Long

    @Update
    suspend fun updateReporte(reporte: Reporte)

    // Usamos 'verificado = false' para obtener reportes que no se han enviado a la red
    @Query("SELECT * FROM reportes WHERE verificado = 0")
    suspend fun getUnsyncedReports(): List<Reporte>
}