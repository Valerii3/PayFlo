package dev.valerii.payflo.repository

import dev.valerii.payflo.model.Group
import dev.valerii.payflo.storage.SettingsStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.encodeBase64

class GroupRepositoryImpl(
    private val httpClient: HttpClient,
    private val settingsStorage: SettingsStorage
) : GroupRepository {
    override suspend fun createGroup(name: String, creatorId: String): Group {
        val response = httpClient.post("$BASE_URL/groups") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "name" to name,
                "creatorId" to creatorId,
                "totalAmount" to 0.0
            ))
        }

        return response.body()
    }

    override suspend fun getGroupsForUser(userId: String): List<Group> =
        httpClient.get("$BASE_URL/users/$userId/groups").body()

    override suspend fun getGroup(id: String): Group? =
        try {
            httpClient.get("$BASE_URL/groups/$id").body()
        } catch (e: Exception) {
            null
        }

    override suspend fun joinGroup(inviteCode: String, userId: String): Result<Unit> =
        try {
            httpClient.post("$BASE_URL/groups/join") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "inviteCode" to inviteCode,
                    "userId" to userId
                ))
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