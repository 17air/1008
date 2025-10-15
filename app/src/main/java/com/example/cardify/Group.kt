package com.example.cardify

data class Group(
    val title: String,
    val description: String,
    val maxPeople: Int,
    val latitude: Double? = null,
    val longitude: Double? = null
)
