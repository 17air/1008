package com.example.cardify.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.cardify.R
import com.example.cardify.ai.LocalTagRecommender

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupCreateScreen(
    recommender: LocalTagRecommender,
    isSubmitting: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSubmit: (title: String, description: String, date: String, tags: List<String>) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var tagsInput by rememberSaveable { mutableStateOf("") }
    var recommendedTags by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(description) {
        recommendedTags = recommender.recommendTags(description)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = stringResource(id = R.string.group_create_title), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(id = R.string.group_title_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(id = R.string.group_description_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text(stringResource(id = R.string.group_date_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
        OutlinedTextField(
            value = tagsInput,
            onValueChange = { tagsInput = it },
            label = { Text(stringResource(id = R.string.group_tags_hint)) },
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )
        if (recommendedTags.isNotEmpty()) {
            Text(text = stringResource(id = R.string.group_recommended_tags_label), style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recommendedTags.forEach { tag ->
                    AssistChip(
                        onClick = { tagsInput = appendTag(tagsInput, tag) },
                        label = { Text("#$tag") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
        } else {
            Text(text = stringResource(id = R.string.group_recommended_tags_empty), style = MaterialTheme.typography.bodyMedium)
        }
        if (!errorMessage.isNullOrEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                onSubmit(title, description, date, tags)
            },
            enabled = !isSubmitting && title.isNotBlank() && description.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text(text = stringResource(id = R.string.group_create_action))
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), enabled = !isSubmitting) {
            Text(text = stringResource(id = R.string.back))
        }
    }
}

private fun appendTag(current: String, tag: String): String {
    val existing = current.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
    if (existing.add(tag)) {
        return existing.joinToString(separator = ", ")
    }
    return current
}
