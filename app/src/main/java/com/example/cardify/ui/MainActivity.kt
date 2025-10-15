package com.example.cardify.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.cardify.R
import com.example.cardify.databinding.ActivityMainBinding
import com.example.cardify.ui.group.GroupMapActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Hosts the [MapListFragment], handles runtime permissions, and provides location updates.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            notifyFragmentPermission(granted)
            if (granted) {
                fetchUserLocation()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.location_permission_rationale),
                    Toast.LENGTH_LONG
                ).show()
                deliverLocation(DEFAULT_LAT, DEFAULT_LNG)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fabOpenGroupMap.setOnClickListener {
            startActivity(Intent(this, GroupMapActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        checkLocationPermissions()
    }

    private fun checkLocationPermissions() {
        if (hasLocationPermission()) {
            notifyFragmentPermission(true)
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
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    deliverLocation(location.latitude, location.longitude)
                } else {
                    requestCurrentLocation()
                }
            }
            .addOnFailureListener {
                deliverLocation(DEFAULT_LAT, DEFAULT_LNG)
            }
    }

    private fun requestCurrentLocation() {
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    deliverLocation(location.latitude, location.longitude)
                } else {
                    deliverLocation(DEFAULT_LAT, DEFAULT_LNG)
                }
            }
            .addOnFailureListener {
                deliverLocation(DEFAULT_LAT, DEFAULT_LNG)
            }
    }

    private fun deliverLocation(latitude: Double, longitude: Double) {
        (supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? MapListFragment)
            ?.onUserLocationUpdated(latitude, longitude)
    }

    private fun notifyFragmentPermission(granted: Boolean) {
        (supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? MapListFragment)
            ?.onLocationPermissionResult(granted)
    }

    companion object {
        private const val DEFAULT_LAT = 37.5665
        private const val DEFAULT_LNG = 126.9780
    }
}
