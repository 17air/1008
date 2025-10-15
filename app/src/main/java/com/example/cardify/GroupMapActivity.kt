package com.example.cardify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore

class GroupMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var map: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var createButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var groupAdapter: GroupAdapter

    private val firestore: FirebaseFirestore by lazy { Firebase.firestore }
    private val groups = mutableListOf<Group>()
    private val markerMap = mutableMapOf<String, Marker>()
    private var hasCenteredOnGroups = false
    private var isFirebaseReady = false

    private val createGroupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val title = data?.getStringExtra("title") ?: return@registerForActivityResult
            val desc = data.getStringExtra("desc") ?: ""
            val maxPeopleText = data.getStringExtra("maxPeople") ?: ""
            val maxPeople = maxPeopleText.toIntOrNull() ?: 0
            val latitude = data.getDoubleExtra("latitude", Double.NaN)
            val longitude = data.getDoubleExtra("longitude", Double.NaN)

            if (!latitude.isNaN() && !longitude.isNaN()) {
                val newGroup = Group(title, desc, maxPeople, latitude, longitude)
                groups.add(0, newGroup)
                groupAdapter.submitList(groups.toList())
                renderMarkers()
                hasCenteredOnGroups = true

                focusOnGroup(newGroup)
                Toast.makeText(this, "새 소모임이 등록되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_map)

        val existingApps = FirebaseApp.getApps(this)
        if (existingApps.isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        isFirebaseReady = FirebaseApp.getApps(this).isNotEmpty()
        if (!isFirebaseReady) {
            Log.e(TAG, "Firebase is not configured. Skipping remote group fetch.")
            Toast.makeText(
                this,
                "소모임 데이터를 불러오려면 Firebase 설정이 필요합니다.",
                Toast.LENGTH_LONG
            ).show()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        createButton = findViewById(R.id.button_create_group)
        recyclerView = findViewById(R.id.recycler_groups)
        groupAdapter = GroupAdapter(::onGroupSelected)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = groupAdapter

        seedLocalGroupsIfNeeded()

        createButton.setOnClickListener {
            val intent = Intent(this, CreateGroupActivity::class.java)
            createGroupLauncher.launch(intent)
        }

        if (isFirebaseReady) {
            fetchGroupsFromFirestore()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.uiSettings?.isZoomControlsEnabled = true
        map?.uiSettings?.isMyLocationButtonEnabled = true
        map?.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            false
        }
        enableMyLocation()
        renderMarkers()
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
            return
        }
        map?.isMyLocationEnabled = true
        updateCurrentLocation { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            } else {
                moveToSeoul()
            }
        }
    }

    private fun updateCurrentLocation(onLocation: (Location?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onLocation(null)
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                onLocation(location)
            }
            .addOnFailureListener {
                onLocation(null)
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
        } else {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            moveToSeoul()
        }
    }

    private fun renderMarkers() {
        val googleMap = map ?: return
        googleMap.clear()
        markerMap.clear()

        groups.forEach { group ->
            val lat = group.latitude
            val lng = group.longitude
            if (lat != null && lng != null) {
                val marker = googleMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(lat, lng))
                        .title(group.title)
                        .snippet(group.description)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                )
                if (marker != null) {
                    markerMap[group.key()] = marker
                }
            }
        }

        if (!hasCenteredOnGroups && groups.isNotEmpty()) {
            val first = groups.firstOrNull { it.latitude != null && it.longitude != null }
            if (first != null) {
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(first.latitude!!, first.longitude!!),
                        13f
                    )
                )
                hasCenteredOnGroups = true
            }
        }
    }

    private fun onGroupSelected(group: Group) {
        focusOnGroup(group)
    }

    private fun focusOnGroup(group: Group) {
        val lat = group.latitude
        val lng = group.longitude
        if (lat == null || lng == null) {
            Toast.makeText(this, "이 소모임의 위치 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val latLng = LatLng(lat, lng)
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        markerMap[group.key()]?.showInfoWindow()
    }

    private fun seedLocalGroupsIfNeeded() {
        if (groups.isNotEmpty()) {
            groupAdapter.submitList(groups.toList())
            renderMarkers()
            return
        }

        val defaultGroups = listOf(
            Group(
                title = "광화문 문화 산책",
                description = "주말 오전에 경복궁과 북촌을 함께 둘러봐요.",
                maxPeople = 8,
                latitude = 37.5752,
                longitude = 126.9769
            ),
            Group(
                title = "한강 러닝 크루",
                description = "매주 수요일 저녁 여의도 한강공원 5km 러닝.",
                maxPeople = 12,
                latitude = 37.5271,
                longitude = 126.9326
            ),
            Group(
                title = "홍대 보드게임 모임",
                description = "보드게임 카페에서 신작 게임을 함께 즐겨요.",
                maxPeople = 6,
                latitude = 37.5551,
                longitude = 126.9226
            )
        )

        groups.addAll(defaultGroups)
        groupAdapter.submitList(groups.toList())
        renderMarkers()
    }

    private fun moveToSeoul() {
        val seoul = LatLng(37.566, 126.978)
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 13f))
    }

    private fun fetchGroupsFromFirestore() {
        firestore.collection("groups")
            .get()
            .addOnSuccessListener { snapshot ->
                val fetchedGroups = snapshot.documents.mapNotNull { it.toGroup() }
                groups.clear()
                groups.addAll(fetchedGroups)
                groupAdapter.submitList(groups.toList())
                hasCenteredOnGroups = false
                renderMarkers()
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to load groups", error)
                Toast.makeText(this, "소모임 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                if (groups.isEmpty()) {
                    moveToSeoul()
                }
            }
    }

    private fun Group.key(): String {
        val latPart = latitude?.toString() ?: "null"
        val lngPart = longitude?.toString() ?: "null"
        return "$title|$latPart|$lngPart"
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1001
        private const val TAG = "GroupMapActivity"
    }

    private fun DocumentSnapshot.toGroup(): Group? {
        val title = getString("title") ?: return null
        val description = getString("description") ?: getString("desc") ?: ""
        val maxPeople = getLong("maxPeople")?.toInt() ?: 0

        val geoPoint = getGeoPoint("location")
        val latitude = when {
            contains("latitude") -> getDouble("latitude")
            geoPoint != null -> geoPoint.latitude
            contains("lat") -> getDouble("lat")
            else -> null
        }
        val longitude = when {
            contains("longitude") -> getDouble("longitude")
            geoPoint != null -> geoPoint.longitude
            contains("lng") -> getDouble("lng") ?: getDouble("lon")
            contains("lon") -> getDouble("lon")
            else -> null
        }

        return Group(title, description, maxPeople, latitude, longitude)
    }
}
