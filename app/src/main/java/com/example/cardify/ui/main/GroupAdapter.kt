package com.example.cardify.ui.main

import android.location.Location
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cardify.R
import com.example.cardify.data.model.Group
import com.example.cardify.databinding.ItemGroupBinding

class GroupAdapter(
    private val onItemClicked: (Group) -> Unit
) : ListAdapter<Group, GroupAdapter.GroupViewHolder>(DiffCallback) {

    private var userLocation: Location? = null

    fun updateUserLocation(location: Location?) {
        userLocation = location
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GroupViewHolder(
        private val binding: ItemGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(group: Group) {
            binding.groupTitle.text = group.title
            binding.groupDescription.text = group.description
            binding.participantsText.text = binding.root.context.getString(
                R.string.participants_format,
                group.currentPeople,
                group.maxPeople
            )
            binding.distanceText.text = computeDistanceText(group)
            binding.root.setOnClickListener { onItemClicked(group) }
        }

        private fun computeDistanceText(group: Group): String {
            val location = userLocation
                ?: return binding.root.context.getString(R.string.distance_placeholder)
            val results = FloatArray(1)
            Location.distanceBetween(
                location.latitude,
                location.longitude,
                group.latitude,
                group.longitude,
                results
            )
            val meters = results.firstOrNull() ?: return binding.root.context.getString(R.string.distance_placeholder)
            return if (meters >= 1000f) {
                binding.root.context.getString(R.string.distance_km_format, meters / 1000f)
            } else {
                binding.root.context.getString(R.string.distance_m_format, meters)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Group>() {
        override fun areItemsTheSame(oldItem: Group, newItem: Group): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Group, newItem: Group): Boolean = oldItem == newItem
    }
}
