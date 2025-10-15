package com.example.cardify.data

/**
 * Payload for creating a new group via the backend service.
 */
data class CreateGroupRequest(
    val title: String,
    val description: String,
    val location: String,
    val maxPeople: Int,
    val latitude: Double? = null,
    val longitude: Double? = null
)
