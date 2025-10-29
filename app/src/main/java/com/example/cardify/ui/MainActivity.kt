package com.example.cardify.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cardify.R
import com.example.cardify.UserSession
import com.example.cardify.ai.LocalTagRecommender
import com.example.cardify.data.model.Group
import com.example.cardify.ui.create.GroupCreateScreen
import com.example.cardify.ui.detail.GroupDetailScreen
import com.example.cardify.ui.joined.JoinedGroupsScreen
import com.example.cardify.ui.list.GroupListScreen
import com.example.cardify.ui.map.MapScreen
import com.example.cardify.ui.mygroups.MyOwnedGroupsScreen
import kotlinx.coroutines.launch

private const val MAP_ROUTE = "map_screen"
private const val LIST_ROUTE = "group_list"
private const val CREATE_ROUTE = "group_create"
private const val DETAIL_ROUTE = "group_detail"
private const val JOINED_ROUTE = "joined_groups"
private const val OWNED_ROUTE = "owned_groups"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CardifyApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardifyApp(viewModel: CardifyViewModel = viewModel()) {
    val navController = rememberNavController()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val joiningGroups by viewModel.joiningGroups.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userTag by viewModel.userTag.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val recommender = remember { LocalTagRecommender(context) }
    val coroutineScope = rememberCoroutineScope()

    var createError by rememberSaveable { mutableStateOf<String?>(null) }
    var isCreating by rememberSaveable { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: MAP_ROUTE

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showActionSheet by rememberSaveable { mutableStateOf(false) }

    val isMapRoute = currentRoute.startsWith(MAP_ROUTE)

    Scaffold(
        topBar = {
            if (isMapRoute) {
                CardifyTopBar(
                    userName = userName,
                    userTag = userTag,
                    onUserNameChanged = { viewModel.updateUserName(it) },
                    onUserTagChanged = { viewModel.updateUserTag(it) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MAP_ROUTE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MAP_ROUTE) {
                MapScreen(
                    groups = groups,
                    onGroupSelected = { group -> navController.navigate("$DETAIL_ROUTE/${group.id}") },
                    onOpenList = { navController.navigate(LIST_ROUTE) },
                    onOpenMore = { showActionSheet = true }
                )
            }
            composable(LIST_ROUTE) {
                GroupListScreen(
                    groups = groups,
                    userId = UserSession.userId,
                    userName = userName,
                    joiningGroups = joiningGroups,
                    recommender = recommender,
                    onJoin = { viewModel.joinGroup(it) },
                    onCreate = { navController.navigate(CREATE_ROUTE) },
                    onGroupSelected = { group -> navController.navigate("$DETAIL_ROUTE/${group.id}") }
                )
            }
            composable(JOINED_ROUTE) {
                JoinedGroupsScreen(
                    groups = groups,
                    currentUserId = UserSession.userId,
                    onBack = { navController.popBackStack() },
                    onGroupSelected = { group -> navController.navigate("$DETAIL_ROUTE/${group.id}") }
                )
            }
            composable(CREATE_ROUTE) {
                GroupCreateScreen(
                    recommender = recommender,
                    isSubmitting = isCreating,
                    errorMessage = createError,
                    onBack = { navController.popBackStack() },
                    onSubmit = { title, description, date, tags, location ->
                        createError = null
                        isCreating = true
                        coroutineScope.launch {
                            val result = viewModel.createGroup(title, description, date, tags, location)
                            isCreating = false
                            result.onSuccess { groupId ->
                                navController.popBackStack()
                                navController.navigate("$DETAIL_ROUTE/$groupId")
                            }.onFailure { error ->
                                createError = error.localizedMessage ?: "Unknown error"
                            }
                        }
                    }
                )
            }
            composable(OWNED_ROUTE) {
                MyOwnedGroupsScreen(
                    groups = groups,
                    currentUserId = UserSession.userId,
                    onBack = { navController.popBackStack() },
                    onEditGroup = { viewModel.updateGroup(it) },
                    onDeleteGroup = { viewModel.deleteGroup(it) },
                    onOpenDetail = { group -> navController.navigate("$DETAIL_ROUTE/${group.id}") }
                )
            }
            composable(
                route = "$DETAIL_ROUTE/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { entry ->
                val groupId = entry.arguments?.getString("groupId") ?: return@composable
                val groupFlow = remember(groupId) { viewModel.group(groupId) }
                val groupState by groupFlow.collectAsStateWithLifecycle()
                GroupDetailScreen(
                    group = groupState,
                    currentUserId = UserSession.userId,
                    isJoining = joiningGroups.contains(groupId),
                    onBack = { navController.popBackStack() },
                    onJoin = { viewModel.joinGroup(groupId) }
                )
            }
        }
    }

    if (showActionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionSheet = false },
            sheetState = sheetState
        ) {
            GroupActionsSheet(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp),
                onJoinedGroups = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showActionSheet = false
                        navController.navigate(JOINED_ROUTE)
                    }
                },
                onCreateGroup = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showActionSheet = false
                        navController.navigate(CREATE_ROUTE)
                    }
                },
                onOwnedGroups = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showActionSheet = false
                        navController.navigate(OWNED_ROUTE)
                    }
                }
            )
        }
    }
}

@Composable
private fun GroupActionsSheet(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onJoinedGroups: () -> Unit,
    onCreateGroup: () -> Unit,
    onOwnedGroups: () -> Unit
) {
    Column(modifier = modifier.padding(contentPadding)) {
        ListItem(
            headlineContent = { Text(text = stringResource(id = R.string.action_joined_groups)) },
            supportingContent = { Text(text = stringResource(id = R.string.action_joined_groups_hint)) },
            leadingContent = { Icon(imageVector = Icons.Filled.People, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(onClick = onJoinedGroups)
        )
        ListItem(
            headlineContent = { Text(text = stringResource(id = R.string.action_create_group)) },
            supportingContent = { Text(text = stringResource(id = R.string.action_create_group_hint)) },
            leadingContent = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(onClick = onCreateGroup)
        )
        ListItem(
            headlineContent = { Text(text = stringResource(id = R.string.action_owned_groups)) },
            supportingContent = { Text(text = stringResource(id = R.string.action_owned_groups_hint)) },
            leadingContent = { Icon(imageVector = Icons.Filled.Settings, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(onClick = onOwnedGroups)
        )
    }
}
