package com.example.cardify.ui.mygroups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.cardify.R
import com.example.cardify.UserSession
import com.example.cardify.data.LocalGroupRepository
import com.example.cardify.data.model.Group
import com.example.cardify.ui.components.GroupSummaryCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGroupsScreen(
    onBack: () -> Unit,
    onGroupSelected: (String) -> Unit
) {
    val userId = remember { UserSession.userId }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var hasError by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<Group?>(null) }
    var deleteTarget by remember { mutableStateOf<Group?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val showMessage: (String) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    DisposableEffect(userId) {
        if (userId.isBlank()) {
            isLoading = false
            groups = emptyList()
            hasError = true
            onDispose { }
        } else {
            val registration = LocalGroupRepository.observeGroups(
                onSuccess = { list ->
                    isLoading = false
                    hasError = false
                    groups = list.filter { it.ownerId == userId }
                },
                onError = {
                    isLoading = false
                    hasError = true
                }
            )
            onDispose { registration.remove() }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = R.string.group_actions_my_groups)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        when {
            isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
            hasError -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.group_actions_error_generic),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            groups.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.my_groups_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    items(groups, key = { it.id }) { group ->
                        GroupSummaryCard(
                            group = group,
                            onClick = { onGroupSelected(group.id) },
                            actions = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { editingGroup = group },
                                        enabled = !isProcessing
                                    ) {
                                        Text(text = stringResource(id = R.string.my_groups_action_edit))
                                    }
                                    TextButton(
                                        onClick = { deleteTarget = group },
                                        enabled = !isProcessing,
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text(text = stringResource(id = R.string.my_groups_action_delete))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    val processingUserId = userId
    editingGroup?.let { group ->
        EditGroupDialog(
            group = group,
            isProcessing = isProcessing,
            onDismiss = { if (!isProcessing) editingGroup = null },
            onConfirm = { title, description, tags, maxPeople ->
                val trimmedTitle = title.trim()
                val trimmedDescription = description.trim()
                val max = maxPeople.trim().toIntOrNull()
                if (trimmedTitle.isEmpty() || trimmedDescription.isEmpty()) {
                    showMessage(context.getString(R.string.fill_all_fields))
                    return@EditGroupDialog
                }
                if (max == null || max <= 0) {
                    showMessage(context.getString(R.string.invalid_max_people))
                    return@EditGroupDialog
                }
                val tagList = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                isProcessing = true
                LocalGroupRepository.updateGroup(
                    groupId = group.id,
                    ownerId = processingUserId,
                    title = trimmedTitle,
                    description = trimmedDescription,
                    tags = tagList,
                    maxPeople = max,
                    onSuccess = {
                        isProcessing = false
                        editingGroup = null
                        showMessage(context.getString(R.string.my_groups_update_success))
                    },
                    onError = { exception ->
                        isProcessing = false
                        val message = when (exception) {
                            is LocalGroupRepository.MaxPeopleTooLowException ->
                                context.getString(
                                    R.string.my_groups_error_max_people,
                                    exception.requiredMin
                                )
                            is LocalGroupRepository.NotGroupOwnerException ->
                                context.getString(R.string.group_actions_error_generic)
                            else -> context.getString(R.string.my_groups_update_error)
                        }
                        showMessage(message)
                    }
                )
            }
        )
    }

    deleteTarget?.let { group ->
        AlertDialog(
            onDismissRequest = { if (!isProcessing) deleteTarget = null },
            title = { Text(text = stringResource(id = R.string.my_groups_delete_title)) },
            text = {
                Text(text = stringResource(id = R.string.my_groups_delete_message, group.title))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isProcessing) return@TextButton
                        isProcessing = true
                        LocalGroupRepository.deleteGroup(
                            groupId = group.id,
                            ownerId = processingUserId,
                            onSuccess = {
                                isProcessing = false
                                deleteTarget = null
                                showMessage(context.getString(R.string.my_groups_delete_success))
                            },
                            onError = { exception ->
                                isProcessing = false
                                val message = when (exception) {
                                    is LocalGroupRepository.NotGroupOwnerException ->
                                        context.getString(R.string.group_actions_error_generic)
                                    else -> context.getString(R.string.my_groups_delete_error)
                                }
                                showMessage(message)
                            }
                        )
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(text = stringResource(id = R.string.my_groups_action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }, enabled = !isProcessing) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun EditGroupDialog(
    group: Group,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, tags: String, maxPeople: String) -> Unit
) {
    var title by remember(group) { mutableStateOf(group.title) }
    var description by remember(group) { mutableStateOf(group.description) }
    var tags by remember(group) { mutableStateOf(group.tags.joinToString(", ")) }
    var maxPeople by remember(group) { mutableStateOf(group.maxPeople.toString()) }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { Text(text = stringResource(id = R.string.my_groups_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(text = stringResource(id = R.string.group_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(text = stringResource(id = R.string.group_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text(text = stringResource(id = R.string.group_tags)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                )
                OutlinedTextField(
                    value = maxPeople,
                    onValueChange = { maxPeople = it },
                    label = { Text(text = stringResource(id = R.string.group_max_people)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title, description, tags, maxPeople) }, enabled = !isProcessing) {
                Text(text = stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}
