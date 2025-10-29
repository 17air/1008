package com.example.cardify.data.model

data class Group(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val maxPeople: Int = 10,
    val currentPeople: Int = 0,
    val ownerId: String = "",
    val ownerName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
