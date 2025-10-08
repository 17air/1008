package com.example.cardify.ui

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cardify.data.Group
import com.example.cardify.databinding.ItemGroupBinding
import java.util.Locale

/**
 * RecyclerView adapter that renders each group card.
 */
class GroupAdapter(
    private val onItemClicked: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    private val groups = mutableListOf<Group>()

    fun submitList(newGroups: List<Group>) {
        groups.clear()
        groups.addAll(newGroups)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding, onItemClicked)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount(): Int = groups.size

    class GroupViewHolder(
        private val binding: ItemGroupBinding,
        private val onItemClicked: (Group) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(group: Group) {
            binding.groupName.text = group.name
            binding.groupTags.text = group.tags.joinToString(separator = ", ")
            binding.groupDistance.text = String.format(
                Locale.getDefault(),
                "%.1f km • %d shared tags",
                group.distanceMeters / 1000.0,
                group.sharedTagsCount
            )

            binding.groupName.setTypeface(
                binding.groupName.typeface,
                if (group.sharedTagsCount > 0) Typeface.BOLD else Typeface.NORMAL
            )

            binding.root.setOnClickListener { onItemClicked(group) }
        }
    }
}
