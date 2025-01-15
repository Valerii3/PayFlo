package dev.valerii.payflo.repository

import dev.valerii.payflo.model.Expense
import dev.valerii.payflo.model.Group

interface GroupRepository {
    suspend fun createGroup(
        name: String,
        photo: String?,
        creatorId: String,
        memberIds: List<String>
    ): Group
    suspend fun getGroupsForUser(userId: String): List<Group>
    suspend fun getGroup(id: String): Group?
    suspend fun joinGroup(inviteCode: String, userId: String): Result<Group>
    suspend fun updateGroup(groupId: String, name: String?, photo: String?): Group
    suspend fun addExpense(
        groupId: String,
        name: String,
        amount: Double,
        creatorId: String,
        participantIds: List<String>
    ): Result<String>
    suspend fun getGroupExpenses(groupId: String): Result<List<Expense>>
}