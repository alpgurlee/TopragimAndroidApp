package com.alperengurle.EmlakApp.ui.favorites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alperengurle.EmlakApp.data.model.Listing
import com.alperengurle.EmlakApp.databinding.ItemFavoriteListingBinding
import com.bumptech.glide.Glide

class FavoritesAdapter(
    private val onItemClick: (Listing) -> Unit,
    private val onRemoveClick: (Listing) -> Unit
) : ListAdapter<Listing, FavoritesAdapter.FavoriteViewHolder>(ListingDiffCallback()) {

    inner class FavoriteViewHolder(
        private val binding: ItemFavoriteListingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.removeButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRemoveClick(getItem(position))
                }
            }
        }

        fun bind(listing: Listing) {
            binding.apply {
                titleTextView.text = listing.title
                priceTextView.text = listing.formattedPrice
                locationTextView.text = "${listing.city}, ${listing.district}"

                listing.mediaUrls.firstOrNull()?.let { imageUrl ->
                    Glide.with(imageView)
                        .load(imageUrl)
                        .centerCrop()
                        .into(imageView)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        return FavoriteViewHolder(
            ItemFavoriteListingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class ListingDiffCallback : DiffUtil.ItemCallback<Listing>() {
        override fun areItemsTheSame(oldItem: Listing, newItem: Listing) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Listing, newItem: Listing) =
            oldItem == newItem
    }
}