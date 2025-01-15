package dev.valerii.payflo.model

import kotlinx.serialization.Serializable

@Serializable
data class BillData(
    val total: Double,
    val items: List<BillItem>,
)


