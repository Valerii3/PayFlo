package dev.valerii.payflo.repository

import dev.valerii.payflo.model.User
import dev.valerii.payflo.repository.RepositoryConstants.BASE_URL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class ContactRepositoryImpl(
    private val httpClient: HttpClient
) : ContactRepository {
    override suspend fun getFriends(userId: String): List<User> =
        httpClient.get("$BASE_URL/users/$userId/friends").body()

    override suspend fun addFriend(userId: String, friendId: String): Boolean {
        val response = httpClient.post("$BASE_URL/users/$userId/friends") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("friendId" to friendId))
        }
        return response.status.isSuccess()
    }
}