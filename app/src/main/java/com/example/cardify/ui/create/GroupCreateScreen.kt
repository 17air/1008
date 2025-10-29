package com.example.cardify.ui.create

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.cardify.R
import com.example.cardify.ai.LocalTagRecommender
import com.example.cardify.ui.location.DefaultSeoulLatLng
import com.example.cardify.ui.location.fetchCurrentLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupCreateScreen(
    recommender: LocalTagRecommender,
    isSubmitting: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSubmit: (title: String, description: String, date: String, tags: List<String>, location: LatLng) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var tagsInput by rememberSaveable { mutableStateOf("") }
    var recommendedTags by remember { mutableStateOf<List<String>>(emptyList()) }

    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }
    var selectedLocation by rememberSaveable { mutableStateOf<LatLng?>(null) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DefaultSeoulLatLng, 12f)
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(description) {
        recommendedTags = recommender.recommendTags(description)
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && selectedLocation == null) {
            val current = fetchCurrentLocation(fusedClient)
            if (current != null) {
                selectedLocation = current
            }
        }
    }

    LaunchedEffect(selectedLocation) {
        selectedLocation?.let { location ->
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(location, 14f))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = stringResource(id = R.string.group_create_title), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(id = R.string.group_title_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(id = R.string.group_description_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text(stringResource(id = R.string.group_date_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
        OutlinedTextField(
            value = tagsInput,
            onValueChange = { tagsInput = it },
            label = { Text(stringResource(id = R.string.group_tags_hint)) },
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )
        Text(
            text = stringResource(id = R.string.group_create_location_instruction),
            style = MaterialTheme.typography.bodyMedium
        )
        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasPermission),
            uiSettings = MapUiSettings(zoomControlsEnabled = false),
            onMapLongClick = { latLng -> selectedLocation = latLng }
        ) {
            selectedLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = stringResource(id = R.string.group_create_marker_title)
                )
            }
        }
        if (selectedLocation != null) {
            Text(
                text = stringResource(
                    id = R.string.group_create_selected_location,
                    selectedLocation!!.latitude,
                    selectedLocation!!.longitude
                ),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text(text = stringResource(id = R.string.location_not_selected), style = MaterialTheme.typography.bodySmall)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        val location = fetchCurrentLocation(fusedClient)
                        if (location != null) {
                            selectedLocation = location
                        }
                    }
                },
                enabled = hasPermission && !isSubmitting
            ) {
                Text(text = stringResource(id = R.string.group_create_use_current_location))
            }
            if (!hasPermission) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.location_permission_denied), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (recommendedTags.isNotEmpty()) {
            Text(text = stringResource(id = R.string.group_recommended_tags_label), style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recommendedTags.forEach { tag ->
                    AssistChip(
                        onClick = { tagsInput = appendTag(tagsInput, tag) },
                        label = { Text("#$tag") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
        } else {
            Text(text = stringResource(id = R.string.group_recommended_tags_empty), style = MaterialTheme.typography.bodyMedium)
        }
        if (!errorMessage.isNullOrEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val location = selectedLocation
                if (location != null) {
                    onSubmit(title, description, date, tags, location)
                }
            },
            enabled = !isSubmitting && title.isNotBlank() && description.isNotBlank() && selectedLocation != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text(text = stringResource(id = R.string.group_create_action))
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), enabled = !isSubmitting) {
            Text(text = stringResource(id = R.string.back))
        }
    }
}

private fun appendTag(current: String, tag: String): String {
    val existing = current.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
    if (existing.add(tag)) {
        return existing.joinToString(separator = ", ")
    }
    return current
}
