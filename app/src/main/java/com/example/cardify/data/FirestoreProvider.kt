package com.example.cardify.data

import com.example.cardify.data.model.Group
import com.example.cardify.data.model.Member
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object FirestoreProvider {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val groupsCollection: Query
        get() = firestore.collection("groups").orderBy("createdAt", Query.Direction.DESCENDING)

    fun observeGroups(
        onSuccess: (List<Group>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return groupsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            val groups = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Group::class.java)?.copy(id = doc.id)
            }.orEmpty()
            onSuccess(groups)
        }
    }

    fun observeGroup(
        groupId: String,
        onSuccess: (Group?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        val groupRef = firestore.collection("groups").document(groupId)
        return groupRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            val group = snapshot?.toObject(Group::class.java)?.copy(id = snapshot.id)
            onSuccess(group)
        }
    }

    fun observeMembership(
        groupId: String,
        userId: String,
        onSuccess: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        val memberRef = firestore.collection("groups").document(groupId)
            .collection("members").document(userId)
        return memberRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            onSuccess(snapshot?.exists() == true)
        }
    }

    fun createGroup(
        group: Group,
        member: Member,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val docRef = firestore.collection("groups").document()
        val groupData = group.copy(id = docRef.id, createdAt = System.currentTimeMillis())
        val batch = firestore.batch()
        batch.set(docRef, groupData)
        batch.set(memberDocument(docRef, member.userId), member)
        batch.commit()
            .addOnSuccessListener { onSuccess(docRef.id) }
            .addOnFailureListener { exception -> onError(exception) }
    }

    fun joinGroup(
        groupId: String,
        member: Member,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ): Task<Void> {
        val groupRef = firestore.collection("groups").document(groupId)
        val memberRef = memberDocument(groupRef, member.userId)
        return firestore.runTransaction { transaction ->
            val groupSnapshot = transaction.get(groupRef)
            val group = groupSnapshot.toObject(Group::class.java)
                ?: throw FirebaseFirestoreException(
                    "Group not found",
                    FirebaseFirestoreException.Code.NOT_FOUND
                )
            if (transaction.get(memberRef).exists()) {
                return@runTransaction null
            }
            if (group.currentPeople >= group.maxPeople) {
                throw FirebaseFirestoreException(
                    "Group is full",
                    FirebaseFirestoreException.Code.ABORTED
                )
            }
            transaction.set(memberRef, member)
            transaction.update(groupRef, "currentPeople", group.currentPeople + 1)
            null
        }.addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onError(exception) }
    }

    fun leaveGroup(
        groupId: String,
        userId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ): Task<Void> {
        val groupRef = firestore.collection("groups").document(groupId)
        val memberRef = memberDocument(groupRef, userId)
        return firestore.runTransaction { transaction ->
            val groupSnapshot = transaction.get(groupRef)
            val group = groupSnapshot.toObject(Group::class.java)
                ?: throw FirebaseFirestoreException(
                    "Group not found",
                    FirebaseFirestoreException.Code.NOT_FOUND
                )
            val memberSnapshot = transaction.get(memberRef)
            if (!memberSnapshot.exists()) {
                return@runTransaction null
            }
            val newCount = (group.currentPeople - 1).coerceAtLeast(0)
            transaction.delete(memberRef)
            transaction.update(groupRef, "currentPeople", newCount)
            null
        }.addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onError(exception) }
    }

    private fun memberDocument(groupRef: DocumentReference, userId: String) =
        groupRef.collection("members").document(userId)
}
