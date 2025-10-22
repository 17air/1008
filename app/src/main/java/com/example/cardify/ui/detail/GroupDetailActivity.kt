package com.example.cardify.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.cardify.R
import com.example.cardify.UserSession
import com.example.cardify.data.LocalGroupRepository
import com.example.cardify.data.LocalGroupRepository.GroupFullException
import com.example.cardify.data.LocalGroupRepository.ListenerRegistration
import com.example.cardify.data.model.Group
import com.example.cardify.data.model.Member
import com.example.cardify.databinding.ActivityGroupDetailBinding

class GroupDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupDetailBinding
    private var groupId: String = ""
    private var groupListener: ListenerRegistration? = null
    private var membershipListener: ListenerRegistration? = null
    private var isMember: Boolean = false
    private var currentGroup: Group? = null
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getStringExtra(EXTRA_GROUP_ID).orEmpty()
        if (groupId.isBlank()) {
            Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.joinLeaveButton.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            if (isMember) {
                leaveGroup()
            } else {
                joinGroup()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        subscribeToGroup()
        subscribeToMembership()
    }

    override fun onStop() {
        super.onStop()
        groupListener?.remove()
        membershipListener?.remove()
    }

    private fun subscribeToGroup() {
        groupListener?.remove()
        groupListener = LocalGroupRepository.observeGroup(
            groupId,
            onSuccess = { group ->
                runOnUiThread {
                    currentGroup = group
                    if (group == null) {
                        Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_LONG).show()
                        finish()
                        return@runOnUiThread
                    }
                    binding.titleText.text = group.title
                    binding.descriptionText.text = group.description
                    val ownerName = group.ownerName.ifBlank { getString(R.string.unknown_owner) }
                    binding.ownerText.text = getString(R.string.owner_prefix, ownerName)
                    binding.participantsText.text = getString(
                        R.string.participants_format,
                        group.currentPeople,
                        group.maxPeople
                    )
                    val formattedTags = group.tags.mapNotNull { tag ->
                        val trimmed = tag.trim()
                        trimmed.takeIf { it.isNotEmpty() }?.let { "#$it" }
                    }
                    binding.tagsText.isVisible = formattedTags.isNotEmpty()
                    binding.tagsText.text = if (formattedTags.isNotEmpty()) {
                        getString(R.string.tags_prefix, formattedTags.joinToString(separator = "  "))
                    } else {
                        ""
                    }
                    updateJoinLeaveButton()
                }
            },
            onError = {
                runOnUiThread {
                    Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun subscribeToMembership() {
        val userId = UserSession.userId
        if (userId.isBlank()) {
            UserSession.initialize { success ->
                if (success) {
                    subscribeToMembership()
                } else {
                    runOnUiThread {
                        Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_LONG).show()
                    }
                }
            }
            return
        }
        membershipListener?.remove()
        membershipListener = LocalGroupRepository.observeMembership(
            groupId,
            userId,
            onSuccess = { joined ->
                runOnUiThread {
                    isMember = joined
                    updateJoinLeaveButton()
                }
            },
            onError = {
                runOnUiThread {
                    Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun joinGroup() {
        val userId = UserSession.userId
        if (userId.isBlank()) {
            Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_LONG).show()
            return
        }
        val group = currentGroup ?: return
        if (group.currentPeople >= group.maxPeople) {
            Toast.makeText(this, R.string.group_full, Toast.LENGTH_LONG).show()
            return
        }
        setProcessing(true)
        val member = Member(userId = userId, name = UserSession.userName)
        LocalGroupRepository.joinGroup(
            groupId,
            member,
            onSuccess = {
                runOnUiThread {
                    setProcessing(false)
                    Toast.makeText(this, R.string.group_joined, Toast.LENGTH_SHORT).show()
                }
            },
            onError = { exception ->
                runOnUiThread {
                    setProcessing(false)
                    Toast.makeText(this, mapJoinError(exception), Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun leaveGroup() {
        val userId = UserSession.userId
        if (userId.isBlank()) {
            Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_LONG).show()
            return
        }
        setProcessing(true)
        LocalGroupRepository.leaveGroup(
            groupId,
            userId,
            onSuccess = {
                runOnUiThread {
                    setProcessing(false)
                    Toast.makeText(this, R.string.group_left, Toast.LENGTH_SHORT).show()
                }
            },
            onError = {
                runOnUiThread {
                    setProcessing(false)
                    Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun mapJoinError(exception: Exception): Int {
        val message = exception.message.orEmpty()
        return when {
            exception is GroupFullException -> R.string.group_full
            message.contains("full", ignoreCase = true) -> R.string.group_full
            else -> R.string.unknown_error
        }
    }

    private fun updateJoinLeaveButton() {
        val group = currentGroup ?: return
        val userId = UserSession.userId
        val isOwner = userId == group.ownerId
        val enabled = isMember || group.currentPeople < group.maxPeople || isOwner
        binding.joinLeaveButton.isEnabled = enabled && !isProcessing
        binding.joinLeaveButton.text = if (isMember) {
            getString(R.string.leave_group)
        } else {
            getString(R.string.join_group)
        }
        binding.joinLeaveButton.isVisible = !isOwner
    }

    private fun setProcessing(processing: Boolean) {
        isProcessing = processing
        binding.actionProgress.isVisible = processing && binding.joinLeaveButton.isVisible
        updateJoinLeaveButton()
    }

    companion object {
        private const val EXTRA_GROUP_ID = "extra_group_id"

        fun createIntent(context: Context, groupId: String): Intent {
            return Intent(context, GroupDetailActivity::class.java).apply {
                putExtra(EXTRA_GROUP_ID, groupId)
            }
        }
    }
}
