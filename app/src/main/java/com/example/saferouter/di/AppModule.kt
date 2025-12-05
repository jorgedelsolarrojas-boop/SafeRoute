package com.example.saferouter.di

import com.example.saferouter.BuildConfig
import com.example.saferouter.data.local.AppDatabase
import com.example.saferouter.data.repository.AlertRepository
import com.example.saferouter.data.ReporteRepository // 👈 Corregido
import com.example.saferouter.data.remote.EmergencyApiService // 👈 Nuevo Service de Retrofit
import com.example.saferouter.remote.GeminiApiService
import com.example.saferouter.presentation.bot.BotViewModel
import com.example.saferouter.presentation.alerts.AlertViewModel

// 🚨 IMPORTACIONES NECESARIAS
import com.example.saferouter.data.dao.ReporteDao
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

// URL BASE DE TU SERVICIO DE CLOUD RUN (MODIFICAR A TU URL REAL)
private const val BASE_URL = "https://risk-ann-service-final-1062148838734.us-central1.run.app/"

val appModule = module {

    // ===================================
    // 1. Database (ROOM)
    // ===================================
    // Obtiene el DAO existente y el nuevo DAO de Reportes
    single { get<AppDatabase>().alertDao() }
    single { get<AppDatabase>().reporteDao() }


    // ===================================
    // 2. Retrofit & API Services
    // ===================================

    // Instancia de Retrofit
    single {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Servicio para la emergencia (usa Retrofit)
    single { get<Retrofit>().create(EmergencyApiService::class.java) }


    // ===================================
    // 3. Repositories
    // ===================================

    // Alert Repository (Existente)
    single { AlertRepository(get()) }

    // 👈 Reporte Repository (Ahora inyecta el DAO y la API Service)
    single { ReporteRepository(get(), get()) }


    // ===================================
    // 4. Otros Servicios
    // ===================================

    // Servicio Gemini (Existente)
    single {
        GeminiApiService(
            apiKey = "AIzaSyC7Rq5obBPoZ_4wsFXfasFXDbJnkHtBvSE"
        )
    }

    // Firebase Auth
    single { FirebaseAuth.getInstance() }


    // ===================================
    // 5. ViewModels
    // ===================================

    viewModel { BotViewModel(get(), get()) }
    viewModel { AlertViewModel(get()) }
}