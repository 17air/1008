package com.example.cardify.ui

import android.content.Context
import com.example.cardify.R
import com.example.cardify.data.Group
import com.example.cardify.data.User
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

/**
 * Handles creating and updating map markers and camera movements.
 */
class MapBinder(
    private val context: Context,
    private val googleMap: GoogleMap
) {

    private val markerMap = mutableMapOf<String, Marker>()

    fun renderMarkers(
        user: User,
        groups: List<Group>
    ) {
        googleMap.clear()
        markerMap.clear()

        val boundsBuilder = LatLngBounds.Builder()
        var hasBounds = false

        val userLatLng = LatLng(user.latitude, user.longitude)
        val userMarker = googleMap.addMarker(
            MarkerOptions()
                .position(userLatLng)
                .title(context.getString(R.string.user_location_marker_title))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )
        if (userMarker != null) {
            hasBounds = true
            boundsBuilder.include(userLatLng)
        }

        groups.forEach { group ->
            val position = LatLng(group.latitude, group.longitude)
            val tagsText = context.getString(
                R.string.group_tags_label,
                group.tags.joinToString(separator = ", ")
            )
            val sharedText = context.getString(
                R.string.group_shared_tags_label,
                group.sharedTagsCount
            )
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(group.name)
                    .snippet("$tagsText\n$sharedText")
                    .icon(
                        BitmapDescriptorFactory.defaultMarker(
                            if (group.sharedTagsCount > 0) {
                                BitmapDescriptorFactory.HUE_ROSE
                            } else {
                                BitmapDescriptorFactory.HUE_ORANGE
                            }
                        )
                    )
            )
            if (marker != null) {
                marker.tag = group.name
                markerMap[group.name] = marker
                boundsBuilder.include(position)
                hasBounds = true
            }
        }

        if (hasBounds) {
            val bounds = try {
                boundsBuilder.build()
            } catch (exception: IllegalStateException) {
                null
            }
            if (bounds != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, MAP_PADDING))
            } else {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, DEFAULT_ZOOM))
            }
        }
    }

    fun focusOnGroup(group: Group) {
        val marker = markerMap[group.name] ?: return
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, FOCUS_ZOOM))
        marker.showInfoWindow()
    }

    companion object {
        private const val MAP_PADDING = 120
        private const val DEFAULT_ZOOM = 13f
        private const val FOCUS_ZOOM = 15f
    }
}
