package com.example.cardify.data.local

import androidx.room.Entity

@Entity(tableName = "memberships", primaryKeys = ["groupId", "userId"])
data class MembershipEntity(
    val groupId: String,
    val userId: String,
    val joinedAt: Long
)
