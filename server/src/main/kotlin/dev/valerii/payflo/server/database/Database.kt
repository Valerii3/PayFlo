package dev.valerii.payflo.server.database

import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = varchar("id", 128)
    val name = varchar("name", 255)
    val profilePicture = text("profile_picture").nullable()
    override val primaryKey = PrimaryKey(id)
}

object Groups : Table() {
    val id = varchar("id", 128)
    val inviteCode = varchar("invite_code", 6).uniqueIndex()  // Add unique constraint
    val name = varchar("name", 255)
    val photo = text("photo").nullable()
    val totalAmount = double("total_amount")
    val creatorId = varchar("creator_id", 128).references(Users.id)
    override val primaryKey = PrimaryKey(id)
}

object GroupMembers : Table() {
    val groupId = varchar("group_id", 128).references(Groups.id)
    val userId = varchar("user_id", 128).references(Users.id)
    override val primaryKey = PrimaryKey(groupId, userId)
}

object Contacts : Table() {
    val userId = varchar("user_id", 128).references(Users.id)
    val friendId = varchar("friend_id", 128).references(Users.id)
    override val primaryKey = PrimaryKey(userId, friendId)
}