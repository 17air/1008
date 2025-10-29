package com.example.cardify.data.model

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromMap(id: String, data: Map<String, Any?>): ChatMessage {
            val timestampValue = data["timestamp"]
            val ts = when (timestampValue) {
                is Timestamp -> timestampValue.toDate().time
                is Long -> timestampValue
                is Number -> timestampValue.toLong()
                else -> System.currentTimeMillis()
            }
            return ChatMessage(
                id = id,
                senderId = data["senderId"] as? String ?: "",
                senderName = data["senderName"] as? String ?: "",
                text = data["text"] as? String ?: "",
                timestamp = ts
            )
        }
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "senderId" to senderId,
        "senderName" to senderName,
        "text" to text,
        "timestamp" to timestamp
    )
}
