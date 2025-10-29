package com.example.cardify.ui.location

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

const val DEFAULT_LATITUDE = 37.5665
const val DEFAULT_LONGITUDE = 126.9780

val DefaultSeoulLatLng: LatLng = LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)

suspend fun fetchCurrentLocation(client: FusedLocationProviderClient): LatLng? {
    val lastKnown = runCatching { client.lastLocation.await() }.getOrNull()
    if (lastKnown != null) {
        return LatLng(lastKnown.latitude, lastKnown.longitude)
    }

    val tokenSource = CancellationTokenSource()
    val current = runCatching {
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token).await()
    }.getOrNull()
    return current?.let { LatLng(it.latitude, it.longitude) }
}
