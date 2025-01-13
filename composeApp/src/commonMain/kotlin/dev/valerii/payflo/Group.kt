package dev.valerii.payflo

import dev.valerii.payflo.screen.Friend

data class Group(
    val id: String,
    val name: String,
    val participants: List<Friend>,
    val totalAmount: Double = 0.0
)
