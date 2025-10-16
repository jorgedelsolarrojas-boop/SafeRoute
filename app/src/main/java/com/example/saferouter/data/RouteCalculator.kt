package com.example.saferouter.data

import android.content.Context
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RouteCalculator(private val context: Context) {

    // Necesitas una API Key de Google Cloud - puedes obtenerla gratis
    private val apiKey = "AIzaSyDAG3ugBHzMZykntEz-j8vkP7OPUPNPdpU"

    private val geoApiContext by lazy {
        GeoApiContext.Builder()
            .apiKey(apiKey)
            .build()
    }

    suspend fun calculateTravelTime(
        origin: LatLng,
        destinationName: String
    ): String = withContext(Dispatchers.IO) {
        try {
            // Primero geocodificar el nombre del destino a coordenadas
            val destinationLatLng = geocodeAddress(destinationName) ?: return@withContext "No disponible"

            val request = DirectionsApi.newRequest(geoApiContext)
                .origin("${origin.latitude},${origin.longitude}")
                .destination("${destinationLatLng.latitude},${destinationLatLng.longitude}")
                .mode(com.google.maps.model.TravelMode.DRIVING)

            val result = request.await()

            if (result.routes.isNotEmpty()) {
                val route = result.routes[0]
                val leg = route.legs[0]
                val durationInMinutes = leg.duration.inSeconds / 60

                when {
                    durationInMinutes < 60 -> "$durationInMinutes minutos"
                    else -> "${durationInMinutes / 60} h ${durationInMinutes % 60} min"
                }
            } else {
                "Ruta no encontrada"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun geocodeAddress(address: String): LatLng? {
        // Implementación simple de geocodificación
        // En una app real, usarías Google Geocoding API
        return when (address.lowercase()) {
            "centro de lima" -> LatLng(-12.046374, -77.042793)
            "centro cívico" -> LatLng(-12.046374, -77.042793)
            "miraflores" -> LatLng(-12.122432, -77.030204)
            "barranco" -> LatLng(-12.143729, -77.019788)
            "san isidro" -> LatLng(-12.097007, -77.033584)
            else -> null
        }
    }

    fun calculateTravelTimeSimple(
        currentLocation: Location,
        destination: String
    ): String {
        // Implementación simple basada en distancia aproximada
        val destinationLatLng = geocodeAddress(destination) ?: return "15 minutos"

        val destinationLocation = Location("destination").apply {
            latitude = destinationLatLng.latitude
            longitude = destinationLatLng.longitude
        }

        val distanceInKm = currentLocation.distanceTo(destinationLocation) / 1000
        val estimatedTimeMinutes = (distanceInKm / 0.5 * 60).toInt() // Asumiendo 30km/h promedio

        return when {
            estimatedTimeMinutes <= 0 -> "5 minutos"
            estimatedTimeMinutes < 60 -> "$estimatedTimeMinutes minutos"
            else -> "${estimatedTimeMinutes / 60} h ${estimatedTimeMinutes % 60} min"
        }
    }
}

