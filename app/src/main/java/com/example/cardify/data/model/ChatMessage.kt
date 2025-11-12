package com.example.cardify.data.model

data class ChatMessage(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val body: String = "",
    val sentAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", System.currentTimeMillis())
}
