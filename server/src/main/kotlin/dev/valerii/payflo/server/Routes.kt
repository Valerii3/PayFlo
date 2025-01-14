package dev.valerii.payflo.server

import dev.valerii.payflo.server.database.Groups
import dev.valerii.payflo.server.database.GroupMembers
import dev.valerii.payflo.server.database.Users
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

fun Route.userRoutes() {
    post("/users") {
        val params = call.receive<Map<String, String>>()
        val userId = UUID.randomUUID().toString()

        transaction {
            Users.insert {
                it[id] = userId
                it[name] = params["name"] ?: throw IllegalArgumentException("Name is required")
                it[profilePicture] = params["profilePicture"]
            }
        }

        call.respond(hashMapOf("userId" to userId))
    }

    get("/users/{id}") {
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val user = transaction {
            Users.selectAll()
                .where { Users.id eq id }
                .map {
                    hashMapOf(
                        "id" to it[Users.id],
                        "name" to it[Users.name],
                        "profilePicture" to it[Users.profilePicture]
                    )
                }
                .firstOrNull()
        }

        user?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NotFound)
    }

    put("/users/{id}") {
        val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
        val user = call.receive<Map<String, String>>()

        val updatedUser = transaction {
            Users.update({ Users.id eq id }) {
                user["name"]?.let { name -> it[Users.name] = name }
                user["profilePicture"]?.let { picture -> it[Users.profilePicture] = picture }
            }

            Users.selectAll()
                .where { Users.id eq id }
                .map {
                    hashMapOf(
                        "id" to it[Users.id],
                        "name" to it[Users.name],
                        "profilePicture" to it[Users.profilePicture]
                    )
                }
                .firstOrNull()
        }

        updatedUser?.let {
            call.respond(it)
        } ?: call.respond(HttpStatusCode.NotFound)
    }

    post("/groups") {
        val params = call.receive<Map<String, Any>>()
        val groupId = UUID.randomUUID().toString()
        val creatorId = params["creatorId"] as? String
            ?: throw IllegalArgumentException("Creator ID is required")

        val inviteCode = generateUniqueInviteCode()

        transaction {
            Groups.insert {
                it[id] = groupId
                it[name] = params["name"] as? String
                    ?: throw IllegalArgumentException("Name is required")
                it[this.inviteCode] = inviteCode
                it[totalAmount] = params["totalAmount"] as? Double ?: 0.0
                it[Groups.creatorId] = creatorId
            }

            // Add creator as first member
            GroupMembers.insert {
                it[this.groupId] = groupId
                it[userId] = creatorId
            }
        }

        call.respond(hashMapOf(
            "groupId" to groupId,
            "inviteCode" to inviteCode
        ))
    }

    get("/users/{id}/groups") {  // Removed trailing slash as it's not needed
        val userId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

        try {
            val userGroups = transaction {
                // Get all groups where user is a member
                (Groups innerJoin GroupMembers)
                    .selectAll().where { GroupMembers.userId eq userId }
                    .map { groupRow ->
                        val groupId = groupRow[Groups.id]

                        // Get all participants for each group
                        val participants = GroupMembers
                            .innerJoin(Users)
                            .selectAll().where { GroupMembers.groupId eq groupId }
                            .map { participantRow ->
                                hashMapOf(
                                    "id" to participantRow[Users.id],
                                    "name" to participantRow[Users.name],
                                    "profilePicture" to participantRow[Users.profilePicture]
                                )
                            }

                        hashMapOf(
                            "id" to groupId,
                            "inviteCode" to groupRow[Groups.inviteCode],
                            "name" to groupRow[Groups.name],
                            "totalAmount" to groupRow[Groups.totalAmount],
                            "creatorId" to groupRow[Groups.creatorId],
                            "participants" to participants
                        )
                    }
            }

            call.respond(userGroups)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    get("/groups/{id}") {
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

        try {
            val group = transaction {
                val groupData = Groups
                    .selectAll().where { Groups.id eq id }
                    .firstOrNull() ?: return@transaction null

                val participants = GroupMembers
                    .innerJoin(Users)
                    .selectAll().where { GroupMembers.groupId eq id }
                    .map { row ->
                        hashMapOf(
                            "id" to row[Users.id],
                            "name" to row[Users.name],
                            "profilePicture" to row[Users.profilePicture]
                        )
                    }

                hashMapOf(
                    "id" to groupData[Groups.id],
                    "inviteCode" to groupData[Groups.inviteCode],
                    "name" to groupData[Groups.name],
                    "totalAmount" to groupData[Groups.totalAmount],
                    "creatorId" to groupData[Groups.creatorId],
                    "participants" to participants
                )
            }

            if (group == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(group)
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }
}

fun generateUniqueInviteCode(): String {
    fun generateCode() = (100000..999999).random().toString()

    var code = generateCode()
    // Keep generating until we find an unused code
    transaction {
        while (Groups.select(Groups.inviteCode eq code).count() > 0) {
            code = generateCode()
        }
    }
    return code
}


