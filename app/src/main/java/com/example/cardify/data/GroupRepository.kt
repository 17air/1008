package com.example.cardify.data

import android.content.Context
import android.util.Log
import com.example.cardify.UserSession
import com.example.cardify.data.local.CardifyDatabase
import com.example.cardify.data.local.GroupDao
import com.example.cardify.data.local.GroupEntity
import com.example.cardify.data.local.MembershipEntity
import com.example.cardify.data.model.Group
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
    private val seedLoaded = AtomicBoolean(false)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize(context: Context) {
        if (::database.isInitialized) return
        database = CardifyDatabase.getInstance(context)
        groupDao = database.groupDao()
        scope.launch { ensureSeedData(context) }
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
    ): String {
        val now = System.currentTimeMillis()
        val groupId = UUID.randomUUID().toString()
        val group = Group(
            id = groupId,
            title = title,
            description = description,
            date = date,
            leaderId = leaderId,
            leaderName = leaderName,
            members = listOf(leaderId),
            tags = tags,
            latitude = latitude,
            longitude = longitude,
            createdAt = now
        )
        storeGroup(group)
        return group.id
    }

    suspend fun updateGroup(group: Group) {
        withContext(Dispatchers.IO) { storeGroup(group) }
    }

    suspend fun deleteGroup(groupId: String) {
        withContext(Dispatchers.IO) {
            groupDao.deleteMembershipsForGroup(groupId)
            groupDao.deleteGroup(groupId)
        }
    }

    suspend fun joinGroup(groupId: String, userId: String) {
        withContext(Dispatchers.IO) {
            val current = groupDao.observeGroup(groupId)
                .map { it?.toModel() }
                .firstOrNull()
            val updated = current?.let { group ->
                if (group.members.contains(userId)) group else group.copy(members = group.members + userId)
            }
            if (updated != null) {
                storeGroup(updated)
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
}
