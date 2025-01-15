package dev.valerii.payflo.model

import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String,
    val groupId: String,
    val name: String,
    val amount: Double,
    val creatorId: String,
    val participants: List<String>, // List of user IDs
)
