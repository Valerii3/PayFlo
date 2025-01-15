package dev.valerii.payflo.repository

import dev.valerii.payflo.model.User

interface ContactRepository {
    suspend fun getFriends(userId: String): List<User>
    suspend fun addFriend(userId: String, friendId: String): Boolean
}