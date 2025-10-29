package com.example.cardify.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardify.UserSession
import com.example.cardify.data.GroupRepository
import com.example.cardify.data.model.ChatMessage
import com.example.cardify.data.model.Group
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CardifyViewModel : ViewModel() {
    private val userId: String = UserSession.userId

    private val _userName = MutableStateFlow(UserSession.userName)
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userTag = MutableStateFlow(UserSession.userTag)
    val userTag: StateFlow<String> = _userTag.asStateFlow()

    private val _joiningGroups = MutableStateFlow<Set<String>>(emptySet())
    val joiningGroups: StateFlow<Set<String>> = _joiningGroups.asStateFlow()

    val groups: StateFlow<List<Group>> = GroupRepository.observeGroups()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            runCatching {
                GroupRepository.createOrUpdateUser(userId, _userName.value, _userTag.value)
            }
        }
    }

    fun updateUserName(name: String) {
        if (name == _userName.value) return
        _userName.value = name
        UserSession.userName = name
        viewModelScope.launch {
            runCatching { GroupRepository.createOrUpdateUser(userId, name, _userTag.value) }
        }
    }

    fun updateUserTag(tag: String) {
        if (tag == _userTag.value) return
        _userTag.value = tag
        UserSession.userTag = tag
        viewModelScope.launch {
            runCatching { GroupRepository.createOrUpdateUser(userId, _userName.value, tag) }
        }
    }

    fun joinGroup(groupId: String) {
        if (_joiningGroups.value.contains(groupId)) return
        _joiningGroups.update { it + groupId }
        viewModelScope.launch {
            runCatching { GroupRepository.joinGroup(groupId, userId) }
            _joiningGroups.update { it - groupId }
        }
    }

    suspend fun createGroup(
        title: String,
        description: String,
        date: String,
        tags: List<String>,
        location: LatLng
    ): Result<String> {
        val trimmedTags = tags.mapNotNull { tag ->
            val cleaned = tag.trim()
            cleaned.takeIf { it.isNotEmpty() }
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                GroupRepository.createGroup(
                    title = title.trim(),
                    description = description.trim(),
                    date = date.trim(),
                    tags = trimmedTags,
                    leaderId = userId,
                    leaderName = displayName(),
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            }
        }
    }

    fun group(groupId: String): StateFlow<Group?> {
        return GroupRepository.observeGroup(groupId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )
    }

    fun chatMessages(groupId: String): StateFlow<List<ChatMessage>> {
        return GroupRepository.observeChatMessages(groupId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
    }

    suspend fun sendMessage(groupId: String, text: String) {
        val message = ChatMessage(
            userId = userId,
            userName = displayName().ifEmpty { "익명" },
            body = text.trim(),
            sentAt = System.currentTimeMillis()
        )
        withContext(Dispatchers.IO) {
            if (message.body.isNotEmpty()) {
                GroupRepository.sendMessage(groupId, message)
            }
        }
    }

    fun updateGroup(group: Group) {
        viewModelScope.launch {
            val updated = group.copy(leaderName = displayName())
            runCatching { GroupRepository.updateGroup(updated) }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            runCatching { GroupRepository.deleteGroup(groupId) }
        }
    }

    private fun displayName(): String {
        val name = _userName.value.ifBlank { "게스트" }
        val tag = _userTag.value.trim()
        return if (tag.isBlank()) name else "$name ○ #$tag"
    }
}
