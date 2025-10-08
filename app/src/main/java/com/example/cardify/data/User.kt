package com.example.cardify.data

/**
 * Represents the current user with a set of tags and coordinates.
 */
data class User(
    val tags: List<String>,
    val latitude: Double,
    val longitude: Double
)
