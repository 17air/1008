package com.example.cardify.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cardify.R
import com.example.cardify.data.Group
import com.example.cardify.data.User
import com.example.cardify.databinding.FragmentMapListBinding
import com.example.cardify.ui.adapter.GroupAdapter
import com.example.cardify.util.Geo
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

/**
 * Displays a Google Map and a RecyclerView showing recommended groups.
 */
class MapListFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapListBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private var mapBinder: MapBinder? = null

    private val groupAdapter = GroupAdapter { group -> onGroupSelected(group) }

    private var user: User = User(
        tags = emptyList(),
        latitude = DEFAULT_LAT,
        longitude = DEFAULT_LNG
    )

    private val baseGroups = listOf(
        Group(
            title = "Hiking Club",
            description = "북악산을 함께 오르는 주말 산행 모임입니다.",
            location = "서울 종로구 북악산 일대",
            maxPeople = 12,
            latitude = 37.5796,
            longitude = 126.9770
        ),
        Group(
            title = "Movie Fans",
            description = "최신 영화를 보고 토론하는 소규모 모임입니다.",
            location = "서울 중구 CGV 명동",
            maxPeople = 10,
            latitude = 37.5600,
            longitude = 126.9830
        ),
        Group(
            title = "Runner Group",
            description = "한강 러닝을 함께 즐기는 저녁 모임입니다.",
            location = "서울 용산구 이촌한강공원",
            maxPeople = 20,
            latitude = 37.5510,
            longitude = 126.9880
        ),
        Group(
            title = "Cafe Study",
            description = "도심 카페에서 스터디를 진행하는 모임입니다.",
            location = "서울 종로구 카페거리",
            maxPeople = 8,
            latitude = 37.5700,
            longitude = 126.9920
        ),
        Group(
            title = "Boardgame Night",
            description = "보드게임으로 친목을 다지는 저녁 모임입니다.",
            location = "서울 중구 보드게임 카페",
            maxPeople = 14,
            latitude = 37.5650,
            longitude = 126.9768
        )
    )

    private var currentGroups: List<Group> = emptyList()
    private var hasLocationPermission: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupMap()
        refreshGroups()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        googleMap = null
        mapBinder = null
        _binding = null
    }

    fun onUserLocationUpdated(latitude: Double, longitude: Double) {
        user = user.copy(latitude = latitude, longitude = longitude)
        refreshGroups()
    }

    fun onLocationPermissionResult(granted: Boolean) {
        hasLocationPermission = granted
        applyMyLocationLayer()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map.apply {
            uiSettings.isCompassEnabled = true
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isMapToolbarEnabled = false
        }
        mapBinder = MapBinder(requireContext(), map).also {
            it.renderMarkers(user, currentGroups)
        }
        map.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            false
        }
        applyMyLocationLayer()
    }

    private fun setupRecyclerView() {
        binding.groupRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = groupAdapter
        }
    }

    private fun setupMap() {
        val existingFragment = childFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment
        val mapFragment = existingFragment ?: SupportMapFragment.newInstance().also { fragment ->
            childFragmentManager.commit {
                replace(R.id.map_container, fragment)
            }
        }
        mapFragment.getMapAsync(this)
    }

    private fun refreshGroups() {
        currentGroups = baseGroups
            .map { group ->
                val distance = Geo.haversineDistance(
                    user.latitude,
                    user.longitude,
                    group.latitude,
                    group.longitude
                )
                group.copy(distanceMeters = distance)
            }
            .sortedBy { it.distanceMeters }

        groupAdapter.submitList(currentGroups)
        mapBinder?.renderMarkers(user, currentGroups)
    }

    private fun applyMyLocationLayer() {
        val map = googleMap ?: return
        val context = context ?: return
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        try {
            map.isMyLocationEnabled = hasLocationPermission && (fineGranted || coarseGranted)
        } catch (securityException: SecurityException) {
            map.isMyLocationEnabled = false
        }
    }

    private fun onGroupSelected(group: Group) {
        mapBinder?.focusOnGroup(group)
    }

    companion object {
        const val TAG: String = "MapListFragment"
        private const val DEFAULT_LAT = 37.5665
        private const val DEFAULT_LNG = 126.9780
    }
}
