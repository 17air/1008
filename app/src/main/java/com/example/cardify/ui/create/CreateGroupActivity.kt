package com.example.cardify.ui.create

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.cardify.R
import com.example.cardify.UserSession
import com.example.cardify.data.FirestoreProvider
import com.example.cardify.data.model.Group
import com.example.cardify.data.model.Member
import com.example.cardify.databinding.ActivityCreateGroupBinding
import com.example.cardify.ui.detail.GroupDetailActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateGroupBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var selectedLatLng: LatLng? = null

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                fetchCurrentLocation()
            } else {
                Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.useLocationButton.setOnClickListener { ensureLocationPermission() }
        binding.createGroupButton.setOnClickListener { createGroup() }
    }

    private fun ensureLocationPermission() {
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
        } else {
            fetchCurrentLocation()
        }
    }

    private fun fetchCurrentLocation() {
        binding.createProgress.isVisible = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                updateLocation(location)
            } else {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { currentLocation ->
                        if (currentLocation != null) {
                            updateLocation(currentLocation)
                        } else {
                            binding.createProgress.isVisible = false
                            Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }.addOnFailureListener {
            binding.createProgress.isVisible = false
            Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun updateLocation(location: Location) {
        selectedLatLng = LatLng(location.latitude, location.longitude)
        binding.locationText.text = getString(
            R.string.location_selected_format,
            location.latitude,
            location.longitude
        )
        binding.createProgress.isVisible = false
    }

    private fun createGroup() {
        val title = binding.titleInput.text?.toString()?.trim().orEmpty()
        val description = binding.descriptionInput.text?.toString()?.trim().orEmpty()
        val tagsInput = binding.tagsInput.text?.toString().orEmpty()
        val maxPeopleText = binding.maxPeopleInput.text?.toString()?.trim().orEmpty()

        if (title.isEmpty() || description.isEmpty() || maxPeopleText.isEmpty()) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_LONG).show()
            return
        }

        val maxPeople = maxPeopleText.toIntOrNull()
        if (maxPeople == null || maxPeople <= 0) {
            Toast.makeText(this, R.string.invalid_max_people, Toast.LENGTH_LONG).show()
            return
        }

        val location = selectedLatLng
        if (location == null) {
            ensureLocationPermission()
            Toast.makeText(this, R.string.location_not_selected, Toast.LENGTH_LONG).show()
            return
        }

        val userId = UserSession.userId
        if (userId.isBlank()) {
            UserSession.initialize { success ->
                runOnUiThread {
                    if (success) {
                        createGroup()
                    } else {
                        Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_LONG).show()
                    }
                }
            }
            return
        }

        val group = Group(
            title = title,
            description = description,
            tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            latitude = location.latitude,
            longitude = location.longitude,
            maxPeople = maxPeople,
            currentPeople = 1,
            ownerId = userId,
            ownerName = UserSession.userName
        )
        val ownerMember = Member(userId = userId, name = UserSession.userName)

        binding.createProgress.isVisible = true
        binding.createGroupButton.isEnabled = false
        FirestoreProvider.createGroup(
            group,
            ownerMember,
            onSuccess = { id ->
                runOnUiThread {
                    binding.createProgress.isVisible = false
                    binding.createGroupButton.isEnabled = true
                    Toast.makeText(this, R.string.group_created, Toast.LENGTH_SHORT).show()
                    startActivity(GroupDetailActivity.createIntent(this, id))
                    finish()
                }
            },
            onError = {
                runOnUiThread {
                    binding.createProgress.isVisible = false
                    binding.createGroupButton.isEnabled = true
                    Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}
