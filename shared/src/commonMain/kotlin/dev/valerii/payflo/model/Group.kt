package dev.valerii.payflo.model

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: String,
    val inviteCode: String,
    val name: String,
    val photo: String? = null,
    val totalAmount: Double,
    val creatorId: String,
    val participants: List<User>
)

@Serializable
data class CreateGroupRequest(
    val name: String,
    val creatorId: String,
    val memberIds: List<String> = emptyList(),
    val photo: String? = null,
    val totalAmount: Double = 0.0
)
