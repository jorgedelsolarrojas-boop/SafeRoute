package com.example.saferouter.data.local.converters

import androidx.room.TypeConverter
import com.example.saferouter.model.UbicacionReporte // 👈 Importar tu modelo de ubicación
import com.google.gson.Gson // 👈 Necesita Gson
import java.util.Date

class Converters {

    // Manejo de Fechas
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // MANEJO DE UBICACIÓN

    // Convierte el objeto UbicacionReporte a String (JSON) para Room
    @TypeConverter
    fun ubicacionReporteToString(ubicacion: UbicacionReporte): String {
        return Gson().toJson(ubicacion) // Convierte el objeto a una cadena JSON
    }

    // Convierte el String (JSON) almacenado en Room de vuelta a UbicacionReporte
    @TypeConverter
    fun stringToUbicacionReporte(json: String): UbicacionReporte {
        return Gson().fromJson(json, UbicacionReporte::class.java)
    }
}