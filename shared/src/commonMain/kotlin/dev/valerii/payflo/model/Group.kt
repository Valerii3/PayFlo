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
    val participants: List<User>,
    val expenses: List<Expense> = emptyList()
)

@Serializable
data class CreateGroupRequest(
    val name: String,
    val creatorId: String,
    val memberIds: List<String> = emptyList(),
    val photo: String? = null,
    val totalAmount: Double = 0.0
)

@Serializable
data class UpdateGroupRequest(
    val name: String? = null,
    val photo: String? = null
)
