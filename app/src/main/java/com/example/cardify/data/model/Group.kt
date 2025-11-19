package com.example.cardify.data.model

data class Group(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val leaderId: String = "",
    val leaderName: String = "",
    val members: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val memberDisplayNames: Map<String, String> = emptyMap()
)
