package dev.valerii.payflo.repository

import dev.valerii.payflo.model.User
import dev.valerii.payflo.model.UserCredentials

interface UserRepository {
    suspend fun createUser(name: String): User
    suspend fun getUser(id: String): User?
    suspend fun updateUser(user: User): User
    suspend fun getSavedCredentials(): UserCredentials?
    suspend fun saveCredentials(credentials: UserCredentials)
}