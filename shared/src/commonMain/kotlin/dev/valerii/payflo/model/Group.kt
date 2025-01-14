package dev.valerii.payflo.model

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: String,
    val inviteCode: String,
    val name: String,
    val totalAmount: Double,
    val creatorId: String,
    val participants: List<User>
)
