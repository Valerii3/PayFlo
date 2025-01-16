package dev.valerii.payflo.model

import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String,
    val name: String,
    val amount: Double,
    val paidById: String,
    val participantIds: List<String>,
    val isBillAttached: Boolean,
    val billImage: String?,
    val participantShares: Map<String, Double> = emptyMap()
)

@Serializable
data class CreateExpenseRequest(
    val groupId: String,
    val name: String,
    val amount: Double,
    val creatorId: String,
    val participantIds: List<String>,
    val isBillAttached: Boolean,
    val billImage: String?
)

