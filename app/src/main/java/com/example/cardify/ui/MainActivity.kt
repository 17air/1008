package com.example.cardify.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import com.example.cardify.ui.chat.GroupChatScreen
import com.example.cardify.ui.create.GroupCreateScreen
import com.example.cardify.ui.detail.GroupDetailScreen
import com.example.cardify.ui.list.GroupListScreen
import com.example.cardify.ui.map.MapScreen
import kotlinx.coroutines.launch

private const val MAP_ROUTE = "map_screen"
private const val LIST_ROUTE = "group_list"
private const val CREATE_ROUTE = "group_create"
private const val DETAIL_ROUTE = "group_detail"
private const val CHAT_ROUTE = "group_chat"

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
    val context = LocalContext.current
    val recommender = remember { LocalTagRecommender(context) }
    val coroutineScope = rememberCoroutineScope()

    var createError by rememberSaveable { mutableStateOf<String?>(null) }
    var isCreating by rememberSaveable { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: MAP_ROUTE

    val fab: (@Composable () -> Unit)? = when {
        currentRoute.startsWith(MAP_ROUTE) -> {
            {
                FloatingActionButton(onClick = { navController.navigate(LIST_ROUTE) }) {
                    Icon(
                        imageVector = Icons.Filled.List,
                        contentDescription = stringResource(id = R.string.map_open_list)
                    )
                }
            }
        }
        currentRoute.startsWith(LIST_ROUTE) -> {
            {
                FloatingActionButton(onClick = { navController.navigate(CREATE_ROUTE) }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.group_create_action)
                    )
                }
            }
        }
        else -> null
    }

    Scaffold(
        topBar = {
            CardifyTopBar(
                userName = userName,
                onUserNameChanged = { viewModel.updateUserName(it) }
            )
        },
        floatingActionButton = { fab?.invoke() }
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
                    onOpenList = { navController.navigate(LIST_ROUTE) }
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
                    onJoin = { viewModel.joinGroup(groupId) },
                    onOpenChat = { group -> navigateToChat(navController, group) }
                )
            }
            composable(
                route = "$CHAT_ROUTE/{groupId}?title={title}",
                arguments = listOf(
                    navArgument("groupId") { type = NavType.StringType },
                    navArgument("title") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    }
                )
            ) { entry ->
                val groupId = entry.arguments?.getString("groupId") ?: return@composable
                val title = entry.arguments?.getString("title").orEmpty()
                val messagesState = viewModel.chatMessages(groupId).collectAsStateWithLifecycle()
                GroupChatScreen(
                    title = title,
                    messages = messagesState.value,
                    currentUserId = UserSession.userId,
                    onBack = { navController.popBackStack() },
                    onSendMessage = { text ->
                        coroutineScope.launch { viewModel.sendMessage(groupId, text) }
                    }
                )
            }
        }
    }
}

private fun navigateToChat(navController: androidx.navigation.NavHostController, group: Group) {
    val encodedTitle = Uri.encode(group.title)
    navController.navigate("$CHAT_ROUTE/${group.id}?title=$encodedTitle")
}
