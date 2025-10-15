package com.example.cardify.api

import com.example.cardify.data.PostResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit definition for group-related endpoints backed by the jsonplaceholder service.
 */
interface GroupApiService {

    /**
     * Creates a new placeholder post to emulate group creation.
     */
    @POST("posts")
    suspend fun createGroup(@Body request: JsonPlaceholderPostRequest): Response<PostResponse>

    /**
     * Returns placeholder posts that the demo uses as nearby groups.
     */
    @GET("posts")
    suspend fun getNearbyGroups(): Response<List<PostResponse>>
}

/**
 * Request payload understood by the jsonplaceholder post endpoint.
 */
data class JsonPlaceholderPostRequest(
    val title: String,
    val body: String,
    val userId: Int
)
