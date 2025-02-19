package dev.valerii.payflo.server

import dev.valerii.payflo.model.BillItem
import dev.valerii.payflo.model.CreateExpenseRequest
import dev.valerii.payflo.model.CreateGroupRequest
import dev.valerii.payflo.model.Expense
import dev.valerii.payflo.model.Group
import dev.valerii.payflo.model.OrderAnalysisRequest
import dev.valerii.payflo.model.User
import dev.valerii.payflo.server.database.BillItemAssignments
import dev.valerii.payflo.server.database.BillItems
import dev.valerii.payflo.server.database.Contacts
import dev.valerii.payflo.server.database.ExpenseParticipants
import dev.valerii.payflo.server.database.Expenses
import dev.valerii.payflo.server.database.GroupMembers
import dev.valerii.payflo.server.database.Groups
import dev.valerii.payflo.server.database.Users
import dev.valerii.payflo.server.llm.ChatGPT
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

@Suppress("TOO_LONG_FUNCTION")
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

    get("/users/{id}/groups") {
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
                    .selectAll()
                    .where { GroupMembers.groupId eq id }
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

            call.respond(group ?: HttpStatusCode.NotFound)
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
                    .selectAll()
                    .where { GroupMembers.groupId eq groupData[Groups.id] }
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

            call.respond(group ?: HttpStatusCode.NotFound)
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
                .selectAll()
                .where { GroupMembers.groupId eq id }
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
                    it[isBillAttached] = request.isBillAttached
                    it[billPhoto] = request.billImage
                }

                // Add participants and their shares
                request.participantIds.forEach { participantId ->
                    ExpenseParticipants.insert {
                        it[this.expenseId] = expenseId
                        it[userId] = participantId
                        it[this.share] = if (request.isBillAttached) 0.0 else {
                            (request.amount / request.participantIds.size).roundToTwoDecimals()
                        }
                    }
                }
            }

            if (request.isBillAttached && request.billImage != null) {
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                scope.launch {
                    try {
                        processBillWithLLM(expenseId, request.billImage!!)
                    } catch (e: Exception) {
                        e.printStackTrace()
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
                            .associate { it[ExpenseParticipants.userId] to it[ExpenseParticipants.share] }
                           // .map { it[ExpenseParticipants.userId] }

                        Expense(
                            id = expenseId,
                            name = row[Expenses.name],
                            amount = row[Expenses.amount],
                            paidById = row[Expenses.creatorId],
                            participantIds = participants.keys.toList(),
                            isBillAttached = row[Expenses.isBillAttached],
                            billImage = row[Expenses.billPhoto],
                            participantShares = participants
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

    get("/expenses/{expenseId}/items") {
        val expenseId = call.parameters["expenseId"]
            ?: return@get call.respond(HttpStatusCode.BadRequest)

        try {
            val items = transaction {
                BillItems
                    .selectAll()
                    .where { BillItems.expenseId eq expenseId }
                    .map { row ->
                        val itemId = row[BillItems.id]
                        // Get assigned users for this item
                        val assignedUsers = BillItemAssignments
                            .selectAll()
                            .where { BillItemAssignments.billItemId eq itemId }
                            .map { it[BillItemAssignments.userId] }

                        BillItem(
                            id = itemId,
                            expenseId = row[BillItems.expenseId],
                            name = row[BillItems.name],
                            price = row[BillItems.price],
                            quantity = row[BillItems.quantity],
                            totalPrice = row[BillItems.totalPrice],
                            assignedToUserIds = assignedUsers
                        )
                    }
            }
            call.respond(items)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }

    put("/bill-items/{itemId}/assignments/toggle") {
        val itemId = call.parameters["itemId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
        val userId = call.receive<Map<String, String>>()["userId"] ?: return@put call.respond(HttpStatusCode.BadRequest)

        transaction {
            val billItem = BillItems
                .selectAll()
                .where { BillItems.id eq itemId }
                .firstOrNull() ?: return@transaction

            val expenseId = billItem[BillItems.expenseId]

            // Check if assignment exists
            val existingAssignment = BillItemAssignments
                .selectAll().where { (BillItemAssignments.billItemId eq itemId) and (BillItemAssignments.userId eq userId) }
                .firstOrNull()

            @Suppress("AVOID_NULL_CHECKS")
            if (existingAssignment != null) {
                BillItemAssignments.deleteWhere {
                    (billItemId eq itemId) and (BillItemAssignments.userId eq userId)
                }

                // remove the user from the expense participants
                val remainingAssignments = BillItemAssignments
                    .selectAll()
                    .where { BillItemAssignments.userId eq userId }
                    .count()

                if (remainingAssignments == 0L) {
                    ExpenseParticipants.deleteWhere {
                        (ExpenseParticipants.expenseId eq expenseId) and
                                (ExpenseParticipants.userId eq userId)
                    }
                }
            } else {
                BillItemAssignments.insert {
                    it[billItemId] = itemId
                    it[this.userId] = userId
                }

                // add
                val participantExists = ExpenseParticipants
                    .selectAll()
                    .where {
                        (ExpenseParticipants.expenseId eq expenseId) and
                                (ExpenseParticipants.userId eq userId)
                    }.count() > 0

                if (!participantExists) {
                    // Calculate initial share (you might want to adjust this logic)
                    val itemAmount = billItem[BillItems.price]
                    ExpenseParticipants.insert {
                        it[this.expenseId] = expenseId
                        it[this.userId] = userId
                        it[share] = itemAmount
                    }
                }
            }
        }
        call.respond(HttpStatusCode.OK)
    }

    post("/analyze-order") {
        val chatGPT = ChatGPT()
        val request = call.receive<OrderAnalysisRequest>()

        val matchedItemIds = chatGPT.analyzeOrder(
            orderDescription = request.orderDescription,
            billItems = request.billItems
        )
        call.respond(matchedItemIds)
    }
}

fun generateUniqueInviteCode(): String {
    @Suppress("AVOID_NESTED_FUNCTIONS")
    fun generateCode() = (100_000..999_999).random().toString()

    var code = generateCode()
    // Keep generating until we find an unused code
    transaction {
        while (Groups.selectAll().where(Groups.inviteCode eq code).count() > 0) {
            code = generateCode()
        }
    }
    return code
}

@Suppress("FUNCTION_NAME_INCORRECT_CASE")
suspend fun processBillWithLLM(expenseId: String, billImage: String) {
    val chatGpt = ChatGPT()
    try {
        val billData = chatGpt.processBillImage(billImage)

        transaction {
            // Update the expense total amount if needed
            Expenses.update({ Expenses.id eq expenseId }) {
                it[amount] = billData.total
            }

            // Store the individual items
            billData.items.forEach { item ->
                BillItems.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[this.expenseId] = expenseId
                    it[name] = item.name
                    it[price] = item.price
                    it[quantity] = item.quantity
                    it[totalPrice] = item.totalPrice
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Suppress("FLOAT_IN_ACCURATE_CALCULATIONS")
fun Double.roundToTwoDecimals(): Double =
    (this * 100).toInt() / 100.0
