package com.example.cardify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    fun observeGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    fun observeGroup(groupId: String): Flow<GroupEntity?>

    @Query("SELECT COUNT(*) FROM groups")
    suspend fun countGroups(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroups(groups: List<GroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: GroupEntity)

    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("SELECT id FROM groups")
    suspend fun allGroupIds(): List<String>

    @Query("DELETE FROM memberships WHERE groupId = :groupId")
    suspend fun deleteMembershipsForGroup(groupId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemberships(memberships: List<MembershipEntity>)

    @Query("SELECT * FROM memberships WHERE userId = :userId")
    fun observeMembershipsForUser(userId: String): Flow<List<MembershipEntity>>
}
