package com.example.saferouter.remote

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiApiService(private val apiKey: String) {
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.0-flash", // 🔥 CAMBIADO de gemini-1.5-flash a gemini-2.0-flash
            apiKey = apiKey
        )
    }

    suspend fun analyzeRouteSafety(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        historicalReports: List<Map<String, Any>>
    ): String = withContext(Dispatchers.IO) {
        try {
            val reportsSummary = if (historicalReports.isNotEmpty()) {
                historicalReports.take(10).joinToString("\n") { report ->
                    "Tipo: ${report["tipo"]}, Descripción: ${report["descripcion"]}, Fecha: ${report["fecha"]}"
                }
            } else {
                "No hay reportes históricos en esta área."
            }

            val prompt = """
                Eres un asistente de seguridad urbana en Lima, Perú. Analiza la siguiente ruta y reportes históricos para determinar si es segura.
                
                Ruta: Desde ($startLat, $startLng) hasta ($endLat, $endLng)
                
                Reportes históricos en el área:
                $reportsSummary
                
                Basándote en esta información, proporciona:
                1. Una evaluación de seguridad (Segura, Moderadamente Segura, Riesgosa)
                2. Razones principales basadas en los reportes
                3. Recomendaciones de precaución
                4. Horarios a evitar si aplica
                
                Responde de manera concisa y útil en español.
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            response.text ?: "No se pudo obtener una evaluación en este momento."
        } catch (e: Exception) {
            "Error al analizar la ruta: ${e.localizedMessage ?: e.message}"
        }
    }

    suspend fun chatAboutLocation(
        locationLat: Double,
        locationLng: Double,
        userQuestion: String,
        historicalReports: List<Map<String, Any>>
    ): String = withContext(Dispatchers.IO) {
        try {
            val reportsSummary = if (historicalReports.isNotEmpty()) {
                historicalReports.take(5).joinToString("\n") { report ->
                    "Tipo: ${report["tipo"]}, Descripción: ${report["descripcion"]}, Fecha: ${report["fecha"]}"
                }
            } else {
                "No hay reportes recientes en esta área."
            }

            val prompt = """
                Eres un asistente de seguridad urbana en Lima, Perú. El usuario está en la ubicación ($locationLat, $locationLng).
                
                Pregunta del usuario: $userQuestion
                
                Reportes históricos en esta área:
                $reportsSummary
                
                Proporciona una respuesta útil y basada en los datos disponibles. Si no hay reportes recientes, ofrece consejos generales de seguridad para Lima.
                
                Responde en español de manera clara, concisa y amigable.
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            response.text ?: "Lo siento, no pude procesar tu pregunta en este momento."
        } catch (e: Exception) {
            "Error en el chat: ${e.localizedMessage ?: e.message}"
        }
    }
}