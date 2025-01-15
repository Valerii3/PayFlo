package dev.valerii.payflo.repository

import dev.valerii.payflo.model.CreateGroupRequest
import dev.valerii.payflo.model.Group
import dev.valerii.payflo.repository.RepositoryConstants.BASE_URL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class GroupRepositoryImpl(
    private val httpClient: HttpClient,
) : GroupRepository {

    override suspend fun createGroup(
        name: String,
        photo: String?,
        creatorId: String,
        memberIds: List<String>
    ): Group {
        val request = CreateGroupRequest(
            name = name,
            photo = photo,
            creatorId = creatorId,
            memberIds = memberIds,
            totalAmount = 0.0
        )

        val response = httpClient.post("$BASE_URL/groups") {
            contentType(ContentType.Application.Json)
            setBody(request)
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

    override suspend fun joinGroup(inviteCode: String, userId: String): Result<Group> =
        try {
            println("Attempting to join group with code: $inviteCode")
            val joinResponse = httpClient.post("$BASE_URL/groups/join") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "inviteCode" to inviteCode,
                    "userId" to userId
                ))
            }
            println("RESPONSE IS ${joinResponse.status}")
            // After joining, fetch the group details
            if (joinResponse.status.isSuccess()) {
                val group = httpClient.get("$BASE_URL/groups/by-invite-code/$inviteCode").body<Group>()
                Result.success(group)
            } else {
                Result.failure(Exception("Failed to join group"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

}