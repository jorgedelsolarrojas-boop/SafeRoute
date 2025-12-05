package com.example.saferouter.data

import com.example.saferouter.data.dao.ReporteDao
import com.example.saferouter.data.remote.EmergencyApiService
import com.example.saferouter.model.Reporte
import com.google.firebase.auth.FirebaseAuth

class ReporteRepository(
    private val reporteDao: ReporteDao,
    private val apiService: EmergencyApiService
) {
    suspend fun saveAndSendEmergency(reporte: Reporte) {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"

        // 1. Guardar localmente
        val id = reporteDao.insertReporte(reporte).toInt()
        val reporteLocal = reporte.copy(id = id.toString(), usuarioId = userUid)

        try {
            // 2. Intentar enviar el reporte (Retrofit)
            apiService.sendEmergency(reporteLocal)

            // 3. Si es exitoso, marcar como sincronizado (isSynced = true)
            reporteDao.updateReporte(reporteLocal.copy(verificado = true)) // Usando 'verificado' como flag

        } catch (e: Exception) {
            // 4. Si falla, se queda como no sincronizado (verificado = false)
            // (La aplicación principal puede reintentar con otro Worker después)
            throw e
        }
    }
}