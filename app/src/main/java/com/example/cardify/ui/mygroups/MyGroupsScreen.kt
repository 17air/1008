package com.example.cardify.ui.mygroups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cardify.R
import com.example.cardify.UserSession
import com.example.cardify.data.LocalGroupRepository
import com.example.cardify.data.model.Group
import com.example.cardify.ui.components.GroupSummaryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGroupsScreen(
    onBack: () -> Unit,
    onGroupSelected: (String) -> Unit
) {
    val userId = remember { UserSession.userId }
    var isLoading by remember { mutableStateOf(true) }
    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var hasError by remember { mutableStateOf(false) }

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
        }
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
                            onClick = { onGroupSelected(group.id) }
                        )
                    }
                }
            }
        }
    }
}
