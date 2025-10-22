package com.example.cardify.data

import com.example.cardify.data.model.Group
import com.example.cardify.data.model.Member
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object LocalGroupRepository {
    interface ListenerRegistration {
        fun remove()
    }

    class GroupFullException : IllegalStateException("Group is full")
    class GroupNotFoundException : IllegalStateException("Group not found")

    private val groups = mutableListOf<Group>()
    private val members = mutableMapOf<String, MutableList<Member>>()

    private val groupListeners = LinkedHashMap<ListenerRegistrationImpl, (List<Group>) -> Unit>()
    private val detailListeners = mutableMapOf<String, LinkedHashMap<ListenerRegistrationImpl, (Group?) -> Unit>>()
    private val membershipListeners = ConcurrentHashMap<Pair<String, String>, LinkedHashMap<ListenerRegistrationImpl, (Boolean) -> Unit>>()

    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true
        seedDefaultGroups()
    }

    fun observeGroups(
        onSuccess: (List<Group>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        initialize()
        val registration = ListenerRegistrationImpl { reg ->
            groupListeners.remove(reg)
        }
        groupListeners[registration] = onSuccess
        onSuccess(currentGroups())
        return registration
    }

    fun observeGroup(
        groupId: String,
        onSuccess: (Group?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        initialize()
        val listeners = detailListeners.getOrPut(groupId) { LinkedHashMap() }
        val registration = ListenerRegistrationImpl { reg ->
            listeners.remove(reg)
            if (listeners.isEmpty()) {
                detailListeners.remove(groupId)
            }
        }
        listeners[registration] = onSuccess
        onSuccess(findGroup(groupId))
        return registration
    }

    fun observeMembership(
        groupId: String,
        userId: String,
        onSuccess: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        initialize()
        val key = groupId to userId
        val listeners = membershipListeners.getOrPut(key) { LinkedHashMap() }
        val registration = ListenerRegistrationImpl { reg ->
            val map = membershipListeners[key]
            map?.remove(reg)
            if (map.isNullOrEmpty()) {
                membershipListeners.remove(key)
            }
        }
        listeners[registration] = onSuccess
        onSuccess(isMember(groupId, userId))
        return registration
    }

    fun createGroup(
        group: Group,
        ownerMember: Member,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            initialize()
            val newId = UUID.randomUUID().toString()
            val sanitizedMembers = mutableListOf(ownerMember)
            val newGroup = group.copy(
                id = newId,
                ownerId = ownerMember.userId,
                ownerName = ownerMember.name,
                currentPeople = sanitizedMembers.size,
                createdAt = System.currentTimeMillis()
            )
            groups.add(0, newGroup)
            groups.sortByDescending { it.createdAt }
            members[newId] = sanitizedMembers
            notifyGroupList()
            notifyGroup(newId)
            notifyMembership(newId, ownerMember.userId)
            onSuccess(newId)
        } catch (exception: Exception) {
            onError(exception)
        }
    }

    fun joinGroup(
        groupId: String,
        member: Member,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            initialize()
            val index = groups.indexOfFirst { it.id == groupId }
            if (index == -1) throw GroupNotFoundException()
            val group = groups[index]
            val memberList = members.getOrPut(groupId) { mutableListOf() }
            if (memberList.any { it.userId == member.userId }) {
                onSuccess()
                return
            }
            if (group.currentPeople >= group.maxPeople) {
                throw GroupFullException()
            }
            memberList.add(member)
            val updatedGroup = group.copy(currentPeople = memberList.size)
            groups[index] = updatedGroup
            notifyGroupList()
            notifyGroup(groupId)
            notifyMembership(groupId, member.userId)
            onSuccess()
        } catch (exception: Exception) {
            onError(exception)
        }
    }

    fun leaveGroup(
        groupId: String,
        userId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            initialize()
            val index = groups.indexOfFirst { it.id == groupId }
            if (index == -1) throw GroupNotFoundException()
            val group = groups[index]
            val memberList = members.getOrPut(groupId) { mutableListOf() }
            val removed = memberList.removeAll { it.userId == userId }
            if (removed) {
                val updatedGroup = group.copy(currentPeople = memberList.size)
                groups[index] = updatedGroup
                notifyGroupList()
                notifyGroup(groupId)
                if (memberList.isEmpty()) {
                    members.remove(groupId)
                }
            }
            notifyMembership(groupId, userId)
            onSuccess()
        } catch (exception: Exception) {
            onError(exception)
        }
    }

    private fun currentGroups(): List<Group> = groups.map { it.copy() }

    private fun findGroup(groupId: String): Group? =
        groups.firstOrNull { it.id == groupId }?.copy()

    private fun isMember(groupId: String, userId: String): Boolean =
        members[groupId]?.any { it.userId == userId } == true

    private fun notifyGroupList() {
        val snapshot = currentGroups()
        groupListeners.values.forEach { listener -> listener(snapshot) }
    }

    private fun notifyGroup(groupId: String) {
        val snapshot = findGroup(groupId)
        detailListeners[groupId]?.values?.forEach { listener -> listener(snapshot) }
    }

    private fun notifyMembership(groupId: String, userId: String) {
        val key = groupId to userId
        val isMember = isMember(groupId, userId)
        membershipListeners[key]?.values?.forEach { listener -> listener(isMember) }
    }

    private fun seedDefaultGroups() {
        if (groups.isNotEmpty()) return
        val now = System.currentTimeMillis()
        addSeedGroup(
            id = UUID.randomUUID().toString(),
            title = "한강 러닝 소모임",
            description = "주말마다 함께 달리며 건강을 챙겨요.",
            tags = listOf("러닝", "운동"),
            latitude = 37.52852,
            longitude = 126.9326,
            maxPeople = 12,
            ownerId = "owner_runner",
            ownerName = "민수",
            createdAt = now - 3_600_000L,
            extraMembers = listOf(
                Member(userId = "runner_friend", name = "유리")
            )
        )
        addSeedGroup(
            id = UUID.randomUUID().toString(),
            title = "홍대 보드게임 모임",
            description = "신작부터 클래식까지 다양한 보드게임을 즐겨요.",
            tags = listOf("보드게임", "친목"),
            latitude = 37.5572,
            longitude = 126.9244,
            maxPeople = 8,
            ownerId = "owner_board",
            ownerName = "수진",
            createdAt = now - 7_200_000L,
            extraMembers = listOf(
                Member(userId = "board_fan", name = "현우"),
                Member(userId = "board_new", name = "지영")
            )
        )
        addSeedGroup(
            id = UUID.randomUUID().toString(),
            title = "강남 코딩 스터디",
            description = "실전 안드로이드 앱을 함께 만들어봅니다.",
            tags = listOf("코딩", "스터디"),
            latitude = 37.4981,
            longitude = 127.0276,
            maxPeople = 15,
            ownerId = "owner_code",
            ownerName = "지훈",
            createdAt = now - 10_800_000L,
            extraMembers = emptyList()
        )
        groups.sortByDescending { it.createdAt }
    }

    private fun addSeedGroup(
        id: String,
        title: String,
        description: String,
        tags: List<String>,
        latitude: Double,
        longitude: Double,
        maxPeople: Int,
        ownerId: String,
        ownerName: String,
        createdAt: Long,
        extraMembers: List<Member>
    ) {
        val ownerMember = Member(userId = ownerId, name = ownerName)
        val participantList = mutableListOf(ownerMember)
        participantList.addAll(extraMembers)
        val group = Group(
            id = id,
            title = title,
            description = description,
            tags = tags,
            latitude = latitude,
            longitude = longitude,
            maxPeople = maxPeople,
            currentPeople = participantList.size,
            ownerId = ownerId,
            ownerName = ownerName,
            createdAt = createdAt
        )
        groups.add(group)
        members[id] = participantList
    }

    private class ListenerRegistrationImpl(
        private val onRemove: (ListenerRegistrationImpl) -> Unit
    ) : ListenerRegistration {
        override fun remove() {
            onRemove(this)
        }
    }
}
