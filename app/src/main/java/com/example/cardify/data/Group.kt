package com.example.cardify.data

/**
 * Represents a locally cached group shown on the map and in the list.
 */
data class Group(
    val title: String,
    val description: String,
    val location: String,
    val maxPeople: Int,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double = 0.0
)
