package dev.valerii.payflo.model

import kotlinx.serialization.Serializable

@Serializable
data class UserCredentials(
    val userId: String
)
