package com.example.cardify.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility methods for geospatial calculations.
 */
object Geo {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /**
     * Calculates the distance in meters between two latitude/longitude pairs using the Haversine formula.
     */
    fun haversineDistanceMeters(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double
    ): Double {
        val latDelta = Math.toRadians(endLat - startLat)
        val lonDelta = Math.toRadians(endLon - startLon)
        val startLatRad = Math.toRadians(startLat)
        val endLatRad = Math.toRadians(endLat)

        val a = sin(latDelta / 2).pow(2) + cos(startLatRad) * cos(endLatRad) * sin(lonDelta / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }
}
