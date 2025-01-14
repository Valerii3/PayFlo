package dev.valerii.payflo.server.database

import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = varchar("id", 128)
    val name = varchar("name", 255)
    val profilePicture = text("profile_picture").nullable()

    override val primaryKey = PrimaryKey(id)
}