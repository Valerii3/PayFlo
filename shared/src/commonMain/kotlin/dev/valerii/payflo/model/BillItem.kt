package dev.valerii.payflo.model

import kotlinx.serialization.Serializable

@Serializable
data class BillItem(
    val id: String,
    val name: String,
    val price: Double,
    val quantity: Int = 1,
    val totalPrice: Double,
    val expenseId: String,
    val assignedToUserIds: List<String> = emptyList()
)
