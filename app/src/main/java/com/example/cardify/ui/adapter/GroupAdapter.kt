package com.example.cardify.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cardify.R
import com.example.cardify.data.Group
import com.example.cardify.databinding.ItemGroupBinding

/**
 * RecyclerView adapter that displays created or fetched groups.
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

            val context = binding.root.context
            binding.groupTitleText.text = group.title
            binding.groupDescriptionText.text = group.description
            binding.groupLocationText.text = group.location
            binding.groupMaxPeopleText.text =
                context.getString(R.string.group_max_people_format, group.maxPeople)

            val distanceText = if (group.distanceMeters >= 1_000) {
                context.getString(R.string.group_distance_km, group.distanceMeters / 1_000.0)
            } else {
                context.getString(R.string.group_distance_m, group.distanceMeters)
            }
            binding.groupDistanceText.text = distanceText
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Group>() {
            override fun areItemsTheSame(oldItem: Group, newItem: Group): Boolean =
                oldItem.title == newItem.title &&
                    oldItem.latitude == newItem.latitude &&
                    oldItem.longitude == newItem.longitude

            override fun areContentsTheSame(oldItem: Group, newItem: Group): Boolean =
                oldItem == newItem
        }
    }
}
