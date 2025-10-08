package com.example.cardify.data

/**
 * Represents a group on the map with descriptive metadata.
 *
 * @property name The display name of the group.
 * @property tags The tags that describe the group.
 * @property latitude Latitude coordinate in decimal degrees.
 * @property longitude Longitude coordinate in decimal degrees.
 * @property distanceMeters Precomputed distance from the user in meters.
 * @property sharedTagsCount Number of tags shared with the user.
 */
data class Group(
    val name: String,
    val tags: List<String>,
    val latitude: Double,
    val longitude: Double,
    var distanceMeters: Double = 0.0,
    var sharedTagsCount: Int = 0
)
