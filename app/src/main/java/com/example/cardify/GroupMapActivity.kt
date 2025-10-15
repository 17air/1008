package com.example.cardify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
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
import com.google.android.gms.maps.model.MarkerOptions

class GroupMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var createButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var groupAdapter: GroupAdapter

    private var currentLocation: Location? = null

    private val groups = mutableListOf(
        Group("한강 러닝 모임", "매주 토요일 한강에서 러닝해요", 10),
        Group("보드게임 모임", "종로에서 보드게임 즐겨요", 8),
        Group("스터디 모임", "IT 취준생 스터디", 6)
    )

    private val createGroupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val title = data?.getStringExtra("title") ?: return@registerForActivityResult
            val desc = data.getStringExtra("desc") ?: ""
            val maxPeopleText = data.getStringExtra("maxPeople") ?: ""
            val maxPeople = maxPeopleText.toIntOrNull() ?: 0

            updateCurrentLocation { location ->
                if (location != null) {
                    val newGroup = Group(title, desc, maxPeople)
                    groups.add(0, newGroup)
                    groupAdapter.submitList(groups.toList())

                    val latLng = LatLng(location.latitude, location.longitude)
                    map.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(title)
                            .snippet(desc)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                    )
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    Toast.makeText(this, "새 소모임이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_map)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        createButton = findViewById(R.id.button_create_group)
        recyclerView = findViewById(R.id.recycler_groups)
        groupAdapter = GroupAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = groupAdapter
        groupAdapter.submitList(groups.toList())

        createButton.setOnClickListener {
            val intent = Intent(this, CreateGroupActivity::class.java)
            createGroupLauncher.launch(intent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        enableMyLocation()
        addDummyMarkers()
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
        map.isMyLocationEnabled = true
        updateCurrentLocation { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
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
                currentLocation = location
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

    private fun addDummyMarkers() {
        val seoul = LatLng(37.566, 126.978)
        val offsets = listOf(
            Pair(0.002, 0.003),
            Pair(-0.002, -0.001),
            Pair(0.001, -0.003)
        )

        groups.forEachIndexed { index, group ->
            val latLng = LatLng(
                seoul.latitude + offsets[index % offsets.size].first,
                seoul.longitude + offsets[index % offsets.size].second
            )
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(group.title)
                    .snippet(group.description)
            )
        }
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 13f))
    }

    private fun moveToSeoul() {
        val seoul = LatLng(37.566, 126.978)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 13f))
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1001
    }
}
