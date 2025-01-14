package dev.valerii.payflo.repository

import dev.valerii.payflo.model.User
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.encodeBase64

class ContactRepositoryImpl(
    private val httpClient: HttpClient
) : ContactRepository {
    override suspend fun getFriends(userId: String): List<User> =
        httpClient.get("$BASE_URL/users/$userId/friends").body()

    override suspend fun addFriend(userId: String, friendId: String): Result<Unit> =
        try {
            httpClient.post("$BASE_URL/users/$userId/friends") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("friendId" to friendId))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    companion object {
        // "http://0.0.0.0:8080"
        // "http://10.0.2.2:8080"
        private const val BASE_URL = "http://10.0.2.2:8080"
        private const val KEY_USER_ID = "user_id"

        fun base64Encode(bytes: ByteArray) = bytes.encodeBase64()
    }
}