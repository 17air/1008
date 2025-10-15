package com.example.cardify.data

/**
 * Mirrors the jsonplaceholder post response used as a stand-in for group data.
 */
data class PostResponse(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String
)
