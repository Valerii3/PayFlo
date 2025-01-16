package dev.valerii.payflo.model

import kotlinx.serialization.Serializable

@Suppress("CLASS_NAME_INCORRECT")
@Serializable
data class BillItemGPT(
    val name: String,
    val price: Double,
    val quantity: Int = 1,
    val totalPrice: Double,
)