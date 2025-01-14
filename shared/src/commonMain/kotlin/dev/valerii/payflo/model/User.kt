package dev.valerii.payflo.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val profilePicture: String? = null
)
