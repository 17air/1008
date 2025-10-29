package com.example.cardify.data

import android.content.Context
import android.util.Log
import com.example.cardify.UserSession
import com.example.cardify.data.local.CardifyDatabase
import com.example.cardify.data.local.GroupDao
import com.example.cardify.data.local.GroupEntity
import com.example.cardify.data.local.MembershipEntity
import com.example.cardify.data.model.ChatMessage
import com.example.cardify.data.model.Group
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

object GroupRepository {
    private const val TAG = "GroupRepository"
    private const val SEED_FILE = "seed_groups.json"

    private val fallbackChats = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()

    private lateinit var database: CardifyDatabase
    private lateinit var groupDao: GroupDao
    private val seedLoaded = AtomicBoolean(false)

    private var firestore: FirebaseFirestore? = null
    private var groupListener: ListenerRegistration? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize(context: Context) {
        if (::database.isInitialized) return
        database = CardifyDatabase.getInstance(context)
        groupDao = database.groupDao()
        firestore = runCatching { FirebaseFirestore.getInstance() }
            .onFailure { Log.w(TAG, "Firestore unavailable, running in offline mode", it) }
            .getOrNull()
        scope.launch { ensureSeedData(context) }
        startFirestoreSync()
    }

    private suspend fun ensureSeedData(context: Context) {
        if (seedLoaded.get()) return
        if (groupDao.countGroups() > 0) {
            seedLoaded.set(true)
            return
        }
        val seeds = runCatching { loadSeedGroups(context) }
            .onFailure { Log.w(TAG, "Failed to load seed groups", it) }
            .getOrElse { emptyList() }
        if (seeds.isNotEmpty()) {
            storeGroups(seeds)
        }
        seedLoaded.set(true)
    }

    fun observeGroups(): Flow<List<Group>> {
        return groupDao.observeGroups().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun createOrUpdateUser(userId: String, name: String, tag: String) {
        val db = firestore ?: return
        runCatching {
            db.collection("users")
                .document(userId)
                .set(mapOf("userId" to userId, "name" to name, "tag" to tag), SetOptions.merge())
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
        val groupId = UUID.randomUUID().toString()
        val group = Group(
            id = groupId,
            title = title,
            description = description,
            date = date,
            leaderId = leaderId,
            leaderName = leaderName,
            members = members,
            tags = tags,
            latitude = latitude,
            longitude = longitude,
            createdAt = now
        )

        val db = firestore
        if (db != null) {
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
            val remoteGroup = group.copy(id = doc.id)
            scope.launch { storeGroup(remoteGroup) }
            return doc.id
        }

        scope.launch { storeGroup(group) }
        return group.id
    }

    suspend fun updateGroup(group: Group) {
        val db = firestore
        if (db != null) {
            val payload = mapOf(
                "title" to group.title,
                "description" to group.description,
                "date" to group.date,
                "tags" to group.tags,
                "members" to group.members,
                "latitude" to group.latitude,
                "longitude" to group.longitude,
                "leaderName" to group.leaderName
            )
            runCatching {
                db.collection("groups")
                    .document(group.id)
                    .set(payload, SetOptions.merge())
                    .await()
            }.onFailure { Log.w(TAG, "Failed to update group", it) }
        }
        scope.launch { storeGroup(group) }
    }

    suspend fun deleteGroup(groupId: String) {
        val db = firestore
        if (db != null) {
            runCatching {
                db.collection("groups").document(groupId).delete().await()
            }.onFailure { Log.w(TAG, "Failed to delete group", it) }
        }
        scope.launch {
            groupDao.deleteMembershipsForGroup(groupId)
            groupDao.deleteGroup(groupId)
        }
    }

    suspend fun joinGroup(groupId: String, userId: String) {
        val db = firestore
        if (db != null) {
            runCatching {
                db.collection("groups")
                    .document(groupId)
                    .update("members", FieldValue.arrayUnion(userId))
                    .await()
            }.onFailure { error ->
                Log.w(TAG, "Failed to join group", error)
            }
        }
        scope.launch {
            val current = groupDao.observeGroup(groupId)
                .map { it?.toModel() }
                .firstOrNull()
            val updated = current?.let { group ->
                if (group.members.contains(userId)) group else group.copy(members = group.members + userId)
            }
            updated?.let { storeGroup(it) }
        }
    }

    fun observeGroup(groupId: String): Flow<Group?> {
        return groupDao.observeGroup(groupId).map { entity -> entity?.toModel() }
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
                .orderBy("sentAt")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Failed to listen to chat $groupId", error)
                        trySend(fallbackChats[groupId]?.value ?: emptyList())
                        return@addSnapshotListener
                    }
                    val messages = snapshot?.documents?.mapNotNull { doc ->
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

    private fun startFirestoreSync() {
        val db = firestore ?: return
        groupListener?.remove()
        groupListener = db.collection("groups")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Failed to listen to groups", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                val groups = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(GroupDocument::class.java)?.toGroup(doc.id)
                }
                scope.launch {
                    if (groups.isNotEmpty()) {
                        storeGroups(groups)
                    }
                    val remoteIds = groups.map { it.id }.toSet()
                    val existing = groupDao.allGroupIds()
                    val removable = existing.filter { id -> id !in remoteIds && !id.startsWith("seed-") }
                    removable.forEach { id ->
                        groupDao.deleteMembershipsForGroup(id)
                        groupDao.deleteGroup(id)
                    }
                }
            }
    }

    private suspend fun storeGroups(groups: List<Group>) {
        groups.forEach { storeGroup(it) }
    }

    private suspend fun storeGroup(group: Group) {
        groupDao.upsertGroup(group.toEntity())
        groupDao.deleteMembershipsForGroup(group.id)
        val memberships = group.members.distinct().map { memberId ->
            MembershipEntity(
                groupId = group.id,
                userId = memberId,
                joinedAt = System.currentTimeMillis()
            )
        }
        if (memberships.isNotEmpty()) {
            groupDao.upsertMemberships(memberships)
        }
    }

    private fun loadSeedGroups(context: Context): List<Group> {
        val json = context.assets.open(SEED_FILE).bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        val groups = mutableListOf<Group>()
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            groups.add(obj.toGroup())
        }
        return groups
    }

    private fun JSONObject.toGroup(): Group {
        val id = optString("id").ifEmpty { UUID.randomUUID().toString() }
        val membersArray = optJSONArray("members") ?: JSONArray()
        val members = mutableListOf<String>()
        for (i in 0 until membersArray.length()) {
            members.add(membersArray.optString(i))
        }
        val tagsArray = optJSONArray("tags") ?: JSONArray()
        val tags = mutableListOf<String>()
        for (i in 0 until tagsArray.length()) {
            tags.add(tagsArray.optString(i))
        }
        return Group(
            id = id,
            title = optString("title"),
            description = optString("description"),
            date = optString("date"),
            leaderId = optString("leaderId"),
            leaderName = optString("leaderName"),
            members = if (members.isEmpty()) listOf(UserSession.userId) else members,
            tags = tags,
            latitude = optDouble("latitude", 0.0),
            longitude = optDouble("longitude", 0.0),
            createdAt = optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun GroupEntity.toModel(): Group = Group(
        id = id,
        title = title,
        description = description,
        date = date,
        leaderId = leaderId,
        leaderName = leaderName,
        members = members,
        tags = tags,
        latitude = latitude,
        longitude = longitude,
        createdAt = createdAt
    )

    private fun Group.toEntity(): GroupEntity = GroupEntity(
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
        createdAt = createdAt
    )

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
}
