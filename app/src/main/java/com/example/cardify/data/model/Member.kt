package com.example.cardify.data.model

data class Member(
    val userId: String = "",
    val name: String = "",
    val joinedAt: Long = System.currentTimeMillis()
)
