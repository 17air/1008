package com.example.cardify.ui.group

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.cardify.R
import com.example.cardify.data.Group
import com.example.cardify.databinding.ActivityGroupMapBinding
import com.example.cardify.viewmodel.GroupMapViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar

/**
 * Displays nearby groups on a Google Map, allowing users to open the create flow.
 */
class GroupMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityGroupMapBinding
    private val viewModel: GroupMapViewModel by viewModels()

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private var googleMap: GoogleMap? = null
    private var userLocation: LatLng = LatLng(DEFAULT_LAT, DEFAULT_LNG)
    private var hasLocationPermission: Boolean = false

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasLocationPermission = granted
            if (granted) {
                fetchUserLocation()
            } else {
                showPermissionDeniedMessage()
                updateMyLocationLayer()
                viewModel.loadNearbyGroups(userLocation.latitude, userLocation.longitude)
            }
        }

    private val createGroupLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.loadNearbyGroups(userLocation.latitude, userLocation.longitude)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupMapFragment()
        observeViewModel()
        binding.fabCreateGroup.setOnClickListener {
            val intent = Intent(this, CreateGroupActivity::class.java)
            createGroupLauncher.launch(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        ensureLocationPermission()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map.apply {
            uiSettings.isCompassEnabled = true
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isMapToolbarEnabled = false
        }
        updateMyLocationLayer()
        renderGroups(viewModel.groups.value.orEmpty())
    }

    private fun setupMapFragment() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.map_container, it)
                    .commitNow()
            }
        mapFragment.getMapAsync(this)
    }

    private fun observeViewModel() {
        viewModel.groups.observe(this) { groups ->
            renderGroups(groups)
        }
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
        viewModel.errorMessage.observe(this) { message ->
            message ?: return@observe
            val displayMessage = if (message.isBlank()) {
                getString(R.string.error_generic)
            } else {
                message
            }
            Snackbar.make(binding.root, displayMessage, Snackbar.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    private fun ensureLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                hasLocationPermission = true
                fetchUserLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Snackbar.make(
                    binding.root,
                    R.string.location_permission_rationale,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(R.string.action_grant) {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }.show()
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun fetchUserLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        updateUserLocation(LatLng(location.latitude, location.longitude))
                    } else {
                        requestCurrentLocation()
                    }
                }
                .addOnFailureListener {
                    updateUserLocation(userLocation)
                }
        } catch (securityException: SecurityException) {
            showPermissionDeniedMessage()
        }
    }

    private fun requestCurrentLocation() {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        updateUserLocation(LatLng(location.latitude, location.longitude))
                    } else {
                        updateUserLocation(userLocation)
                    }
                }
                .addOnFailureListener {
                    updateUserLocation(userLocation)
                }
        } catch (securityException: SecurityException) {
            showPermissionDeniedMessage()
        }
    }

    private fun updateUserLocation(latLng: LatLng) {
        userLocation = latLng
        updateMyLocationLayer()
        viewModel.loadNearbyGroups(latLng.latitude, latLng.longitude)
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))
    }

    private fun updateMyLocationLayer() {
        val map = googleMap ?: return
        try {
            map.isMyLocationEnabled = hasLocationPermission
        } catch (securityException: SecurityException) {
            map.isMyLocationEnabled = false
        }
    }

    private fun renderGroups(groups: List<Group>) {
        val map = googleMap ?: return
        map.clear()

        val boundsBuilder = LatLngBounds.Builder()
        var hasBounds = false

        groups.forEach { group ->
            val position = LatLng(group.latitude, group.longitude)
            val snippet = buildString {
                if (!group.meetingTime.isNullOrBlank()) {
                    append(getString(R.string.group_marker_time, group.meetingTime))
                }
                if (group.currentMembers != null && group.maxMembers != null) {
                    if (isNotEmpty()) append('\n')
                    append(
                        getString(
                            R.string.group_marker_members,
                            group.currentMembers,
                            group.maxMembers
                        )
                    )
                }
            }
            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(group.name)
                    .snippet(snippet.ifBlank { null })
            )
            boundsBuilder.include(position)
            hasBounds = true
        }

        if (hasBounds) {
            boundsBuilder.include(userLocation)
            val bounds = try {
                boundsBuilder.build()
            } catch (exception: IllegalStateException) {
                null
            }
            bounds?.let {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(it, MAP_PADDING))
            }
        }
    }

    private fun showPermissionDeniedMessage() {
        Snackbar.make(binding.root, R.string.location_permission_rationale, Snackbar.LENGTH_LONG).show()
    }

    companion object {
        private const val DEFAULT_LAT = 37.5665
        private const val DEFAULT_LNG = 126.9780
        private const val DEFAULT_ZOOM = 13f
        private const val MAP_PADDING = 120
    }
}
