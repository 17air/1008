package com.example.cardify.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cardify.R
import com.example.cardify.data.model.Group
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupDetailScreen(
    group: Group?,
    currentUserId: String,
    isJoining: Boolean,
    onBack: () -> Unit,
    onJoin: (String) -> Unit,
    onOpenChat: (Group) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = group?.title ?: stringResource(id = R.string.group_detail_loading),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text = stringResource(id = R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (group == null) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = stringResource(id = R.string.group_detail_loading))
            }
            return@Scaffold
        }

        val isMember = group.members.contains(currentUserId)
        val leaderLabel = if (group.leaderId == currentUserId) stringResource(id = R.string.group_leader_badge) else null
        val location = remember(group.latitude, group.longitude) {
            if (group.latitude == 0.0 && group.longitude == 0.0) null else LatLng(group.latitude, group.longitude)
        }
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(location ?: LatLng(37.5665, 126.9780), 13f)
        }

        LaunchedEffect(location) {
            location?.let {
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 14f))
            }
        }

        val scrollState = rememberScrollState()

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (location != null) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = false)
                ) {
                    Marker(state = MarkerState(position = location), title = group.title)
                }
                Text(
                    text = stringResource(
                        id = R.string.group_detail_location_summary,
                        location.latitude,
                        location.longitude
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(text = stringResource(id = R.string.group_detail_location_missing))
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = group.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(id = R.string.group_date_format, group.date.ifBlank { stringResource(id = R.string.group_date_unknown) }),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(id = R.string.group_leader_format, group.leaderName.ifBlank { stringResource(id = R.string.user_guest_placeholder) }),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (leaderLabel != null) FontWeight.Bold else FontWeight.Normal
                )
                leaderLabel?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = stringResource(id = R.string.group_member_count_format, group.members.size),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(id = R.string.group_detail_description_header), style = MaterialTheme.typography.titleMedium)
                Text(text = group.description, style = MaterialTheme.typography.bodyLarge)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(id = R.string.group_detail_tags_header), style = MaterialTheme.typography.titleMedium)
                if (group.tags.isEmpty()) {
                    Text(text = stringResource(id = R.string.group_detail_tags_empty))
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        group.tags.forEach { tag ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "#${tag}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isMember) {
                Button(
                    onClick = { onOpenChat(group) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.group_chat_with_members))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = { onJoin(group.id) },
                enabled = !isMember && !isJoining,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when {
                        isMember -> stringResource(id = R.string.group_joined_button)
                        isJoining -> stringResource(id = R.string.group_joining_button)
                        else -> stringResource(id = R.string.group_join_button)
                    }
                )
            }
        }
    }
}
