package dev.valerii.payflo.server

import dev.valerii.payflo.model.BillItem
import dev.valerii.payflo.server.database.Contacts
import dev.valerii.payflo.server.database.ExpenseParticipants
import dev.valerii.payflo.server.database.Expenses
import dev.valerii.payflo.server.database.GroupMembers
import dev.valerii.payflo.server.database.Groups
import dev.valerii.payflo.server.database.Users

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun main() {
    embeddedServer(Netty, port = 8080) {
        configureRouting()
        configureSerialization()
        configureDatabases()
    }.start(wait = true)
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}

fun Application.configureRouting() {
    routing {
        userRoutes()
    }
}

fun Application.configureDatabases() {
    // Connect to SQLite Database
    Database.connect(
        url = "jdbc:sqlite:data.db",
        driver = "org.sqlite.JDBC"
    )

    // Create tables
    transaction {
        SchemaUtils.create(Users)
        SchemaUtils.create(Groups)
        SchemaUtils.create(GroupMembers)
        SchemaUtils.create(Contacts)
        SchemaUtils.create(Expenses)
        SchemaUtils.create(ExpenseParticipants)


    }
}