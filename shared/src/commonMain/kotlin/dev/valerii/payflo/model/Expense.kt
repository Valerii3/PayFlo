package dev.valerii.payflo.model

import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String,
    val name: String,
    val amount: Double,
    val paidById: String, // who paid
    val participantIds: List<String> // who needs to pay
)

@Serializable
data class CreateExpenseRequest(
    val groupId: String,
    val name: String,
    val amount: Double,
    val creatorId: String,
    val participantIds: List<String>
)

