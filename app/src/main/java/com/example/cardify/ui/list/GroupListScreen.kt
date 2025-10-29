package com.example.cardify.ui.list

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cardify.R
import com.example.cardify.ai.LocalTagRecommender
import com.example.cardify.data.model.Group

@Composable
fun GroupListScreen(
    groups: List<Group>,
    userId: String,
    userName: String,
    joiningGroups: Set<String>,
    recommender: LocalTagRecommender,
    onJoin: (String) -> Unit,
    onCreate: () -> Unit,
    onGroupSelected: (Group) -> Unit
) {
    val joinedGroups = remember(groups, userId) { groups.filter { it.members.contains(userId) } }
    val preferredTags = remember(joinedGroups) { joinedGroups.flatMap { it.tags }.distinct() }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.group_welcome_format, userName.ifEmpty { stringResource(id = R.string.user_guest_placeholder) }),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.group_create_action))
                }
            }
        }

        if (scoredGroups.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp
            ) {
                Text(
                    text = stringResource(id = R.string.group_list_empty),
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(scoredGroups) { (group, score) ->
                    GroupRow(
                        group = group,
                        similarity = score,
                        isMember = group.members.contains(userId),
                        isJoining = joiningGroups.contains(group.id),
                        isLeader = group.leaderId == userId,
                        onJoin = { onJoin(group.id) },
                        onViewDetail = { onGroupSelected(group) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupRow(
    group: Group,
    similarity: Float,
    isMember: Boolean,
    isJoining: Boolean,
    isLeader: Boolean,
    onJoin: () -> Unit,
    onViewDetail: () -> Unit
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
                if (similarity > 0.2f) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(id = R.string.group_recommended_badge)) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.tags.take(4).forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .padding(end = 4.dp)
                        ) {
                            Text(
                                text = "#${tag}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
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
