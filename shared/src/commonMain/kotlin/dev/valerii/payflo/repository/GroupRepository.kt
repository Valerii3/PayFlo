package dev.valerii.payflo.repository

import dev.valerii.payflo.model.Group

interface GroupRepository {
    suspend fun createGroup(name: String, creatorId: String): Group
    suspend fun getGroupsForUser(userId: String): List<Group>
    suspend fun getGroup(id: String): Group?
    suspend fun joinGroup(inviteCode: String, userId: String): Result<Unit>
}