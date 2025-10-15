package com.example.cardify.ui.group

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cardify.R
import com.example.cardify.data.Group
import com.example.cardify.databinding.ActivityGroupMapBinding
import com.example.cardify.ui.adapter.GroupAdapter
import com.example.cardify.util.Geo
import com.example.cardify.viewmodel.GroupViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar

/**
 * Displays the combined map and group list experience for the demo.
 */
class GroupMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityGroupMapBinding
    private val viewModel: GroupViewModel by viewModels()

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val groupAdapter by lazy { GroupAdapter { focusOnGroup(it) } }

    private var googleMap: GoogleMap? = null
    private var hasLocationPermission: Boolean = false
    private var userLocation: LatLng = LatLng(DEFAULT_LAT, DEFAULT_LNG)
    private var currentGroups: List<Group> = emptyList()
    private val markerByGroup = mutableMapOf<Group, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupMapFragment()
        observeViewModel()
        setupButtons()
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
        renderGroups(currentGroups)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CREATE_GROUP && resultCode == Activity.RESULT_OK && data != null) {
            val title = data.getStringExtra(EXTRA_GROUP_TITLE) ?: return
            val description = data.getStringExtra(EXTRA_GROUP_DESCRIPTION).orEmpty()
            val location = data.getStringExtra(EXTRA_GROUP_LOCATION).orEmpty()
            val maxPeople = data.getIntExtra(EXTRA_GROUP_MAX_PEOPLE, 1)
            val latitude = data.getDoubleExtra(EXTRA_GROUP_LATITUDE, userLocation.latitude)
            val longitude = data.getDoubleExtra(EXTRA_GROUP_LONGITUDE, userLocation.longitude)

            val group = Group(
                title = title,
                description = description,
                location = location,
                maxPeople = maxPeople,
                latitude = latitude,
                longitude = longitude
            )
            viewModel.addGroup(group, userLocation)
            zoomToUser()
        }
    }

    private fun setupRecyclerView() {
        binding.groupRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@GroupMapActivity)
            adapter = groupAdapter
        }
    }

    private fun setupMapFragment() {
        val fragment = supportFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.map_container, it)
                    .commitNow()
            }
        fragment.getMapAsync(this)
    }

    private fun setupButtons() {
        val launchCreateScreen = {
            val intent = Intent(this, CreateGroupActivity::class.java)
            startActivityForResult(intent, REQUEST_CREATE_GROUP)
        }
        binding.fabCreateGroup.setOnClickListener { launchCreateScreen() }
        binding.buttonCreateGroup.setOnClickListener { launchCreateScreen() }
    }

    private fun observeViewModel() {
        viewModel.groups.observe(this) { groups ->
            val adjustedGroups = groups.map { group ->
                group.copy(
                    distanceMeters = Geo.haversineDistance(
                        userLocation.latitude,
                        userLocation.longitude,
                        group.latitude,
                        group.longitude
                    )
                )
            }
            currentGroups = adjustedGroups
            groupAdapter.submitList(adjustedGroups)
            binding.emptyStateText.isVisible = adjustedGroups.isEmpty()
            binding.groupRecyclerView.isVisible = adjustedGroups.isNotEmpty()
            renderGroups(adjustedGroups)
        }
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
        viewModel.errorMessage.observe(this) { message ->
            message ?: return@observe
            val resolved = if (message.isBlank()) getString(R.string.error_generic) else message
            Snackbar.make(binding.root, resolved, Snackbar.LENGTH_LONG).show()
            viewModel.clearError()
        }
        viewModel.toastMessage.observe(this) { message ->
            message ?: return@observe
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            viewModel.clearToast()
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
                    requestLocationPermission()
                }.show()
            }

            else -> requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            hasLocationPermission = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            if (hasLocationPermission) {
                fetchUserLocation()
            } else {
                showPermissionDeniedMessage()
                updateMyLocationLayer()
                viewModel.loadGroups(userLocation)
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
        viewModel.loadGroups(latLng)
        zoomToUser()
    }

    private fun zoomToUser() {
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM))
    }

    private fun updateMyLocationLayer() {
        val map = googleMap ?: return
        try {
            map.isMyLocationEnabled = hasLocationPermission
        } catch (_: SecurityException) {
            map.isMyLocationEnabled = false
        }
    }

    private fun renderGroups(groups: List<Group>) {
        val map = googleMap ?: return
        map.clear()
        markerByGroup.clear()

        val boundsBuilder = LatLngBounds.Builder()
        var hasBounds = false

        map.addMarker(
            MarkerOptions()
                .position(userLocation)
                .title(getString(R.string.user_location_marker_title))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )?.let {
            boundsBuilder.include(userLocation)
            hasBounds = true
        }

        groups.forEach { group ->
            val position = LatLng(group.latitude, group.longitude)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(group.title)
                    .snippet(group.location)
            )
            if (marker != null) {
                markerByGroup[group] = marker
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
            bounds?.let {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(it, MAP_PADDING))
            } ?: map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM))
        } else {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM))
        }
    }

    private fun focusOnGroup(group: Group) {
        val target = currentGroups.firstOrNull { it == group } ?: return
        val position = LatLng(target.latitude, target.longitude)
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(position, FOCUS_ZOOM))
        markerByGroup[target]?.showInfoWindow()
    }

    private fun showPermissionDeniedMessage() {
        Snackbar.make(binding.root, R.string.location_permission_rationale, Snackbar.LENGTH_LONG).show()
    }

    companion object {
        private const val DEFAULT_LAT = 37.566
        private const val DEFAULT_LNG = 126.978
        private const val DEFAULT_ZOOM = 13f
        private const val FOCUS_ZOOM = 15f
        private const val MAP_PADDING = 120
        private const val LOCATION_PERMISSION_REQUEST = 2001
        private const val REQUEST_CREATE_GROUP = 1001

        const val EXTRA_GROUP_TITLE = "extra_group_title"
        const val EXTRA_GROUP_DESCRIPTION = "extra_group_description"
        const val EXTRA_GROUP_LOCATION = "extra_group_location"
        const val EXTRA_GROUP_MAX_PEOPLE = "extra_group_max_people"
        const val EXTRA_GROUP_LATITUDE = "extra_group_latitude"
        const val EXTRA_GROUP_LONGITUDE = "extra_group_longitude"
    }
}
