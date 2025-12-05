package com.example.saferouter.data.remote

import com.example.saferouter.model.Reporte
import retrofit2.http.Body
import retrofit2.http.POST

interface EmergencyApiService {

    // El endpoint que creaste en Cloud Run
    @POST("report-emergency")
    suspend fun sendEmergency(@Body reporte: Reporte)
}