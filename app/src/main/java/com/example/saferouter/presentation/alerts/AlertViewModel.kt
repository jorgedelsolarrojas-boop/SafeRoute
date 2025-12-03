package com.example.saferouter.presentation.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.saferouter.data.repository.AlertRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel para manejar las alertas proactivas
 */
class AlertViewModel(
    private val alertRepository: AlertRepository
) : ViewModel() {

    val alerts = alertRepository.getActiveAlerts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    suspend fun markAlertAsRead(alertId: String) {
        alertRepository.markAsRead(alertId)
    }
}