package com.example.cardify.ui.mygroups

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cardify.R
import com.example.cardify.data.model.Group

@Composable
fun MyOwnedGroupsScreen(
    groups: List<Group>,
    currentUserId: String,
    onBack: () -> Unit,
    onEditGroup: (Group) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onOpenDetail: (Group) -> Unit
) {
    val owned = groups.filter { it.leaderId == currentUserId }
    var editingGroup by remember { mutableStateOf<Group?>(null) }
    var deletingGroup by remember { mutableStateOf<Group?>(null) }
    var titleInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }

    LaunchedEffect(editingGroup) {
        editingGroup?.let { group ->
            titleInput = group.title
            descriptionInput = group.description
            dateInput = group.date
            tagsInput = group.tags.joinToString(", ")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.owned_groups_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text = stringResource(id = R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (owned.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = stringResource(id = R.string.owned_groups_empty), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(owned) { group ->
                    OwnedGroupRow(
                        group = group,
                        onView = { onOpenDetail(group) },
                        onEdit = { editingGroup = group },
                        onDelete = { deletingGroup = group }
                    )
                }
            }
        }
    }

    editingGroup?.let { group ->
        AlertDialog(
            onDismissRequest = { editingGroup = null },
            title = { Text(text = stringResource(id = R.string.owned_groups_edit_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text(text = stringResource(id = R.string.group_title_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it },
                        label = { Text(text = stringResource(id = R.string.group_description_hint)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text(text = stringResource(id = R.string.group_date_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        label = { Text(text = stringResource(id = R.string.group_tags_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onEditGroup(
                        group.copy(
                            title = titleInput.trim(),
                            description = descriptionInput.trim(),
                            date = dateInput.trim(),
                            tags = tags
                        )
                    )
                    editingGroup = null
                }) {
                    Text(text = stringResource(id = R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingGroup = null }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }

    deletingGroup?.let { group ->
        AlertDialog(
            onDismissRequest = { deletingGroup = null },
            title = { Text(text = stringResource(id = R.string.owned_groups_delete_title)) },
            text = { Text(text = stringResource(id = R.string.owned_groups_delete_message, group.title)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteGroup(group.id)
                    deletingGroup = null
                }) {
                    Text(text = stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingGroup = null }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun OwnedGroupRow(
    group: Group,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp),
        onClick = onView
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.group_date_format, group.date.ifBlank { stringResource(id = R.string.group_date_unknown) }),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = group.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onEdit) {
                    Text(text = stringResource(id = R.string.edit))
                }
                Button(onClick = onDelete) {
                    Text(text = stringResource(id = R.string.delete))
                }
            }
        }
    }
}
