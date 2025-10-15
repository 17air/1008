package com.example.cardify.api

import com.example.cardify.data.CreateGroupRequest
import com.example.cardify.data.Group
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit definition for group-related endpoints.
 */
interface GroupApiService {

    /**
     * Creates a new group with the provided payload.
     */
    @POST("groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): Response<Group>

    /**
     * Returns nearby groups around the provided coordinate.
     */
    @GET("groups/nearby")
    suspend fun getNearbyGroups(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double
    ): Response<List<Group>>
}
