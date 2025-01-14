package dev.valerii.payflo.repository

import dev.valerii.payflo.model.User
import dev.valerii.payflo.model.UserCredentials
import dev.valerii.payflo.storage.SettingsStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
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

    override suspend fun getUser(id: String): User? = try {
        httpClient.get("$BASE_URL/users/$id").body()
    } catch (e: Exception) {
        null
    }

    override suspend fun updateUser(user: User): User =
        httpClient.put("$BASE_URL/users/${user.id}") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.body()

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
        private const val BASE_URL = "http://0.0.0.0:8080"
        private const val KEY_USER_ID = "user_id"

        private fun base64Encode(bytes: ByteArray) = bytes.encodeBase64()
    }
}