package com.example.cardify.data

import android.util.Log
import com.example.cardify.UserSession
import com.example.cardify.data.model.ChatMessage
import com.example.cardify.data.model.Group
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

object GroupRepository {
    private const val TAG = "GroupRepository"

    private val fallbackGroups = MutableStateFlow<List<Group>>(emptyList())
    private val fallbackChats = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()

    private var firestore: FirebaseFirestore? = null

    fun initialize() {
        if (firestore == null) {
            firestore = runCatching { FirebaseFirestore.getInstance() }
                .onFailure { Log.w(TAG, "Firestore not available, using in-memory store", it) }
                .getOrNull()
        }
        if (fallbackGroups.value.isEmpty()) {
            fallbackGroups.value = seedGroups()
        }
    }

    fun observeGroups(): Flow<List<Group>> {
        val db = firestore ?: return fallbackGroups
        return callbackFlow {
            val registration: ListenerRegistration = db.collection("groups")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Failed to listen to groups", error)
                        trySend(fallbackGroups.value)
                        return@addSnapshotListener
                    }
                    if (snapshot == null) {
                        trySend(fallbackGroups.value)
                        return@addSnapshotListener
                    }
                    val groups = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(GroupDocument::class.java)?.toGroup(doc.id)
                    }.sortedByDescending { it.createdAt }
                    trySend(groups)
                }
            awaitClose { registration.remove() }
        }
    }

    suspend fun createOrUpdateUser(userId: String, name: String) {
        val db = firestore ?: return
        runCatching {
            db.collection("users")
                .document(userId)
                .set(mapOf("userId" to userId, "name" to name))
                .await()
        }.onFailure { Log.w(TAG, "Failed to store user", it) }
    }

    suspend fun createGroup(
        title: String,
        description: String,
        date: String,
        tags: List<String>,
        leaderId: String,
        leaderName: String,
        latitude: Double,
        longitude: Double
    ): String {
        val now = System.currentTimeMillis()
        val members = listOf(leaderId)
        val db = firestore
        if (db == null) {
            val id = UUID.randomUUID().toString()
            val group = Group(
                id = id,
                title = title,
                description = description,
                date = date,
                leaderId = leaderId,
                leaderName = leaderName,
                tags = tags,
                members = members,
                latitude = latitude,
                longitude = longitude,
                createdAt = now
            )
            fallbackGroups.value = (fallbackGroups.value + group)
                .sortedByDescending { it.createdAt }
            return id
        }

        val doc = db.collection("groups").document()
        val payload = mapOf(
            "title" to title,
            "description" to description,
            "date" to date,
            "leaderId" to leaderId,
            "leaderName" to leaderName,
            "tags" to tags,
            "members" to members,
            "latitude" to latitude,
            "longitude" to longitude,
            "createdAt" to now
        )
        doc.set(payload).await()
        return doc.id
    }

    suspend fun joinGroup(groupId: String, userId: String) {
        val db = firestore
        if (db == null) {
            val updated = fallbackGroups.value.map { group ->
                if (group.id == groupId && !group.members.contains(userId)) {
                    group.copy(members = group.members + userId)
                } else {
                    group
                }
            }
            fallbackGroups.value = updated
            return
        }
        runCatching {
            db.collection("groups")
                .document(groupId)
                .update("members", FieldValue.arrayUnion(userId))
                .await()
        }.onFailure { error ->
            Log.w(TAG, "Failed to join group", error)
        }
    }

    fun observeChatMessages(groupId: String): Flow<List<ChatMessage>> {
        val db = firestore
        if (db == null) {
            return fallbackChats.getOrPut(groupId) { MutableStateFlow(emptyList()) }
        }
        return callbackFlow {
            val registration = db.collection("chats")
                .document(groupId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Failed to listen to chat $groupId", error)
                        trySend(fallbackChats[groupId]?.value ?: emptyList())
                        return@addSnapshotListener
                    }
                    val messages = snapshot?.documents?.map { doc ->
                        ChatMessage.fromMap(doc.id, doc.data ?: emptyMap())
                    } ?: emptyList()
                    trySend(messages)
                }
            awaitClose { registration.remove() }
        }
    }

    suspend fun sendMessage(groupId: String, message: ChatMessage) {
        val db = firestore
        if (db == null) {
            val flow = fallbackChats.getOrPut(groupId) { MutableStateFlow(emptyList()) }
            flow.value = flow.value + message.copy(id = UUID.randomUUID().toString())
            return
        }
        val payload = message.toMap()
        db.collection("chats")
            .document(groupId)
            .collection("messages")
            .add(payload)
            .await()
    }

    private fun seedGroups(): List<Group> {
        val userId = UserSession.userId.ifEmpty { UUID.randomUUID().toString() }
        val defaultGroups = listOf(
            Group(
                id = UUID.randomUUID().toString(),
                title = "아침 러닝 소모임",
                description = "주말 아침에 한강에서 러닝 함께해요",
                date = "매주 토요일 08:00",
                leaderId = userId,
                leaderName = UserSession.userName.ifEmpty { "게스트" },
                tags = listOf("운동", "러닝"),
                members = listOf(userId),
                latitude = 37.5511694,
                longitude = 126.9882266,
                createdAt = System.currentTimeMillis()
            ),
            Group(
                id = UUID.randomUUID().toString(),
                title = "주말 여행 계획",
                description = "근교 여행 같이 떠나실 분 모집합니다.",
                date = "2024-08-17",
                leaderId = "travel_leader",
                leaderName = "여행러",
                tags = listOf("여행", "사진", "맛집"),
                members = listOf("travel_leader"),
                latitude = 37.579617,
                longitude = 126.977041,
                createdAt = System.currentTimeMillis() - 3_600_000L
            ),
            Group(
                id = UUID.randomUUID().toString(),
                title = "카페 코딩 모임",
                description = "토요일 오후에 만나서 각자 프로젝트 진행해요",
                date = "매주 토요일 14:00",
                leaderId = "dev_master",
                leaderName = "개발자A",
                tags = listOf("코딩", "스터디"),
                members = listOf("dev_master"),
                latitude = 37.5662952,
                longitude = 126.9779451,
                createdAt = System.currentTimeMillis() - 7_200_000L
            )
        )
        return defaultGroups
    }

    private data class GroupDocument(
        val title: String? = null,
        val description: String? = null,
        val date: String? = null,
        val leaderId: String? = null,
        val leaderName: String? = null,
        val tags: List<String>? = null,
        val members: List<String>? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val createdAt: Long? = null
    ) {
        fun toGroup(id: String): Group = Group(
            id = id,
            title = title.orEmpty(),
            description = description.orEmpty(),
            date = date.orEmpty(),
            leaderId = leaderId.orEmpty(),
            leaderName = leaderName.orEmpty(),
            tags = tags ?: emptyList(),
            members = members ?: emptyList(),
            latitude = latitude ?: 0.0,
            longitude = longitude ?: 0.0,
            createdAt = createdAt ?: System.currentTimeMillis()
        )
    }

    fun observeGroup(groupId: String): Flow<Group?> {
        val db = firestore
        if (db == null) {
            return fallbackGroups.map { groups -> groups.firstOrNull { it.id == groupId } }
        }

        return callbackFlow {
            val registration = db.collection("groups")
                .document(groupId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Failed to listen to group $groupId", error)
                        trySend(fallbackGroups.value.firstOrNull { it.id == groupId })
                        return@addSnapshotListener
                    }
                    val group = snapshot?.takeIf { it.exists() }?.let { doc ->
                        doc.toObject(GroupDocument::class.java)?.toGroup(doc.id)
                    }
                    trySend(group)
                }
            awaitClose { registration.remove() }
        }
    }
}
