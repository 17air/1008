package com.example.cardify.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cardify.UserSession
import com.example.cardify.ai.LocalTagRecommender
import com.example.cardify.data.model.Group
import com.example.cardify.ui.chat.GroupChatScreen
import com.example.cardify.ui.create.GroupCreateScreen
import com.example.cardify.ui.list.GroupListScreen
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

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

    Scaffold(
        topBar = {
            CardifyTopBar(
                userName = userName,
                onUserNameChanged = { viewModel.updateUserName(it) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("create") }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "list",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("list") {
                GroupListScreen(
                    groups = groups,
                    userId = UserSession.userId,
                    userName = userName,
                    joiningGroups = joiningGroups,
                    recommender = recommender,
                    onJoin = { viewModel.joinGroup(it) },
                    onCreate = { navController.navigate("create") },
                    onOpenChat = { group ->
                        navigateToChat(navController, group)
                    }
                )
            }
            composable("create") {
                GroupCreateScreen(
                    recommender = recommender,
                    isSubmitting = isCreating,
                    errorMessage = createError,
                    onBack = { navController.popBackStack() },
                    onSubmit = { title, description, date, tags ->
                        createError = null
                        isCreating = true
                        coroutineScope.launch {
                            val result = viewModel.createGroup(title, description, date, tags)
                            isCreating = false
                            result.onSuccess { groupId ->
                                navController.popBackStack()
                                val encodedTitle = Uri.encode(title)
                                navController.navigate("chat/$groupId?title=$encodedTitle")
                            }.onFailure { error ->
                                createError = error.localizedMessage ?: "Unknown error"
                            }
                        }
                    }
                )
            }
            composable(
                route = "chat/{groupId}?title={title}",
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
    navController.navigate("chat/${group.id}?title=$encodedTitle")
}
