package dev.valerii.payflo.model

import kotlinx.serialization.Serializable

@Serializable
data class BillItem(
    val name: String,
    val price: Double,
    val quantity: Int = 1,  // Added quantity field
    val totalPrice: Double  // Price * quantity
)
