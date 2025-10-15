package com.example.cardify.ui.group

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.cardify.R
import com.example.cardify.data.CreateGroupRequest
import com.example.cardify.databinding.ActivityCreateGroupBinding
import com.example.cardify.viewmodel.CreateGroupViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

/**
 * Allows users to create a new group via the backend API.
 */
class CreateGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateGroupBinding
    private val viewModel: CreateGroupViewModel by viewModels()

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private var hasLocationPermission: Boolean = false
    private var lastKnownLatitude: Double? = null
    private var lastKnownLongitude: Double? = null

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasLocationPermission = granted
            if (granted) {
                fetchUserLocation()
            } else {
                setFallbackLocationIfBlank()
                Snackbar.make(binding.root, R.string.location_permission_rationale, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_grant) {
                        requestLocationPermission()
                    }
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.buttonSubmit.setOnClickListener { submitForm() }
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        ensureLocationPermission()
    }

    private fun observeViewModel() {
        viewModel.isSubmitting.observe(this) { isSubmitting ->
            binding.progressBar.isVisible = isSubmitting
            binding.buttonSubmit.isEnabled = !isSubmitting
        }
        viewModel.creationSuccess.observe(this) {
            Toast.makeText(this, R.string.message_group_created, Toast.LENGTH_LONG).show()
            setResult(Activity.RESULT_OK)
            finish()
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
                setFallbackLocationIfBlank()
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
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun fetchUserLocation() {
        if (!hasLocationPermission) {
            return
        }
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        applyLocation(location)
                    } else {
                        requestCurrentLocation()
                    }
                }
                .addOnFailureListener {
                    requestCurrentLocation()
                }
        } catch (securityException: SecurityException) {
            Snackbar.make(binding.root, R.string.location_permission_rationale, Snackbar.LENGTH_LONG).show()
            setFallbackLocationIfBlank()
        }
    }

    private fun requestCurrentLocation() {
        if (!hasLocationPermission) return
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        applyLocation(location)
                    } else {
                        setFallbackLocationIfBlank()
                    }
                }
                .addOnFailureListener {
                    setFallbackLocationIfBlank()
                }
        } catch (securityException: SecurityException) {
            Snackbar.make(binding.root, R.string.location_permission_rationale, Snackbar.LENGTH_LONG).show()
            setFallbackLocationIfBlank()
        }
    }

    private fun applyLocation(location: Location) {
        lastKnownLatitude = location.latitude
        lastKnownLongitude = location.longitude
        if (binding.inputLocation.text.isNullOrBlank()) {
            resolveAddress(location.latitude, location.longitude)
        }
    }

    private fun resolveAddress(latitude: Double, longitude: Double) {
        lifecycleScope.launch {
            val resolvedAddress = withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(this@CreateGroupActivity, Locale.getDefault())
                    val results: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
                    results?.firstOrNull()?.getAddressLine(0)
                } catch (ioException: IOException) {
                    null
                }
            }

            val fallback = getString(R.string.formatted_coordinates, latitude, longitude)
            binding.inputLocation.setText(resolvedAddress ?: fallback)
        }
    }

    private fun setFallbackLocationIfBlank() {
        if (binding.inputLocation.text.isNullOrBlank()) {
            binding.inputLocation.setText(
                getString(R.string.formatted_coordinates, DEFAULT_LAT, DEFAULT_LNG)
            )
        }
        if (lastKnownLatitude == null || lastKnownLongitude == null) {
            lastKnownLatitude = DEFAULT_LAT
            lastKnownLongitude = DEFAULT_LNG
        }
    }

    private fun submitForm() {
        val title = binding.inputTitle.text?.toString().orEmpty()
        val description = binding.inputDescription.text?.toString().orEmpty()
        val location = binding.inputLocation.text?.toString().orEmpty()
        val maxPeopleText = binding.inputMaxPeople.text?.toString().orEmpty()

        val errorResId = viewModel.validateInput(title, description, location, maxPeopleText)
        if (errorResId != null) {
            Snackbar.make(binding.root, getString(errorResId), Snackbar.LENGTH_LONG).show()
            return
        }

        val request = CreateGroupRequest(
            title = title,
            description = description,
            location = location,
            maxPeople = maxPeopleText.toInt(),
            latitude = lastKnownLatitude,
            longitude = lastKnownLongitude
        )
        viewModel.submitGroup(request)
    }

    companion object {
        private const val DEFAULT_LAT = 37.566
        private const val DEFAULT_LNG = 126.978
    }
}
