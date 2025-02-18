package com.alperengurle.EmlakApp.ui.admin

// AdminListingsAdapter.kt - app/src/main/java/com/alperengurle/EmlakApp/ui/admin/adapter/AdminListingsAdapter.kt

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alperengurle.EmlakApp.data.model.Listing
import com.alperengurle.EmlakApp.databinding.ItemAdminListingBinding
import com.bumptech.glide.Glide

class AdminListingsAdapter(
    private val onApprove: (String) -> Unit,
    private val onReject: (String) -> Unit
) : ListAdapter<Listing, AdminListingsAdapter.ListingViewHolder>(ListingDiffCallback()) {

    inner class ListingViewHolder(
        private val binding: ItemAdminListingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listing: Listing) {
            binding.apply {
                titleTextView.text = listing.title
                priceTextView.text = listing.formattedPrice
                locationTextView.text = "${listing.city}, ${listing.district}"

                Glide.with(itemView)
                    .load(listing.mediaUrls.firstOrNull())
                    .into(listingImageView)

                approveButton.setOnClickListener {
                    listing.id?.let { id -> onApprove(id) }
                }

                rejectButton.setOnClickListener {
                    listing.id?.let { id -> onReject(id) }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingViewHolder {
        return ListingViewHolder(
            ItemAdminListingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class ListingDiffCallback : DiffUtil.ItemCallback<Listing>() {
        override fun areItemsTheSame(oldItem: Listing, newItem: Listing) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Listing, newItem: Listing) =
            oldItem == newItem
    }
}