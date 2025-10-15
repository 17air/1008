package com.example.cardify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var titleInput: EditText
    private lateinit var descInput: EditText
    private lateinit var maxInput: EditText
    private lateinit var addressView: TextView
    private lateinit var registerButton: Button

    private var currentLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        titleInput = findViewById(R.id.input_title)
        descInput = findViewById(R.id.input_desc)
        maxInput = findViewById(R.id.input_max_people)
        addressView = findViewById(R.id.text_location)
        registerButton = findViewById(R.id.button_register)

        fetchCurrentLocation()

        registerButton.setOnClickListener {
            registerGroup()
        }
    }

    private fun fetchCurrentLocation() {
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

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                val address = getAddressFromLocation(location)
                addressView.text = address ?: "위치를 불러오는 중..."
            } else {
                addressView.text = "현재 위치를 가져올 수 없습니다."
            }
        }.addOnFailureListener {
            addressView.text = "현재 위치를 가져올 수 없습니다."
        }
    }

    private fun getAddressFromLocation(location: Location): String? {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun registerGroup() {
        val title = titleInput.text.toString().trim()
        val desc = descInput.text.toString().trim()
        val maxPeople = maxInput.text.toString().trim()

        if (title.isEmpty() || desc.isEmpty() || maxPeople.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val location = currentLocation
        if (location == null) {
            Toast.makeText(this, "현재 위치를 확인 중입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val data = Intent().apply {
            putExtra("title", title)
            putExtra("desc", desc)
            putExtra("maxPeople", maxPeople)
            putExtra("latitude", location.latitude)
            putExtra("longitude", location.longitude)
        }
        setResult(RESULT_OK, data)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation()
        } else {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 2001
    }
}
