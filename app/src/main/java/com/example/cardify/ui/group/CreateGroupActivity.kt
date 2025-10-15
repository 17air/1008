package com.example.cardify.ui.group

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.cardify.R
import com.example.cardify.databinding.ActivityCreateGroupBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

/**
 * Screen that allows the user to create a new group around their current location.
 */
class CreateGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateGroupBinding

    private val fusedLocationProvider by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private var hasLocationPermission: Boolean = false
    private var lastLatitude: Double = DEFAULT_LAT
    private var lastLongitude: Double = DEFAULT_LNG

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasLocationPermission = granted
            if (granted) {
                fetchCurrentLocation()
            } else {
                setFallbackLocation()
                Toast.makeText(
                    this,
                    R.string.location_permission_rationale,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegisterGroup.setOnClickListener { submitGroup() }
        ensureLocationPermission()
    }

    private fun ensureLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                hasLocationPermission = true
                fetchCurrentLocation()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(
                    this,
                    R.string.location_permission_rationale,
                    Toast.LENGTH_LONG
                ).show()
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            else -> permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun fetchCurrentLocation() {
        try {
            fusedLocationProvider.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        applyLocation(location)
                    } else {
                        requestFreshLocation()
                    }
                }
                .addOnFailureListener {
                    setFallbackLocation()
                }
        } catch (securityException: SecurityException) {
            setFallbackLocation()
        }
    }

    private fun requestFreshLocation() {
        if (!hasLocationPermission) {
            setFallbackLocation()
            return
        }
        try {
            fusedLocationProvider.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        applyLocation(location)
                    } else {
                        setFallbackLocation()
                    }
                }
                .addOnFailureListener {
                    setFallbackLocation()
                }
        } catch (securityException: SecurityException) {
            setFallbackLocation()
        }
    }

    private fun applyLocation(location: Location) {
        lastLatitude = location.latitude
        lastLongitude = location.longitude
        resolveAddress(location.latitude, location.longitude)
    }

    private fun setFallbackLocation() {
        binding.editLocation.setText(
            getString(R.string.formatted_coordinates, DEFAULT_LAT, DEFAULT_LNG)
        )
        lastLatitude = DEFAULT_LAT
        lastLongitude = DEFAULT_LNG
    }

    private fun resolveAddress(latitude: Double, longitude: Double) {
        lifecycleScope.launch {
            val addressLine = withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(this@CreateGroupActivity, Locale.getDefault())
                    val results: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
                    results?.firstOrNull()?.getAddressLine(0)
                } catch (ioException: IOException) {
                    null
                }
            }
            val fallback = getString(R.string.formatted_coordinates, latitude, longitude)
            binding.editLocation.setText(addressLine ?: fallback)
        }
    }

    private fun submitGroup() {
        val title = binding.editTitle.text?.toString().orEmpty().trim()
        val description = binding.editDescription.text?.toString().orEmpty().trim()
        val location = binding.editLocation.text?.toString().orEmpty().trim()
        val maxPeopleText = binding.editMaxPeople.text?.toString().orEmpty().trim()

        when {
            title.isBlank() -> {
                showValidationError(getString(R.string.error_group_title_required))
                return
            }

            description.isBlank() -> {
                showValidationError(getString(R.string.error_group_description_required))
                return
            }

            location.isBlank() -> {
                showValidationError(getString(R.string.error_group_location_required))
                return
            }

            maxPeopleText.isBlank() -> {
                showValidationError(getString(R.string.error_group_max_people_number))
                return
            }
        }

        val maxPeople = maxPeopleText.toIntOrNull()
        if (maxPeople == null || maxPeople <= 0) {
            showValidationError(getString(R.string.error_group_max_people_positive))
            return
        }

        Toast.makeText(this, R.string.message_group_created, Toast.LENGTH_SHORT).show()

        val data = Intent().apply {
            putExtra(GroupMapActivity.EXTRA_GROUP_TITLE, title)
            putExtra(GroupMapActivity.EXTRA_GROUP_DESCRIPTION, description)
            putExtra(GroupMapActivity.EXTRA_GROUP_LOCATION, location)
            putExtra(GroupMapActivity.EXTRA_GROUP_MAX_PEOPLE, maxPeople)
            putExtra(GroupMapActivity.EXTRA_GROUP_LATITUDE, lastLatitude)
            putExtra(GroupMapActivity.EXTRA_GROUP_LONGITUDE, lastLongitude)
        }
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun showValidationError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val DEFAULT_LAT = 37.566
        private const val DEFAULT_LNG = 126.978
    }
}
