package com.example.cardify.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cardify.R

@Composable
fun CardifyTopBar(
    userName: String,
    userTag: String,
    onUserNameChanged: (String) -> Unit,
    onUserTagChanged: (String) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    id = R.string.user_display_with_tag,
                    userName.ifBlank { stringResource(id = R.string.user_guest_placeholder) },
                    userTag.ifBlank { stringResource(id = R.string.user_tag_placeholder) }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = userName,
                onValueChange = { onUserNameChanged(it.take(40)) },
                label = { Text(stringResource(id = R.string.user_name_field_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = userTag,
                onValueChange = { onUserTagChanged(it.take(20)) },
                label = { Text(stringResource(id = R.string.user_tag_field_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
