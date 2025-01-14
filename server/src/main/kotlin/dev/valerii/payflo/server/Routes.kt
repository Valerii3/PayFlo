package dev.valerii.payflo.server

import dev.valerii.payflo.server.database.Users
import io.ktor.server.routing.Route
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import java.util.UUID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

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
                }.firstOrNull()
        }

        if (user != null) {
            call.respond(user)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}