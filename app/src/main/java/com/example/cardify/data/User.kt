package com.example.cardify.data

/**
 * Represents the current user with an associated location and interest tags.
 */
data class User(
    val tags: List<String>,
    val latitude: Double,
    val longitude: Double
)
