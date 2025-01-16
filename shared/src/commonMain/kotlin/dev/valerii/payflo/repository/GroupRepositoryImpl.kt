package dev.valerii.payflo.repository

import dev.valerii.payflo.model.BillItem
import dev.valerii.payflo.model.CreateExpenseRequest
import dev.valerii.payflo.model.CreateGroupRequest
import dev.valerii.payflo.model.Expense
import dev.valerii.payflo.model.Group
import dev.valerii.payflo.model.OrderAnalysisRequest
import dev.valerii.payflo.model.UpdateGroupRequest
import dev.valerii.payflo.repository.RepositoryConstants.BASE_URL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
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

        val creationResponse = response.body<Map<String, String>>()
        val groupId = creationResponse["id"] ?: throw Exception("Group ID not returned")

        // Then fetch and return the complete group
        return getGroup(groupId) ?: throw Exception("Could not fetch created group")
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
            val joinResponse = httpClient.post("$BASE_URL/groups/join") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "inviteCode" to inviteCode,
                    "userId" to userId
                ))
            }

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

    override suspend fun updateGroup(groupId: String, name: String?, photo: String?): Group {
        val response = httpClient.put("$BASE_URL/groups/$groupId") {
            contentType(ContentType.Application.Json)
            setBody(UpdateGroupRequest(
                name = name,
                photo = photo
            ))
        }

        val group = response.body<Group>()
        return group
    }

    @Suppress("TOO_MANY_PARAMETERS")
    override suspend fun addExpense(
        groupId: String,
        name: String,
        amount: Double,
        creatorId: String,
        participantIds: List<String>,
        isBillAttached: Boolean,
        billImage: String?
    ): Result<String> = try {
        val request = CreateExpenseRequest(
            groupId = groupId,
            name = name,
            amount = amount,
            creatorId = creatorId,
            participantIds = participantIds,
            isBillAttached = isBillAttached,
            billImage = billImage
        )

        val response = httpClient.post("$BASE_URL/expenses") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (response.status.isSuccess()) {
            val result = response.body<Map<String, String>>()
            Result.success(result["expenseId"]!!)
        } else {
            Result.failure(Exception("Failed to create expense"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getGroupExpenses(groupId: String): Result<List<Expense>> = try {
        val response = httpClient.get("$BASE_URL/groups/$groupId/expenses")
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            Result.failure(Exception("Failed to fetch expenses"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getBillItemsForExpense(expenseId: String): List<BillItem> =
        httpClient.get("$BASE_URL/expenses/$expenseId/items").body()

    override suspend fun toggleBillItemAssignment(itemId: String, userId: String) {
        httpClient.put("$BASE_URL/bill-items/$itemId/assignments/toggle") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("userId" to userId))
        }
    }

    override suspend fun assignItemsByDescription(
        billItems: List<BillItem>,
        orderDescription: String,
        userId: String
    ) {
        try {
            val request = OrderAnalysisRequest(
                orderDescription = orderDescription,
                billItems = billItems
            )

            val matchedItemIds = httpClient.post("$BASE_URL/analyze-order") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<List<String>>()

            // Then toggle assignments for each matched item
            matchedItemIds.forEach { itemId ->
                toggleBillItemAssignment(itemId, userId)
            }
        } catch (e: Exception) {
            throw e
        }
    }
}