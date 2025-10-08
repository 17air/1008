package com.example.cardify.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.cardify.R
import com.example.cardify.data.Group
import com.example.cardify.data.User
import com.example.cardify.databinding.FragmentKakaoMapBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView

/**
 * Fragment that displays a Kakao Map with the user's current position and mock group markers.
 */
class KakaoMapFragment : Fragment(), MapView.MapViewEventListener, MapView.POIItemEventListener {

    private var _binding: FragmentKakaoMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private var userMarker: MapPOIItem? = null

    private var user: User = User(
        tags = listOf("등산", "산책"),
        latitude = DEFAULT_LAT,
        longitude = DEFAULT_LNG
    )

    private val groups = listOf(
        Group("Hiking Club", listOf("등산", "야외활동"), 37.55, 126.97),
        Group("Cafe Study", listOf("스터디", "커피"), 37.56, 127.00)
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchUserLocation()
        } else {
            context?.let {
                Toast.makeText(
                    it,
                    getString(R.string.location_permission_rationale),
                    Toast.LENGTH_SHORT
                ).show()
            }
            applyUserLocation(DEFAULT_LAT, DEFAULT_LNG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKakaoMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMapView()
        if (hasLocationPermission()) {
            fetchUserLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapViewContainer.removeAllViews()
        _binding = null
    }

    private fun setupMapView() {
        mapView = MapView(requireContext())
        binding.mapViewContainer.addView(mapView)
        mapView.setMapViewEventListener(this)
        mapView.setPOIItemEventListener(this)
        mapView.setZoomLevel(3, false)
        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(DEFAULT_LAT, DEFAULT_LNG), false)
        refreshMarkers()
    }

    private fun hasLocationPermission(): Boolean {
        val context = context ?: return false
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun fetchUserLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    applyUserLocation(location.latitude, location.longitude)
                } else {
                    requestCurrentLocation()
                }
            }
            .addOnFailureListener {
                applyUserLocation(DEFAULT_LAT, DEFAULT_LNG)
            }
    }

    private fun requestCurrentLocation() {
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    applyUserLocation(location.latitude, location.longitude)
                } else {
                    applyUserLocation(DEFAULT_LAT, DEFAULT_LNG)
                }
            }
            .addOnFailureListener {
                applyUserLocation(DEFAULT_LAT, DEFAULT_LNG)
            }
    }

    private fun applyUserLocation(latitude: Double, longitude: Double) {
        user = user.copy(latitude = latitude, longitude = longitude)
        if (_binding == null || !::mapView.isInitialized) {
            return
        }
        refreshMarkers()
        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude), true)
    }

    private fun refreshMarkers() {
        if (!::mapView.isInitialized) return

        val newMarkers = mutableListOf<MapPOIItem>()

        val userPoint = MapPoint.mapPointWithGeoCoord(user.latitude, user.longitude)
        userMarker = MapPOIItem().apply {
            itemName = "현재 위치"
            mapPoint = userPoint
            markerType = MapPOIItem.MarkerType.RedPin
            tag = USER_MARKER_TAG
        }
        newMarkers.add(userMarker!!)

        val userTagSet = user.tags.toSet()
        groups.forEachIndexed { index, group ->
            val mapPoint = MapPoint.mapPointWithGeoCoord(group.latitude, group.longitude)
            val sharedTags = group.tags.filter { it in userTagSet }
            val marker = MapPOIItem().apply {
                itemName = group.name
                this.mapPoint = mapPoint
                markerType = if (sharedTags.isNotEmpty()) {
                    MapPOIItem.MarkerType.YellowPin
                } else {
                    MapPOIItem.MarkerType.BluePin
                }
                selectedMarkerType = MapPOIItem.MarkerType.RedPin
                tag = index
                userObject = sharedTags
            }
            newMarkers.add(marker)
        }

        mapView.removeAllPOIItems()
        mapView.addPOIItems(newMarkers.toTypedArray())
    }

    override fun onMapViewInitialized(mapView: MapView?) {
        // Map is ready. Markers are added in refreshMarkers().
    }

    override fun onMapViewCenterPointMoved(mapView: MapView?, mapPoint: MapPoint?) = Unit

    override fun onMapViewZoomLevelChanged(mapView: MapView?, zoomLevel: Int) = Unit

    override fun onMapViewSingleTapped(mapView: MapView?, mapPoint: MapPoint?) = Unit

    override fun onMapViewDoubleTapped(mapView: MapView?, mapPoint: MapPoint?) = Unit

    override fun onMapViewLongPressed(mapView: MapView?, mapPoint: MapPoint?) = Unit

    override fun onMapViewDragStarted(mapView: MapView?, mapPoint: MapPoint?) = Unit

    override fun onMapViewDragEnded(mapView: MapView?, mapPoint: MapPoint?) = Unit

    override fun onMapViewMoveFinished(mapView: MapView?, mapPoint: MapPoint?) = Unit

    override fun onPOIItemSelected(mapView: MapView?, poiItem: MapPOIItem?) {
        val tag = poiItem?.tag ?: return
        if (tag == USER_MARKER_TAG) {
            return
        }
        val group = groups.getOrNull(tag) ?: return
        val sharedTags = (poiItem.userObject as? List<*>)
            ?.filterIsInstance<String>()
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
            ?: getString(R.string.group_marker_no_tags)
        val message = getString(R.string.group_marker_message, group.name, sharedTags)
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onCalloutBalloonOfPOIItemTouched(mapView: MapView?, poiItem: MapPOIItem?) = Unit

    override fun onCalloutBalloonOfPOIItemTouched(
        mapView: MapView?,
        poiItem: MapPOIItem?,
        buttonType: MapPOIItem.CalloutBalloonButtonType?
    ) = Unit

    override fun onDraggablePOIItemMoved(mapView: MapView?, poiItem: MapPOIItem?, mapPoint: MapPoint?) = Unit

    companion object {
        private const val USER_MARKER_TAG = -1
        private const val DEFAULT_LAT = 37.5665
        private const val DEFAULT_LNG = 126.9780
    }
}
