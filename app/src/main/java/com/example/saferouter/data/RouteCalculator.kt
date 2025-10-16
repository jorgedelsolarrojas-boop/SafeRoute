package com.example.saferouter.data

import android.content.Context
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder

class RouteCalculator(private val context: Context) {

    // API Key de Google Cloud
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
            // Primero geocodificar el nombre del destino a coordenadas usando API real
            val destinationLatLng = geocodeAddressReal(destinationName)
                ?: geocodeAddressFallback(destinationName)
                ?: return@withContext "Dirección no encontrada"

            val request = DirectionsApi.newRequest(geoApiContext)
                .origin("${origin.latitude},${origin.longitude}")
                .destination("${destinationLatLng.latitude},${destinationLatLng.longitude}")
                .mode(com.google.maps.model.TravelMode.DRIVING)

            val result = request.await()

            if (result.routes.isNotEmpty()) {
                val route = result.routes[0]
                val leg = route.legs[0]
                val durationInMinutes = leg.duration.inSeconds / 60

                formatTime(durationInMinutes.toInt())
            } else {
                "Ruta no encontrada"
            }
        } catch (e: Exception) {
            // Si falla la API, usar cálculo simple
            calculateTravelTimeSimple(
                Location("origin").apply {
                    latitude = origin.latitude
                    longitude = origin.longitude
                },
                destinationName
            )
        }
    }

    /**
     * Geocodificación real usando Google Geocoding API (Método HTTP directo)
     */
    private suspend fun geocodeAddressReal(address: String): LatLng? = withContext(Dispatchers.IO) {
        try {
            if (address.isBlank()) return@withContext null

            // Codificar la dirección para URL
            val encodedAddress = URLEncoder.encode(address, "UTF-8")
            val url = "https://maps.googleapis.com/maps/api/geocode/json?address=$encodedAddress&key=$apiKey"

            // Hacer la solicitud HTTP
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)

                if (jsonObject.getString("status") == "OK") {
                    val results = jsonObject.getJSONArray("results")
                    if (results.length() > 0) {
                        val firstResult = results.getJSONObject(0)
                        val geometry = firstResult.getJSONObject("geometry")
                        val location = geometry.getJSONObject("location")

                        val lat = location.getDouble("lat")
                        val lng = location.getDouble("lng")

                        return@withContext LatLng(lat, lng)
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Geocodificación usando la librería de Google Maps Services
     */
    private suspend fun geocodeAddressWithLibrary(address: String): LatLng? = withContext(Dispatchers.IO) {
        try {
            if (address.isBlank()) return@withContext null

            val results = GeocodingApi.geocode(geoApiContext, address).await()

            if (results.isNotEmpty()) {
                val location = results[0].geometry.location
                return@withContext LatLng(location.lat, location.lng)
            }

            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fallback: Geocodificación con direcciones conocidas de Lima
     */
    private fun geocodeAddressFallback(address: String): LatLng? {
        val addressLower = address.lowercase().trim()

        return when {
            // Centro de Lima
            addressLower.contains("centro") && addressLower.contains("lima") ->
                LatLng(-12.046374, -77.042793)
            addressLower.contains("centro cívico") || addressLower.contains("centro civico") ->
                LatLng(-12.046374, -77.042793)
            addressLower.contains("plaza de armas") || addressLower.contains("plaza mayor") ->
                LatLng(-12.046374, -77.042793)

            // Distritos populares
            addressLower.contains("miraflores") ->
                LatLng(-12.122432, -77.030204)
            addressLower.contains("barranco") ->
                LatLng(-12.143729, -77.019788)
            addressLower.contains("san isidro") ->
                LatLng(-12.097007, -77.033584)
            addressLower.contains("surco") || addressLower.contains("santiago de surco") ->
                LatLng(-12.154820, -76.993488)
            addressLower.contains("la molina") ->
                LatLng(-12.079614, -76.938629)
            addressLower.contains("san borja") ->
                LatLng(-12.090977, -77.005638)
            addressLower.contains("lince") ->
                LatLng(-12.084380, -77.033065)
            addressLower.contains("jesus maria") || addressLower.contains("jesús maría") ->
                LatLng(-12.074003, -77.049331)
            addressLower.contains("pueblo libre") ->
                LatLng(-12.075876, -77.063427)
            addressLower.contains("magdalena") ->
                LatLng(-12.091207, -77.075162)
            addressLower.contains("san miguel") ->
                LatLng(-12.077237, -77.089489)
            addressLower.contains("callao") ->
                LatLng(-12.056490, -77.118640)
            addressLower.contains("la victoria") ->
                LatLng(-12.067658, -77.031821)
            addressLower.contains("chorrillos") ->
                LatLng(-12.168779, -77.013510)
            addressLower.contains("surquillo") ->
                LatLng(-12.110849, -77.016424)
            addressLower.contains("breña") || addressLower.contains("brena") ->
                LatLng(-12.057806, -77.053177)
            addressLower.contains("rimac") || addressLower.contains("rímac") ->
                LatLng(-12.025817, -77.043079)
            addressLower.contains("los olivos") ->
                LatLng(-11.970625, -77.072157)
            addressLower.contains("san martin de porres") || addressLower.contains("san martín de porres") ->
                LatLng(-12.010636, -77.074959)
            addressLower.contains("independencia") ->
                LatLng(-11.991971, -77.059639)
            addressLower.contains("comas") ->
                LatLng(-11.938694, -77.052450)

            // Centros comerciales y lugares conocidos
            addressLower.contains("jockey plaza") ->
                LatLng(-12.093088, -76.974975)
            addressLower.contains("larcomar") ->
                LatLng(-12.132222, -77.024722)
            addressLower.contains("real plaza") ->
                LatLng(-12.089722, -77.003611)
            addressLower.contains("aeropuerto") || addressLower.contains("jorge chavez") || addressLower.contains("jorge chávez") ->
                LatLng(-12.021889, -77.114319)

            else -> null
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Radio de la Tierra en kilómetros

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }

    private fun calculateTimeFromDistance(distanceKm: Float): Int {
        // Velocidad promedio de 30 km/h en ciudad
        val averageSpeedKmh = 30.0
        val timeHours = distanceKm / averageSpeedKmh
        val timeMinutes = (timeHours * 60).toInt()

        // Mínimo 5 minutos, máximo 4 horas
        return when {
            timeMinutes < 5 -> 5
            timeMinutes > 240 -> 240
            else -> timeMinutes
        }
    }

    private fun formatTime(minutes: Int): String {
        return when {
            minutes < 60 -> "$minutes min"
            else -> {
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                if (remainingMinutes == 0) "$hours h" else "$hours h $remainingMinutes min"
            }
        }
    }

    fun calculateTravelTimeSimple(
        currentLocation: Location,
        destination: String
    ): String {
        // Usar geocodificación fallback
        val destinationLatLng = geocodeAddressFallback(destination)
            ?: LatLng(-12.046374, -77.042793) // Default: Centro de Lima

        val destinationLocation = Location("destination").apply {
            latitude = destinationLatLng.latitude
            longitude = destinationLatLng.longitude
        }

        val distanceInKm = currentLocation.distanceTo(destinationLocation) / 1000
        val estimatedTimeMinutes = calculateTimeFromDistance(distanceInKm)

        return formatTime(estimatedTimeMinutes)
    }
}