package com.example.cardify.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.cardify.data.User
import com.example.cardify.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng

/**
 * The single activity that hosts the [MapListFragment] and handles runtime permissions.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val defaultUserTags = listOf("등산", "산책")
    private val defaultUserLocation = LatLng(37.5665, 126.9780)

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                fetchUserLocation()
            } else {
                Toast.makeText(
                    this,
                    getString(com.example.cardify.R.string.location_permission_rationale),
                    Toast.LENGTH_SHORT
                ).show()
                deliverUserLocation(defaultUserLocation)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (hasLocationPermission()) {
            fetchUserLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun fetchUserLocation() {
        if (!hasLocationPermission()) {
            deliverUserLocation(defaultUserLocation)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    deliverUserLocation(LatLng(location.latitude, location.longitude))
                } else {
                    requestCurrentLocation()
                }
            }
            .addOnFailureListener {
                deliverUserLocation(defaultUserLocation)
            }
    }

    private fun requestCurrentLocation() {
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    deliverUserLocation(LatLng(location.latitude, location.longitude))
                } else {
                    deliverUserLocation(defaultUserLocation)
                }
            }
            .addOnFailureListener {
                deliverUserLocation(defaultUserLocation)
            }
    }

    private fun deliverUserLocation(latLng: LatLng) {
        val fragment = supportFragmentManager
            .findFragmentById(com.example.cardify.R.id.fragment_container_view) as? MapListFragment
        val user = User(defaultUserTags, latLng.latitude, latLng.longitude)
        fragment?.onUserLocationAvailable(user)
    }
}
