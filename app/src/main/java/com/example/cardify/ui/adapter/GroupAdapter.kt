package com.example.cardify.ui.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cardify.R
import com.example.cardify.data.Group
import com.example.cardify.databinding.ItemGroupBinding

/**
 * RecyclerView adapter that displays groups and highlights shared tags.
 */
class GroupAdapter(
    private val onItemClick: (Group) -> Unit
) : ListAdapter<Group, GroupAdapter.GroupViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemGroupBinding.inflate(inflater, parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GroupViewHolder(
        private val binding: ItemGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(group: Group) {
            binding.root.setOnClickListener { onItemClick(group) }

            binding.groupNameText.text = group.name
            binding.groupNameText.setTypeface(
                null,
                if (group.sharedTagsCount > 0) Typeface.BOLD else Typeface.NORMAL
            )

            val context = binding.root.context
            binding.groupTagsText.text = context.getString(
                R.string.group_tags_label,
                group.tags.joinToString(separator = ", ")
            )
            binding.groupSharedTagsText.text = context.getString(
                R.string.group_shared_tags_label,
                group.sharedTagsCount
            )
            binding.groupDistanceText.text = formatDistance(group.distanceMeters)
        }

        private fun formatDistance(distanceMeters: Double): String {
            val context = binding.root.context
            return if (distanceMeters >= 1_000) {
                context.getString(
                    R.string.group_distance_km,
                    distanceMeters / 1_000.0
                )
            } else {
                context.getString(
                    R.string.group_distance_m,
                    distanceMeters
                )
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Group>() {
            override fun areItemsTheSame(oldItem: Group, newItem: Group): Boolean =
                oldItem.name == newItem.name

            override fun areContentsTheSame(oldItem: Group, newItem: Group): Boolean =
                oldItem == newItem
        }
    }
}
