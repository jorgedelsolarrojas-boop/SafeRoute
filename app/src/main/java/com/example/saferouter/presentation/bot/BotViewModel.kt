package com.example.saferouter.presentation.bot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.saferouter.data.repository.ReporteRepository
import com.example.saferouter.remote.GeminiApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para manejar la lógica del chatbot de seguridad
 */
class BotViewModel(
    private val geminiApiService: GeminiApiService,
    private val reporteRepository: ReporteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BotUiState>(BotUiState.Idle)
    val uiState: StateFlow<BotUiState> = _uiState

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    fun sendMessage(message: String, currentLat: Double? = null, currentLng: Double? = null) {
        viewModelScope.launch {
            _uiState.value = BotUiState.Loading
            _messages.value = _messages.value + ChatMessage(text = message, isUser = true)

            try {
                val reports = if (currentLat != null && currentLng != null) {
                    // Obtener reportes en un radio de 2km
                    reporteRepository.getReportesInArea(
                        currentLat - 0.02, currentLat + 0.02,
                        currentLng - 0.02, currentLng + 0.02
                    )
                } else {
                    reporteRepository.getAllReportes()
                }

                val response = geminiApiService.chatAboutLocation(
                    locationLat = currentLat ?: 0.0,
                    locationLng = currentLng ?: 0.0,
                    userQuestion = message,
                    historicalReports = reports
                )

                _messages.value = _messages.value + ChatMessage(text = response, isUser = false)
                _uiState.value = BotUiState.Success
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(
                    text = "Error: ${e.message}",
                    isUser = false
                )
                _uiState.value = BotUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun analyzeRouteSafety(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ) {
        viewModelScope.launch {
            _uiState.value = BotUiState.Loading

            try {
                // Obtener reportes en el área de la ruta
                val minLat = minOf(startLat, endLat) - 0.01
                val maxLat = maxOf(startLat, endLat) + 0.01
                val minLng = minOf(startLng, endLng) - 0.01
                val maxLng = maxOf(startLng, endLng) + 0.01

                val reports = reporteRepository.getReportesInArea(minLat, maxLat, minLng, maxLng)
                val analysis = geminiApiService.analyzeRouteSafety(
                    startLat, startLng, endLat, endLng, reports
                )

                _messages.value = _messages.value + ChatMessage(
                    text = "Análisis de ruta:\n$analysis",
                    isUser = false
                )
                _uiState.value = BotUiState.Success
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(
                    text = "Error analizando la ruta: ${e.message}",
                    isUser = false
                )
                _uiState.value = BotUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
        _uiState.value = BotUiState.Idle
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class BotUiState {
    object Idle : BotUiState()
    object Loading : BotUiState()
    object Success : BotUiState()
    data class Error(val message: String) : BotUiState()
}

