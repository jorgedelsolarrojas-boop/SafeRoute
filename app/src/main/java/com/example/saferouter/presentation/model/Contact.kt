package com.example.saferouter.presentation.model

data class Contact(
    val nombre: String = "",
    val telefono: String = "",
    val email: String = "",
    val isAppUser: Boolean = false
) {
    constructor() : this("", "", "", false)
}