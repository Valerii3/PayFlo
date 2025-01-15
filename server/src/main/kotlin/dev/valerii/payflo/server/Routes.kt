package dev.valerii.payflo.server

import dev.valerii.payflo.model.CreateExpenseRequest
import dev.valerii.payflo.model.CreateGroupRequest
import dev.valerii.payflo.model.Expense
import dev.valerii.payflo.model.Group
import dev.valerii.payflo.model.User
import dev.valerii.payflo.server.database.Contacts
import dev.valerii.payflo.server.database.ExpenseParticipants
import dev.valerii.payflo.server.database.Expenses
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
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
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
        val request = call.receive<CreateGroupRequest>()
        // Generate necessary values for the group
        val groupId = UUID.randomUUID().toString()
        val inviteCode = generateUniqueInviteCode()
        try {
            // Use a transaction block to insert into the database
            val group = transaction {
                // Insert group into the Groups table
                Groups.insert {
                    it[id] = groupId
                    it[name] = request.name
                    it[photo] = request.photo
                    it[this.inviteCode] = inviteCode
                    it[totalAmount] = request.totalAmount
                    it[creatorId] = request.creatorId
                }

                // Add all members (including the creator) to the GroupMembers table
                val membersList = (request.memberIds + request.creatorId).distinct()
                membersList.forEach { memberId ->
                    GroupMembers.insert {
                        it[this.groupId] = groupId
                        it[userId] = memberId
                    }
                }

                // Return group info with participant IDs
                mapOf(
                    "id" to groupId,
                )
            }

            call.respond(HttpStatusCode.Created, group)
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request parameters")
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "An unexpected error occurred")
        }
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
                                User(
                                    id = participantRow[Users.id],
                                    name = participantRow[Users.name],
                                    profilePicture = participantRow[Users.profilePicture]
                                )
                            }

                        Group(
                            id = groupId,
                            inviteCode = groupRow[Groups.inviteCode],
                            name = groupRow[Groups.name],
                            totalAmount = groupRow[Groups.totalAmount],
                            creatorId = groupRow[Groups.creatorId],
                            participants = participants
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
                        User(
                            id = row[Users.id],
                            name = row[Users.name],
                            profilePicture = row[Users.profilePicture]
                        )
                    }

                Group(
                    id = groupData[Groups.id],
                    inviteCode = groupData[Groups.inviteCode],
                    name = groupData[Groups.name],
                    photo = groupData[Groups.photo],
                    totalAmount = groupData[Groups.totalAmount],
                    creatorId = groupData[Groups.creatorId],
                    participants = participants
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

    get("/users/{id}/friends") {
        val userId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

        try {
            val friends = transaction {
                val friendUser = Users.alias("friend_user")  // Create alias for Users table

                Contacts
                    .join(friendUser, JoinType.INNER, onColumn = Contacts.friendId, otherColumn = friendUser[Users.id])
                    .select(
                        friendUser[Users.id],
                        friendUser[Users.name],
                        friendUser[Users.profilePicture]
                    )
                    .where { Contacts.userId eq userId }
                    .map { row ->
                        hashMapOf(
                            "id" to row[friendUser[Users.id]],
                            "name" to row[friendUser[Users.name]],
                            "profilePicture" to row[friendUser[Users.profilePicture]]
                        )
                    }
            }

            call.respond(friends)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    get("/groups/by-invite-code/{code}") {
        val code = call.parameters["code"] ?: return@get call.respond(HttpStatusCode.BadRequest)

        try {
            val group = transaction {
                val groupData = Groups
                    .selectAll().where { Groups.inviteCode eq code }
                    .firstOrNull() ?: return@transaction null

                val participants = GroupMembers
                    .innerJoin(Users)
                    .selectAll().where { GroupMembers.groupId eq groupData[Groups.id] }
                    .map { row ->
                        User(
                            id = row[Users.id],
                            name = row[Users.name],
                            profilePicture = row[Users.profilePicture]
                        )
                    }

                Group(
                    id = groupData[Groups.id],
                    inviteCode = groupData[Groups.inviteCode],
                    name = groupData[Groups.name],
                    totalAmount = groupData[Groups.totalAmount],
                    creatorId = groupData[Groups.creatorId],
                    participants = participants
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

    post("/users/{id}/friends") {
        val userId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val friendId = call.receive<Map<String, String>>()["friendId"]
            ?: return@post call.respond(HttpStatusCode.BadRequest)

        try {
            transaction {
                Contacts.insert {
                    it[this.userId] = userId
                    it[this.friendId] = friendId
                }
            }
            call.respond(HttpStatusCode.OK)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    post("/groups/join") {
        val params = call.receive<Map<String, String>>()
        val inviteCode = params["inviteCode"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Invite code is required")
        val userId = params["userId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "User ID is required")

        try {
            val group = transaction {
                // Find group by invite code
                val groupData = Groups
                    .selectAll().where { Groups.inviteCode eq inviteCode }
                    .firstOrNull() ?: return@transaction null

                // Check if user is already a member
                val isAlreadyMember = GroupMembers
                    .selectAll()
                    .where {
                        GroupMembers.groupId.eq(groupData[Groups.id])
                            .and(GroupMembers.userId.eq(userId))
                    }
                    .count() > 0

                if (!isAlreadyMember) {
                    // Add user to group members
                    GroupMembers.insert {
                        it[this.groupId] = groupData[Groups.id]
                        it[this.userId] = userId
                    }
                }

                // Return group data
                groupData[Groups.id]
            } ?: return@post call.respond(HttpStatusCode.NotFound, "Group not found")

            call.respond(HttpStatusCode.OK, mapOf("groupId" to group))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "An unexpected error occurred")
        }
    }

    // Add this inside the Route.userRoutes() function

    put("/groups/{id}") {
        val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
        val groupUpdate = call.receive<Map<String, String>>()  // Changed to Map<String, String>

        val updatedGroup = transaction {
            // Update group information
            Groups.update({ Groups.id eq id }) {
                groupUpdate["name"]?.let { name -> it[Groups.name] = name }
                groupUpdate["photo"]?.let { photo -> it[Groups.photo] = photo }
            }

            // Fetch updated group with participants
            val groupData = Groups
                .selectAll().where { Groups.id eq id }
                .firstOrNull() ?: return@transaction null

            val participants = GroupMembers
                .innerJoin(Users)
                .selectAll().where { GroupMembers.groupId eq id }
                .map { row ->
                    User(
                        id = row[Users.id],
                        name = row[Users.name],
                        profilePicture = row[Users.profilePicture]
                    )
                }

            Group(
                id = groupData[Groups.id],
                inviteCode = groupData[Groups.inviteCode],
                name = groupData[Groups.name],
                photo = groupData[Groups.photo],
                totalAmount = groupData[Groups.totalAmount],
                creatorId = groupData[Groups.creatorId],
                participants = participants
            )
        }

        updatedGroup?.let {
            call.respond(it)
        } ?: call.respond(HttpStatusCode.NotFound)
    }

    post("/expenses") {
        val request = call.receive<CreateExpenseRequest>()
        try {
            val expenseId = UUID.randomUUID().toString()
            val share = request.amount / request.participantIds.size

            transaction {
                // Create the expense
                Expenses.insert {
                    it[id] = expenseId
                    it[groupId] = request.groupId
                    it[name] = request.name
                    it[amount] = request.amount
                    it[creatorId] = request.creatorId
                }

                // Add participants and their shares
                request.participantIds.forEach { participantId ->
                    ExpenseParticipants.insert {
                        it[this.expenseId] = expenseId
                        it[userId] = participantId
                        it[this.share] = share
                    }
                }
            }

            call.respond(HttpStatusCode.Created, mapOf("expenseId" to expenseId))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    get("/groups/{groupId}/expenses") {
        val groupId = call.parameters["groupId"] ?: return@get call.respond(HttpStatusCode.BadRequest)

        try {
            val expenses = transaction {
                val expensesList = Expenses
                    .selectAll()
                    .where { Expenses.groupId eq groupId }
                    .map { row ->
                        val expenseId = row[Expenses.id]

                        // Get participants for this expense
                        val participants = ExpenseParticipants
                            .selectAll()
                            .where { ExpenseParticipants.expenseId eq expenseId }
                            .map { it[ExpenseParticipants.userId] }

                        Expense(
                            id = expenseId,
                            name = row[Expenses.name],
                            amount = row[Expenses.amount],
                            paidById = row[Expenses.creatorId],
                            participantIds = participants
                        )
                    }

                expensesList
            }

            call.respond(expenses)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }
}

fun generateUniqueInviteCode(): String {
    fun generateCode() = (100000..999999).random().toString()

    var code = generateCode()
    // Keep generating until we find an unused code
    transaction {
        while (Groups.selectAll().where(Groups.inviteCode eq code).count() > 0) {
            code = generateCode()
        }
    }
    return code
}
