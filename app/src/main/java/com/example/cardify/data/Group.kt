package com.example.cardify.data

/**
 * Represents a community group displayed in the map and list views.
 *
 * @param name Display name of the group.
 * @param tags Tags associated with the group.
 * @param latitude Latitude in decimal degrees.
 * @param longitude Longitude in decimal degrees.
 * @param distanceMeters Distance from the user in meters (computed at runtime).
 * @param sharedTagsCount Number of tags shared with the current user (computed at runtime).
 */
data class Group(
    val name: String,
    val tags: List<String>,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double = 0.0,
    val sharedTagsCount: Int = 0,
    val description: String? = null,
    val meetingTime: String? = null,
    val currentMembers: Int? = null,
    val maxMembers: Int? = null
)
