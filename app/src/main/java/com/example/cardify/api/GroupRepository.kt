package com.example.cardify.api

import com.example.cardify.data.CreateGroupRequest
import com.example.cardify.data.Group
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles group-related network operations and wraps Retrofit responses.
 */
class GroupRepository(
    private val service: GroupApiService = ApiClient.groupApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun createGroup(request: CreateGroupRequest): Result<Group> = withContext(ioDispatcher) {
        runCatching {
            val response = service.createGroup(request)
            if (response.isSuccessful) {
                response.body() ?: throw IllegalStateException("Empty response body")
            } else {
                throw IllegalStateException("Create group failed: ${'$'}{response.code()}")
            }
        }
    }

    suspend fun loadNearbyGroups(latitude: Double, longitude: Double): Result<List<Group>> =
        withContext(ioDispatcher) {
            runCatching {
                val response = service.getNearbyGroups(latitude, longitude)
                if (response.isSuccessful) {
                    response.body().orEmpty()
                } else {
                    throw IllegalStateException("Fetch groups failed: ${'$'}{response.code()}")
                }
            }
        }
}
