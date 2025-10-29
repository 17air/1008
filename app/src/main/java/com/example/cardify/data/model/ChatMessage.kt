package com.example.cardify.data.model

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val body: String = "",
    val sentAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromMap(id: String, data: Map<String, Any?>): ChatMessage {
            val timestampValue = data["sentAt"] ?: data["timestamp"]
            val ts = when (timestampValue) {
                is Timestamp -> timestampValue.toDate().time
                is Long -> timestampValue
                is Number -> timestampValue.toLong()
                else -> System.currentTimeMillis()
            }
            return ChatMessage(
                id = id,
                userId = data["userId"] as? String ?: data["senderId"] as? String ?: "",
                userName = data["userName"] as? String ?: data["senderName"] as? String ?: "",
                body = data["body"] as? String ?: data["text"] as? String ?: "",
                sentAt = ts
            )
        }
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "userName" to userName,
        "body" to body,
        "sentAt" to sentAt
    )
}
