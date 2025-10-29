package com.example.cardify.ui.list

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.cardify.R
import com.example.cardify.ai.LocalTagRecommender
import com.example.cardify.data.model.Group
import com.google.android.gms.maps.model.LatLng

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupListScreen(
    groups: List<Group>,
    userId: String,
    userName: String,
    userTag: String,
    userLocation: LatLng?,
    joiningGroups: Set<String>,
    recommender: LocalTagRecommender,
    onJoin: (String) -> Unit,
    onCreate: () -> Unit,
    onGroupSelected: (Group) -> Unit,
    onGroupHighlighted: (Group) -> Unit
) {
    val joinedGroups = remember(groups, userId) { groups.filter { it.members.contains(userId) } }
    val preferredTags = remember(joinedGroups) { joinedGroups.flatMap { it.tags }.distinct() }
    val normalizedUserTag = remember(userTag) { userTag.trim() }
    val scoredGroups = remember(groups, preferredTags) {
        groups.map { group ->
            val similarity = if (preferredTags.isEmpty()) {
                0f
            } else {
                recommender.similarityBetween(preferredTags, group.tags) ?: 0f
            }
            group to similarity
        }.sortedWith(compareByDescending<Pair<Group, Float>> { it.second }.thenByDescending { it.first.createdAt })
    }

    val highlightedGroups = remember(groups, normalizedUserTag) {
        if (normalizedUserTag.isEmpty()) {
            emptyList()
        } else {
            groups.mapNotNull { group ->
                val exact = group.tags.any { it.equals(normalizedUserTag, ignoreCase = true) }
                val partial = group.tags.any { tag -> tag.contains(other = normalizedUserTag, ignoreCase = true) }
                val similarity = recommender.similarityBetween(listOf(normalizedUserTag), group.tags) ?: 0f
                val score = when {
                    exact -> 2f
                    partial -> 1.5f
                    similarity.isNaN() || similarity <= 0f -> 0f
                    else -> similarity
                }
                if (score > 0f) group to score else null
            }
                .sortedByDescending { it.second }
                .map { it.first }
                .distinct()
        }
    }

    val highlightedLimited = remember(highlightedGroups) { highlightedGroups.take(5) }

    val nearbyGroups = remember(groups, userLocation) {
        if (userLocation == null) {
            emptyList()
        } else {
            groups.mapNotNull { group ->
                if (group.latitude == 0.0 && group.longitude == 0.0) {
                    null
                } else {
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        userLocation.latitude,
                        userLocation.longitude,
                        group.latitude,
                        group.longitude,
                        results
                    )
                    val distanceMeters = results.firstOrNull() ?: Float.NaN
                    if (distanceMeters.isNaN() || distanceMeters > FIVE_KM_METERS) {
                        null
                    } else {
                        group to distanceMeters
                    }
                }
            }
                .sortedBy { it.second }
                .map { it.first }
        }
    }

    val nearbyLimited = remember(nearbyGroups) { nearbyGroups.take(5) }

    val listState = rememberLazyListState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var pendingScrollTarget by remember { mutableStateOf<Group?>(null) }

    val filteredPairs = remember(scoredGroups, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            scoredGroups
        } else {
            scoredGroups.filter { (group, _) ->
                group.title.contains(other = query, ignoreCase = true) ||
                    group.description.contains(other = query, ignoreCase = true) ||
                    group.tags.any { tag -> tag.contains(other = query, ignoreCase = true) }
            }
        }
    }

    LaunchedEffect(pendingScrollTarget, filteredPairs) {
        val target = pendingScrollTarget ?: return@LaunchedEffect
        val position = filteredPairs.indexOfFirst { (group, _) -> group.id == target.id }
        if (position >= 0) {
            val headerOffset = 5
            listState.animateScrollToItem(headerOffset + position)
            onGroupHighlighted(target)
            pendingScrollTarget = null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        state = listState
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(
                            id = R.string.group_welcome_format,
                            userName.ifEmpty { stringResource(id = R.string.user_guest_placeholder) }
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(id = R.string.group_create_action))
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tagLabel = if (normalizedUserTag.isBlank()) {
                    stringResource(id = R.string.group_list_tag_unknown)
                } else {
                    normalizedUserTag
                }
                val similarCount = highlightedLimited.size
                Text(
                    text = stringResource(
                        id = R.string.group_list_similar_header_full,
                        tagLabel,
                        similarCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (similarCount == 0) {
                    Text(
                        text = stringResource(id = R.string.group_list_similar_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    highlightedLimited.forEach { group ->
                        Text(
                            text = group.title,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable {
                                searchQuery = ""
                                pendingScrollTarget = group
                            }
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val nearbyCount = nearbyLimited.size
                Text(
                    text = stringResource(
                        id = R.string.group_list_radius_header,
                        nearbyCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (nearbyCount == 0) {
                    Text(
                        text = stringResource(id = R.string.group_list_radius_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    nearbyLimited.forEach { group ->
                        Text(
                            text = group.title,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable {
                                searchQuery = ""
                                pendingScrollTarget = group
                            }
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(text = stringResource(id = R.string.group_search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
        }

        item {
            Text(
                text = stringResource(id = R.string.group_list_all_groups, groups.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }

        if (filteredPairs.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp
                ) {
                    val message = if (searchQuery.isBlank()) {
                        stringResource(id = R.string.group_list_empty)
                    } else {
                        stringResource(id = R.string.group_search_empty, searchQuery)
                    }
                    Text(
                        text = message,
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            items(filteredPairs) { (group, _) ->
                GroupRow(
                    group = group,
                    isMember = group.members.contains(userId),
                    isJoining = joiningGroups.contains(group.id),
                    isLeader = group.leaderId == userId,
                    onJoin = { onJoin(group.id) },
                    onViewDetail = { onGroupSelected(group) },
                    onTagSelected = { tag ->
                        searchQuery = tag
                    }
                )
            }
        }
    }
}

private const val FIVE_KM_METERS = 5_000f

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupRow(
    group: Group,
    isMember: Boolean,
    isJoining: Boolean,
    isLeader: Boolean,
    onJoin: () -> Unit,
    onViewDetail: () -> Unit,
    onTagSelected: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetail() }
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.group_date_format, group.date.ifEmpty { stringResource(id = R.string.group_date_unknown) }),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = group.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.group_member_count_format, group.members.size),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(
                        id = R.string.group_leader_format,
                        group.leaderName.ifEmpty { stringResource(id = R.string.user_guest_placeholder) }
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isLeader) {
                    Text(
                        text = stringResource(id = R.string.group_leader_badge),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (group.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    group.tags.take(4).forEach { tag ->
                        AssistChip(
                            onClick = { onTagSelected(tag) },
                            label = { Text(text = "#$tag") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onViewDetail, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(id = R.string.group_view_details))
                }
                Button(
                    onClick = onJoin,
                    enabled = !isMember && !isJoining,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isMember) {
                            stringResource(id = R.string.group_joined_button)
                        } else {
                            stringResource(id = if (isJoining) R.string.group_joining_button else R.string.group_join_button)
                        }
                    )
                }
            }
        }
    }
}
