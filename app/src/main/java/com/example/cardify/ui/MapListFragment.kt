package com.example.cardify.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cardify.data.Group
import com.example.cardify.data.User
import com.example.cardify.databinding.FragmentMapListBinding
import com.example.cardify.util.Geo
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

/**
 * Fragment that hosts both the Google Map and the RecyclerView list.
 */
class MapListFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapListBinding? = null
    private val binding get() = _binding!!

    private val adapter = GroupAdapter { group -> onGroupSelected(group) }
    private val mapBinder = MapBinder()

    private var googleMap: GoogleMap? = null
    private var user: User? = null

    private val baseGroups = listOf(
        Group("Hiking Club", listOf("등산", "야외활동"), 37.5796, 126.9770),
        Group("Movie Fans", listOf("영화", "토론"), 37.5600, 126.9830),
        Group("Runner Group", listOf("러닝", "산책"), 37.5510, 126.9880),
        Group("Cafe Study", listOf("스터디", "카페"), 37.5700, 126.9920),
        Group("Boardgame Night", listOf("보드게임", "취미"), 37.5650, 126.9768)
    )

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        googleMap = null
        _binding = null
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        mapBinder.attachMap(map)
        enableMyLocationLayerIfGranted(map)
        refreshGroups()
    }

    /**
     * Called by [MainActivity] once the user's location is available or when falling back to the default.
     */
    fun onUserLocationAvailable(user: User) {
        this.user = user
        enableMyLocationLayerIfGranted()
        refreshGroups()
    }

    private fun setupRecyclerView() {
        binding.groupList.layoutManager = LinearLayoutManager(requireContext())
        binding.groupList.adapter = adapter
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(binding.mapContainer.id) as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also { fragment ->
                childFragmentManager
                    .beginTransaction()
                    .replace(binding.mapContainer.id, fragment)
                    .commit()
            }
        mapFragment.getMapAsync(this)
    }

    private fun refreshGroups() {
        val currentUser = user ?: return
        val enrichedGroups = baseGroups.map { group ->
            val sharedTags = group.tags.intersect(currentUser.tags.toSet())
            val distance = Geo.haversineDistanceMeters(
                currentUser.latitude,
                currentUser.longitude,
                group.latitude,
                group.longitude
            )
            group.copy(
                distanceMeters = distance,
                sharedTagsCount = sharedTags.size
            )
        }.sortedWith(
            compareByDescending<Group> { it.sharedTagsCount }
                .thenBy { it.distanceMeters }
        )

        adapter.submitList(enrichedGroups)
        mapBinder.render(enrichedGroups, currentUser)
    }

    private fun onGroupSelected(group: Group) {
        mapBinder.focusOnGroup(group)
    }

    private fun enableMyLocationLayerIfGranted(map: GoogleMap? = googleMap) {
        val context = context ?: return
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            map?.isMyLocationEnabled = true
        }
    }
}
