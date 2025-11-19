package com.example.cardify.data

import android.content.Context
import android.util.Log
import com.example.cardify.UserSession
import com.example.cardify.data.local.CardifyDatabase
import com.example.cardify.data.local.GroupDao
import com.example.cardify.data.local.GroupEntity
import com.example.cardify.data.local.MembershipEntity
import com.example.cardify.data.model.Group
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

object GroupRepository {
    private const val TAG = "GroupRepository"
    private const val SEED_FILE = "seed_groups.json"

    private lateinit var database: CardifyDatabase
    private lateinit var groupDao: GroupDao
    private lateinit var firestore: FirebaseFirestore
    private var groupsListener: ListenerRegistration? = null
    private val seedLoaded = AtomicBoolean(false)
    private val remoteListenerStarted = AtomicBoolean(false)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize(context: Context) {
        if (!::database.isInitialized) {
            database = CardifyDatabase.getInstance(context)
            groupDao = database.groupDao()
        }
        if (!::firestore.isInitialized) {
            firestore = FirebaseFirestore.getInstance()
        }
        startRemoteListener()
        scope.launch { ensureSeedData(context) }
    }

    private fun startRemoteListener() {
        if (!remoteListenerStarted.compareAndSet(false, true)) return
        groupsListener?.remove()
        groupsListener = groupsCollection
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Remote listener failed", error)
                    return@addSnapshotListener
                }
                val remoteGroups = snapshot?.documents?.mapNotNull { it.toGroup() } ?: return@addSnapshotListener
                scope.launch {
                    storeGroups(remoteGroups)
                    cleanupMissingGroups(remoteGroups.map { it.id }.toSet())
                }
            }
    }

    private suspend fun cleanupMissingGroups(remoteIds: Set<String>) {
        val localIds = groupDao.allGroupIds()
        val toDelete = localIds.filterNot { remoteIds.contains(it) }
        toDelete.forEach { id ->
            groupDao.deleteMembershipsForGroup(id)
            groupDao.deleteGroup(id)
        }
    }

    private suspend fun ensureSeedData(context: Context) {
        if (seedLoaded.get()) return
        val seeds = runCatching { loadSeedGroups(context) }
            .onFailure { Log.w(TAG, "Failed to load seed groups", it) }
            .getOrElse { emptyList() }
        if (seeds.isNotEmpty()) {
            storeGroups(seeds)
            pushSeedsToRemoteIfNeeded(seeds)
        }
        seedLoaded.set(true)
    }

    private suspend fun pushSeedsToRemoteIfNeeded(seeds: List<Group>) {
        if (seeds.isEmpty()) return
        val remoteCount = runCatching { groupsCollection.limit(1).get().await().size() }
            .getOrDefault(0)
        if (remoteCount > 0) return
        seeds.forEach { group ->
            runCatching {
                groupsCollection.document(group.id).set(group.toRemoteMap()).await()
            }.onFailure { Log.w(TAG, "Failed to seed remote group ${group.id}", it) }
        }
    }

    fun observeGroups(): Flow<List<Group>> {
        return groupDao.observeGroups().map { entities ->
            entities.map { it.toModel() }
        }
    }

    fun observeGroup(groupId: String): Flow<Group?> {
        return groupDao.observeGroup(groupId).map { entity -> entity?.toModel() }
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
    ): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val groupId = UUID.randomUUID().toString()
        val memberIds = if (leaderId.isNotBlank()) listOf(leaderId) else emptyList()
        val displayNames = if (leaderId.isNotBlank()) mapOf(leaderId to leaderName) else emptyMap()
        val group = Group(
            id = groupId,
            title = title,
            description = description,
            date = date,
            leaderId = leaderId,
            leaderName = leaderName,
            members = memberIds,
            tags = tags,
            latitude = latitude,
            longitude = longitude,
            createdAt = now,
            memberDisplayNames = displayNames
        )
        groupsCollection.document(groupId).set(group.toRemoteMap()).await()
        storeGroup(group)
        group.id
    }

    suspend fun updateGroup(group: Group) {
        withContext(Dispatchers.IO) {
            groupsCollection.document(group.id).set(group.toRemoteMap(), SetOptions.merge()).await()
            storeGroup(group)
        }
    }

    suspend fun deleteGroup(groupId: String) {
        withContext(Dispatchers.IO) {
            runCatching { groupsCollection.document(groupId).delete().await() }
                .onFailure { Log.w(TAG, "Failed to delete remote group", it) }
            groupDao.deleteMembershipsForGroup(groupId)
            groupDao.deleteGroup(groupId)
        }
    }

    suspend fun joinGroup(groupId: String, userId: String, displayName: String) {
        withContext(Dispatchers.IO) {
            val updates = hashMapOf<String, Any>(
                "members" to FieldValue.arrayUnion(userId)
            )
            updates["memberDisplayNames.$userId"] = displayName
            runCatching { groupsCollection.document(groupId).update(updates).await() }
                .onFailure { Log.w(TAG, "Failed to update remote membership", it) }
            val current = groupDao.observeGroup(groupId)
                .map { it?.toModel() }
                .firstOrNull()
            val updated = current?.let { group ->
                val memberIds = if (group.members.contains(userId)) group.members else group.members + userId
                group.copy(
                    members = memberIds,
                    memberDisplayNames = group.memberDisplayNames + (userId to displayName)
                )
            }
            if (updated != null) {
                storeGroup(updated)
            }
        }
    }

    fun syncUserProfile(name: String, tag: String) {
        if (!::firestore.isInitialized) return
        val userId = UserSession.userId
        val profile = mapOf(
            "userId" to userId,
            "name" to name,
            "tag" to tag,
            "displayName" to if (tag.isBlank()) name else "$name ○ #$tag",
            "updatedAt" to System.currentTimeMillis()
        )
        scope.launch {
            runCatching {
                usersCollection.document(userId).set(profile, SetOptions.merge()).await()
            }.onFailure { Log.w(TAG, "Failed to sync user profile", it) }
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
        val memberNamesObject = optJSONObject("memberDisplayNames")
        val memberNames = mutableMapOf<String, String>()
        memberNamesObject?.let { obj ->
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                memberNames[key] = obj.optString(key)
            }
        }
        val leaderId = optString("leaderId")
        val leaderName = optString("leaderName")
        if (memberNames.isEmpty() && leaderId.isNotBlank() && leaderName.isNotBlank()) {
            memberNames[leaderId] = leaderName
        }
        return Group(
            id = id,
            title = optString("title"),
            description = optString("description"),
            date = optString("date"),
            leaderId = leaderId,
            leaderName = leaderName,
            members = if (members.isEmpty()) listOf(UserSession.userId) else members,
            tags = tags,
            latitude = optDouble("latitude", 0.0),
            longitude = optDouble("longitude", 0.0),
            createdAt = optLong("createdAt", System.currentTimeMillis()),
            memberDisplayNames = memberNames
        )
    }

    private fun DocumentSnapshot.toGroup(): Group? {
        val id = id
        val title = getString("title") ?: return null
        val description = getString("description").orEmpty()
        val date = getString("date").orEmpty()
        val leaderId = getString("leaderId").orEmpty()
        val leaderName = getString("leaderName").orEmpty()
        val members = (get("members") as? List<*>)
            ?.mapNotNull { it as? String }
            ?: emptyList()
        val tags = (get("tags") as? List<*>)
            ?.mapNotNull { it as? String }
            ?: emptyList()
        val memberNames = (get("memberDisplayNames") as? Map<*, *>)
            ?.mapNotNull { (key, value) ->
                val k = key as? String ?: return@mapNotNull null
                val v = value as? String ?: return@mapNotNull null
                k to v
            }
            ?.toMap()
            ?: emptyMap()
        val createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        val latitude = getDouble("latitude") ?: 0.0
        val longitude = getDouble("longitude") ?: 0.0
        return Group(
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
            createdAt = createdAt,
            memberDisplayNames = memberNames
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
        createdAt = createdAt,
        memberDisplayNames = memberDisplayNames
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
        createdAt = createdAt,
        memberDisplayNames = memberDisplayNames
    )

    private fun Group.toRemoteMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "date" to date,
        "leaderId" to leaderId,
        "leaderName" to leaderName,
        "members" to members,
        "tags" to tags,
        "latitude" to latitude,
        "longitude" to longitude,
        "createdAt" to createdAt,
        "memberDisplayNames" to memberDisplayNames
    )

    private val groupsCollection get() = firestore.collection("groups")
    private val usersCollection get() = firestore.collection("users")
}
