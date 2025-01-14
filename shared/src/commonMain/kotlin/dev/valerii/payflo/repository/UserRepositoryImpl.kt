package dev.valerii.payflo.repository

import dev.valerii.payflo.model.User
import dev.valerii.payflo.model.UserCredentials
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import dev.valerii.payflo.storage.SettingsStorage
import io.ktor.util.encodeBase64

class UserRepositoryImpl(
    private val httpClient: HttpClient,
    private val settingsStorage: SettingsStorage
) : UserRepository {

    override suspend fun createUser(name: String): User {
        // Make API call to create user
        val response = httpClient.post("$BASE_URL/users") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "name" to name,
            ))
        }

        val userId = response.body<Map<String, String>>()["userId"]
            ?: throw IllegalStateException("Server did not return a userId")

        return User(id = userId, name = name)
    }

    override suspend fun getUser(id: String): User? {
        return try {
            httpClient.get("$BASE_URL/users/$id").body()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateUser(user: User): User {
        return httpClient.put("$BASE_URL/users/${user.id}") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.body()
    }

    override suspend fun getSavedCredentials(): UserCredentials? {
        val userId = settingsStorage.getString(KEY_USER_ID)
        return userId?.let { UserCredentials(it) }
    }

    override suspend fun saveCredentials(credentials: UserCredentials) {
        settingsStorage.putString(KEY_USER_ID, credentials.userId)
    }

    companion object {
        // "http://0.0.0.0:8080"
        // "http://10.0.2.2:8080"
        private const val BASE_URL = "http://0.0.0.0:8080" // Change for production
        private const val KEY_USER_ID = "user_id"

        private fun base64Encode(bytes: ByteArray): String {
            return bytes.encodeBase64()
        }
    }
}