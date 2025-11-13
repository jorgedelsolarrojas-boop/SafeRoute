package com.example.saferouter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first


/**
 * Manager para persistencia de preferencias de notificaciones
 * Usa DataStore (Unidad 6 - Persistencia de datos)
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_preferences")

class NotificationPreferencesManager(private val context: Context) {

    companion object {
        // Keys para preferencias
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val ALERT_RADIUS = intPreferencesKey("alert_radius")
        private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        private val SOUND_TYPE = stringPreferencesKey("sound_type")

        // Keys para tipos de incidentes
        private val NOTIFY_ROBO = booleanPreferencesKey("notify_robo")
        private val NOTIFY_ASALTO = booleanPreferencesKey("notify_asalto")
        private val NOTIFY_CONGESTION = booleanPreferencesKey("notify_congestion")
        private val NOTIFY_ILUMINACION = booleanPreferencesKey("notify_iluminacion")
        private val NOTIFY_HUELGA = booleanPreferencesKey("notify_huelga")
        private val NOTIFY_OTRO = booleanPreferencesKey("notify_otro")

        // Key para notificación de llegada
        private val ARRIVAL_NOTIFICATION = booleanPreferencesKey("arrival_notification")
    }

    // Flujos de datos (Flow)
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[NOTIFICATIONS_ENABLED] ?: true }

    val alertRadius: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[ALERT_RADIUS] ?: 500 } // 500m por defecto

    val soundEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SOUND_ENABLED] ?: true }

    val vibrationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[VIBRATION_ENABLED] ?: true }

    val soundType: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SOUND_TYPE] ?: "default" }

    val arrivalNotificationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ARRIVAL_NOTIFICATION] ?: true }

    // Flujos para tipos de incidentes
    val notifyRobo: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[NOTIFY_ROBO] ?: true }

    val notifyAsalto: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[NOTIFY_ASALTO] ?: true }

    val notifyCongestion: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[NOTIFY_CONGESTION] ?: true }

    val notifyIluminacion: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[NOTIFY_ILUMINACION] ?: true }

    val notifyHuelga: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[NOTIFY_HUELGA] ?: true }

    val notifyOtro: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[NOTIFY_OTRO] ?: true }

    // Funciones para guardar preferencias
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setAlertRadius(radius: Int) {
        context.dataStore.edit { preferences ->
            preferences[ALERT_RADIUS] = radius
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SOUND_ENABLED] = enabled
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun setSoundType(type: String) {
        context.dataStore.edit { preferences ->
            preferences[SOUND_TYPE] = type
        }
    }

    suspend fun setArrivalNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ARRIVAL_NOTIFICATION] = enabled
        }
    }

    // Funciones para configurar tipos de incidentes
    suspend fun setNotifyRobo(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFY_ROBO] = enabled
        }
    }

    suspend fun setNotifyAsalto(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFY_ASALTO] = enabled
        }
    }

    suspend fun setNotifyCongestion(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFY_CONGESTION] = enabled
        }
    }

    suspend fun setNotifyIluminacion(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFY_ILUMINACION] = enabled
        }
    }

    suspend fun setNotifyHuelga(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFY_HUELGA] = enabled
        }
    }

    suspend fun setNotifyOtro(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFY_OTRO] = enabled
        }
    }

    // Función para verificar si un tipo de incidente debe notificarse
    suspend fun shouldNotifyForType(tipo: String): Boolean {
        val preferences = context.dataStore.data.map { it }.first()
        return when (tipo) {
            "Robo" -> preferences[NOTIFY_ROBO] ?: true
            "Asalto" -> preferences[NOTIFY_ASALTO] ?: true
            "Alta congestión vehicular" -> preferences[NOTIFY_CONGESTION] ?: true
            "Baja iluminación de la zona" -> preferences[NOTIFY_ILUMINACION] ?: true
            "Huelga" -> preferences[NOTIFY_HUELGA] ?: true
            "Otro" -> preferences[NOTIFY_OTRO] ?: true
            else -> true
        }
    }

    // Función para obtener todas las preferencias de una vez
    suspend fun getAllPreferences(): NotificationPreferences {
        val preferences = context.dataStore.data.map { it }.first()
        return NotificationPreferences(
            notificationsEnabled = preferences[NOTIFICATIONS_ENABLED] ?: true,
            alertRadius = preferences[ALERT_RADIUS] ?: 500,
            soundEnabled = preferences[SOUND_ENABLED] ?: true,
            vibrationEnabled = preferences[VIBRATION_ENABLED] ?: true,
            soundType = preferences[SOUND_TYPE] ?: "default",
            arrivalNotificationEnabled = preferences[ARRIVAL_NOTIFICATION] ?: true,
            notifyRobo = preferences[NOTIFY_ROBO] ?: true,
            notifyAsalto = preferences[NOTIFY_ASALTO] ?: true,
            notifyCongestion = preferences[NOTIFY_CONGESTION] ?: true,
            notifyIluminacion = preferences[NOTIFY_ILUMINACION] ?: true,
            notifyHuelga = preferences[NOTIFY_HUELGA] ?: true,
            notifyOtro = preferences[NOTIFY_OTRO] ?: true
        )
    }
}

// Data class para las preferencias
data class NotificationPreferences(
    val notificationsEnabled: Boolean = true,
    val alertRadius: Int = 500,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val soundType: String = "default",
    val arrivalNotificationEnabled: Boolean = true,
    val notifyRobo: Boolean = true,
    val notifyAsalto: Boolean = true,
    val notifyCongestion: Boolean = true,
    val notifyIluminacion: Boolean = true,
    val notifyHuelga: Boolean = true,
    val notifyOtro: Boolean = true
)