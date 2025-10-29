package com.example.cardify.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import com.example.cardify.R
import com.example.cardify.UserSession
import com.example.cardify.data.LocalGroupRepository
import com.example.cardify.data.LocalGroupRepository.ListenerRegistration
import com.example.cardify.data.model.Group
import com.example.cardify.databinding.ActivityMainBinding
import com.example.cardify.ui.actions.GroupActionsBottomSheet
import com.example.cardify.ui.create.CreateGroupActivity
import com.example.cardify.ui.detail.GroupDetailActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity :
    AppCompatActivity(),
    OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener,
    GroupActionsBottomSheet.ActionHandler {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val groupAdapter = GroupAdapter { group ->
        focusGroupOnMap(group)
        startActivity(GroupDetailActivity.createIntent(this, group.id))
    }

    private val markerMap = mutableMapOf<String, Marker>()
    private var googleMap: GoogleMap? = null
    private var lastKnownLocation: Location? = null
    private var groupListener: ListenerRegistration? = null
    private var isCameraCentered = false
    private val defaultLatLng = LatLng(37.566, 126.978)

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                enableMyLocation()
            } else {
                Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.groupRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.groupRecyclerView.adapter = groupAdapter
        setupFloatingActionButton(binding.createGroupFab)

        binding.loadingIndicator.isVisible = true

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also { fragment ->
                supportFragmentManager.beginTransaction()
                    .replace(R.id.map_container, fragment)
                    .commit()
            }
        mapFragment.getMapAsync(this)

        UserSession.initialize { success ->
            runOnUiThread {
                if (success) {
                    subscribeToGroups()
                } else {
                    binding.loadingIndicator.isVisible = false
                    Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        groupListener?.remove()
        groupListener = null
    }

    override fun onRequestCreateGroup() {
        startActivity(Intent(this, CreateGroupActivity::class.java))
    }

    override fun onRequestOpenGroupDetail(groupId: String) {
        groupAdapter.currentList.firstOrNull { it.id == groupId }?.let { group ->
            focusGroupOnMap(group)
        }
        startActivity(GroupDetailActivity.createIntent(this, groupId))
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isMapToolbarEnabled = false
            setOnMarkerClickListener(this@MainActivity)
        }
        enableMyLocation()
        if (!isCameraCentered) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 12f))
        }
        renderMarkers(emptyList())
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        marker.showInfoWindow()
        marker.tag?.let { tag ->
            val groupId = tag as? String ?: return false
            startActivity(GroupDetailActivity.createIntent(this, groupId))
        }
        return true
    }

    private fun subscribeToGroups() {
        groupListener?.remove()
        groupListener = LocalGroupRepository.observeGroups(
            onSuccess = { groups ->
                runOnUiThread {
                    binding.loadingIndicator.isVisible = false
                    binding.emptyStateText.isVisible = groups.isEmpty()
                    groupAdapter.submitList(groups)
                    renderMarkers(groups)
                }
            },
            onError = {
                runOnUiThread {
                    binding.loadingIndicator.isVisible = false
                    Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun showGroupActionsSheet() {
        val existing = supportFragmentManager.findFragmentByTag(GroupActionsBottomSheet.TAG)
        if (existing != null && existing.isAdded) {
            return
        }
        GroupActionsBottomSheet().show(supportFragmentManager, GroupActionsBottomSheet.TAG)
    }

    private fun setupFloatingActionButton(composeView: ComposeView) {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent {
            MaterialTheme {
                FloatingActionButtonContent { showGroupActionsSheet() }
            }
        }
    }

    @Composable
    private fun FloatingActionButtonContent(onClick: () -> Unit) {
        val label = stringResource(id = R.string.create_group)
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = label)
        }
    }

    private fun renderMarkers(groups: List<Group>) {
        val map = googleMap ?: return
        markerMap.values.forEach { it.remove() }
        markerMap.clear()
        groups.forEach { group ->
            val position = LatLng(group.latitude, group.longitude)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(group.title)
                    .snippet(group.description)
            )
            marker?.tag = group.id
            if (marker != null) {
                markerMap[group.id] = marker
            }
        }
        if (!isCameraCentered) {
            if (groups.isNotEmpty()) {
                focusGroupOnMap(groups.first())
            } else {
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 12f))
            }
        }
    }

    private fun enableMyLocation() {
        val map = googleMap ?: return
        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }
        map.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onLocationAvailable(location)
            } else {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { currentLocation ->
                        currentLocation?.let { onLocationAvailable(it) }
                    }
            }
        }
    }

    private fun onLocationAvailable(location: Location) {
        lastKnownLocation = location
        groupAdapter.updateUserLocation(location)
        centerCamera(location)
    }

    private fun centerCamera(location: Location) {
        if (googleMap == null) return
        if (!isCameraCentered) {
            val latLng = LatLng(location.latitude, location.longitude)
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
            isCameraCentered = true
        }
    }

    private fun focusGroupOnMap(group: Group) {
        val map = googleMap ?: return
        val latLng = LatLng(group.latitude, group.longitude)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        isCameraCentered = true
        markerMap[group.id]?.showInfoWindow()
    }

    override fun onResume() {
        super.onResume()
        enableMyLocation()
        lastKnownLocation?.let { groupAdapter.updateUserLocation(it) }
    }
}
