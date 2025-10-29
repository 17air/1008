package com.example.cardify.ui.actions

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cardify.R
import com.example.cardify.ui.create.GroupCreationScreen
import com.example.cardify.ui.joined.JoinedGroupsScreen
import com.example.cardify.ui.mygroups.MyGroupsScreen
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class GroupActionsBottomSheet : BottomSheetDialogFragment() {

    interface ActionHandler {
        fun onRequestCreateGroup()
        fun onRequestOpenGroupDetail(groupId: String)
    }

    private var actionHandler: ActionHandler? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        actionHandler = when {
            parentFragment is ActionHandler -> parentFragment as ActionHandler
            context is ActionHandler -> context
            else -> null
        }
    }

    override fun onDetach() {
        super.onDetach()
        if (parentFragment !== actionHandler) {
            actionHandler = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val composeView = ComposeView(requireContext())
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent {
            MaterialTheme {
                GroupActionsSheet(
                    onDismiss = { dismissAllowingStateLoss() },
                    onOpenCreateGroup = {
                        actionHandler?.onRequestCreateGroup()
                        dismissAllowingStateLoss()
                    },
                    onOpenGroupDetail = { groupId ->
                        actionHandler?.onRequestOpenGroupDetail(groupId)
                        dismissAllowingStateLoss()
                    }
                )
            }
        }
        return composeView
    }

    companion object {
        const val TAG = "GroupActionsBottomSheet"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupActionsSheet(
    onDismiss: () -> Unit,
    onOpenCreateGroup: () -> Unit,
    onOpenGroupDetail: (String) -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "menu") {
        composable("menu") {
            GroupActionsMenuScreen(
                onJoinedGroups = { navController.navigate("joined") },
                onCreateGroup = { navController.navigate("create") },
                onMyGroups = { navController.navigate("my") },
                onDismiss = onDismiss
            )
        }
        composable("joined") {
            JoinedGroupsScreen(
                onBack = { navController.popBackStack() },
                onGroupSelected = onOpenGroupDetail
            )
        }
        composable("create") {
            GroupCreationScreen(
                onBack = { navController.popBackStack() },
                onLaunchCreateForm = onOpenCreateGroup
            )
        }
        composable("my") {
            MyGroupsScreen(
                onBack = { navController.popBackStack() },
                onGroupSelected = onOpenGroupDetail
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupActionsMenuScreen(
    onJoinedGroups: () -> Unit,
    onCreateGroup: () -> Unit,
    onMyGroups: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 24.dp)) {
        Text(
            text = stringResource(id = R.string.group_actions_menu_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
        )
        ListItem(
            headlineContent = { Text(text = stringResource(id = R.string.group_actions_joined_groups)) },
            supportingContent = { Text(text = stringResource(id = R.string.group_actions_joined_groups_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            onClick = onJoinedGroups
        )
        ListItem(
            headlineContent = { Text(text = stringResource(id = R.string.group_actions_create_group)) },
            supportingContent = { Text(text = stringResource(id = R.string.group_actions_create_group_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            onClick = onCreateGroup
        )
        ListItem(
            headlineContent = { Text(text = stringResource(id = R.string.group_actions_my_groups)) },
            supportingContent = { Text(text = stringResource(id = R.string.group_actions_my_groups_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            onClick = onMyGroups
        )
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.close))
        }
    }
}
