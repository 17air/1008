package com.example.cardify.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cardify.data.model.ChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val groupId: String) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val messagesRef = db.collection("chats")
        .document(groupId)
        .collection("messages")

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var registration: ListenerRegistration? = null

    init {
        registration = messagesRef
            .orderBy("sentAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                val mapped = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(ChatMessage::class.java)?.copy(id = document.id)
                } ?: emptyList()
                _messages.value = mapped
            }
    }

    fun sendMessage(userId: String, userName: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val message = ChatMessage(
            userId = userId,
            userName = userName,
            body = trimmed,
            sentAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            runCatching { messagesRef.add(message) }
        }
    }

    override fun onCleared() {
        registration?.remove()
        super.onCleared()
    }
}

class ChatViewModelFactory(private val groupId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(groupId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
