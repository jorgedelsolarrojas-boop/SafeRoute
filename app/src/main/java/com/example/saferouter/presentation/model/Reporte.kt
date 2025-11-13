package com.example.saferouter.model

import java.util.*

data class Reporte(
    val id: String = "",
    val tipo: String = "",
    val descripcion: String = "",
    val ubicacion: UbicacionReporte = UbicacionReporte(),
    val evidenciaUrl: String = "",
    val usuarioId: String = "",
    val usuarioNombre: String = "",
    val fecha: Date = Date(),
    val puntos: Int = 10,
    val verificado: Boolean = false,
    val distancia: Double = 0.0
) {
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