package com.example.cardify.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val date: String,
    val leaderId: String,
    val leaderName: String,
    val tags: List<String>,
    val members: List<String>,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long,
    val memberDisplayNames: Map<String, String>
)
