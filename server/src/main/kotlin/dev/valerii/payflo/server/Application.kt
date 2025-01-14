package dev.valerii.payflo.server

/*import dev.valerii.payflo.Greeting
import dev.valerii.payflo.SERVER_PORT*/
import dev.valerii.payflo.server.database.Users
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun main() {
    embeddedServer(Netty, port = 8080/*host = "0.0.0.0", module = Application::module*/) {
        configureRouting()
        configureSerialization()
        configureDatabases()
    }.start(wait = true)
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
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
    }
}