package com.example.cardify.data

/**
 * Represents a group marker with descriptive metadata for display on the Kakao Map.
 */
data class Group(
    val name: String,
    val tags: List<String>,
    val latitude: Double,
    val longitude: Double
)
