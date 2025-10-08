package com.example.cardify.ui

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
 * Helper responsible for creating markers and synchronising camera updates.
 */
class MapBinder {

    private var googleMap: GoogleMap? = null
    private val groupMarkers = mutableMapOf<String, Marker>()
    private var userMarker: Marker? = null

    fun attachMap(map: GoogleMap) {
        googleMap = map.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isMapToolbarEnabled = true
        }
    }

    fun render(groups: List<Group>, user: User?) {
        val map = googleMap ?: return
        map.clear()
        groupMarkers.clear()
        userMarker = null

        val boundsBuilder = LatLngBounds.Builder()
        var hasPoints = false

        user?.let {
            val userLatLng = LatLng(it.latitude, it.longitude)
            userMarker = map.addMarker(
                MarkerOptions()
                    .position(userLatLng)
                    .title("You")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
            boundsBuilder.include(userLatLng)
            hasPoints = true
        }

        groups.forEach { group ->
            val position = LatLng(group.latitude, group.longitude)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(group.name)
                    .snippet(group.tags.joinToString(", "))
            )
            if (marker != null) {
                groupMarkers[group.name] = marker
            }
            boundsBuilder.include(position)
            hasPoints = true
        }

        if (hasPoints) {
            try {
                val bounds = boundsBuilder.build()
                val padding = 100
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            } catch (illegalState: IllegalStateException) {
                // When there is only a single marker the bounds builder might fail; fallback to direct camera move.
                val fallbackLatLng = user?.let { LatLng(it.latitude, it.longitude) }
                    ?: groups.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
                fallbackLatLng?.let { latLng ->
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
                }
            }
        }
    }

    fun focusOnGroup(group: Group) {
        val map = googleMap ?: return
        val marker = groupMarkers[group.name] ?: return
        marker.showInfoWindow()
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15f))
    }
}
