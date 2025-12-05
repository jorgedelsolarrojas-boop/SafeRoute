package com.example.saferouter.model

import androidx.room.Entity // 👈 NUEVO
import androidx.room.PrimaryKey // 👈 NUEVO
import java.util.*

@Entity(tableName = "reportes") // 👈 LO CONVERTIMOS EN UNA ENTIDAD ROOM
data class Reporte(
    @PrimaryKey // 👈 AÑADIMOS LA CLAVE PRIMARIA
    val id: String = "",
    val tipo: String = "",
    val descripcion: String = "",
    val ubicacion: UbicacionReporte = UbicacionReporte(),
    val evidenciaUrl: String = "",
    val usuarioId: String = "",
    val usuarioNombre: String = "",
    val fecha: Date = Date(),
    val puntos: Int = 10,
    val verificado: Boolean = false, // Usaremos este como flag de sincronización
    val distancia: Double = 0.0
) {
    // Es mejor evitar el constructor secundario para Room y usar valores por defecto.
    constructor() : this("", "", "", UbicacionReporte(), "", "", "", Date(), 10, false, 0.0)
}

data class UbicacionReporte(
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val direccion: String = "",
    val timestamp: Long = 0
) {
    constructor() : this(0.0, 0.0, "", 0)
}