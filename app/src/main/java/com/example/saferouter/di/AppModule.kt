package com.example.saferouter.di

import com.example.saferouter.BuildConfig
import com.example.saferouter.data.local.AppDatabase
import com.example.saferouter.data.repository.AlertRepository
import com.example.saferouter.data.repository.ReporteRepository
import com.example.saferouter.remote.GeminiApiService
import com.example.saferouter.presentation.bot.BotViewModel
import com.example.saferouter.presentation.alerts.AlertViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single { AppDatabase.getInstance(androidContext()) }
    single { get<AppDatabase>().alertDao() }

    // Repositories
    single { AlertRepository(get()) }
    single { ReporteRepository() }

    // Services - 🔥 PASAR LA API KEY COMO PARÁMETRO
    single {
        GeminiApiService(
            apiKey = "AIzaSyC7Rq5obBPoZ_4wsFXfasFXDbJnkHtBvSE"
            // O si tienes BuildConfig habilitado: BuildConfig.GEMINI_API_KEY
        )
    }

    // ViewModels
    viewModel { BotViewModel(get(), get()) }
    viewModel { AlertViewModel(get()) }
}