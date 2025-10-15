package com.example.cardify.api

import com.example.cardify.data.CreateGroupRequest
import com.example.cardify.data.PostResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Handles group-related network operations and wraps Retrofit responses.
 */
class GroupRepository(
    private val service: GroupApiService = RetrofitClient.groupApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun createGroup(request: CreateGroupRequest): Result<PostResponse> = withContext(ioDispatcher) {
        runCatching {
            val payload = JsonPlaceholderPostRequest(
                title = request.title,
                body = buildString {
                    appendLine(request.description)
                    appendLine("일시: ${'$'}{request.dateTime}")
                    appendLine("장소: ${'$'}{request.location}")
                    append("정원: ${'$'}{request.maxPeople}명")
                }.trimEnd(),
                userId = 1
            )

            val response = service.createGroup(payload)
            if (response.isSuccessful) {
                response.body() ?: throw IllegalStateException("Empty response body")
            } else {
                throw IOException("Create group failed: ${'$'}{response.code()}")
            }
        }
    }

    suspend fun loadNearbyGroups(latitude: Double, longitude: Double): List<PostResponse> =
        withContext(ioDispatcher) {
            val response = service.getNearbyGroups()
            if (response.isSuccessful) {
                response.body().orEmpty()
            } else {
                throw IOException("Fetch groups failed: ${'$'}{response.code()}")
            }
        }
}
